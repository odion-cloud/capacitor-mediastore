package com.capacitor.mediastore

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import com.getcapacitor.JSObject
import java.util.concurrent.ConcurrentHashMap

/**
 * Observes MediaStore for file changes (added, deleted, modified).
 * Uses ContentObserver which is available since API 1.
 * Compares snapshots to determine change type.
 */
class MediaFileObserver(
    private val context: Context,
    private val mediaStoreHelper: MediaStoreHelper,
    private val listener: OnMediaChangeListener
) {
    companion object {
        private const val TAG = "MediaFileObserver"
        private const val DEBOUNCE_MS = 500L
    }

    interface OnMediaChangeListener {
        fun onMediaChanged(event: JSObject)
    }

    private var contentObserver: ContentObserver? = null
    private val handler = Handler(Looper.getMainLooper())
    private var debounceRunnable: Runnable? = null

    // Snapshot of known file IDs -> dateModified for comparison
    private val fileSnapshot = ConcurrentHashMap<String, Long>()
    private var isWatching = false
    private var watchedMediaTypes: List<String> = listOf("all")

    fun startWatching(mediaTypes: List<String>? = null) {
        if (isWatching) {
            stopWatching()
        }

        watchedMediaTypes = mediaTypes ?: listOf("all")

        // Build initial snapshot
        buildSnapshot()

        contentObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                onChange(selfChange, null)
            }

            override fun onChange(selfChange: Boolean, uri: Uri?) {
                // Debounce rapid-fire changes (e.g. batch operations)
                debounceRunnable?.let { handler.removeCallbacks(it) }
                debounceRunnable = Runnable { detectChanges() }
                handler.postDelayed(debounceRunnable!!, DEBOUNCE_MS)
            }
        }

        // Register observer on the broadest URI to catch all changes
        val uri = MediaStore.Files.getContentUri("external")
        context.contentResolver.registerContentObserver(uri, true, contentObserver!!)

        // Also register on specific URIs for better coverage on older devices
        if (watchedMediaTypes.contains("all") || watchedMediaTypes.contains("audio")) {
            context.contentResolver.registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, contentObserver!!
            )
        }
        if (watchedMediaTypes.contains("all") || watchedMediaTypes.contains("image")) {
            context.contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, contentObserver!!
            )
        }
        if (watchedMediaTypes.contains("all") || watchedMediaTypes.contains("video")) {
            context.contentResolver.registerContentObserver(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, contentObserver!!
            )
        }

        isWatching = true
        Log.d(TAG, "Started watching media changes for types: $watchedMediaTypes")
    }

    fun stopWatching() {
        contentObserver?.let {
            context.contentResolver.unregisterContentObserver(it)
        }
        contentObserver = null
        debounceRunnable?.let { handler.removeCallbacks(it) }
        debounceRunnable = null
        fileSnapshot.clear()
        isWatching = false
        Log.d(TAG, "Stopped watching media changes")
    }

    /**
     * Build an initial snapshot of all media file IDs and their modification timestamps.
     */
    private fun buildSnapshot() {
        fileSnapshot.clear()

        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DATE_MODIFIED
        )

        val uris = getUrisForWatchedTypes()

        for (uri in uris) {
            try {
                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val dateModCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                    while (cursor.moveToNext()) {
                        val id = cursor.getString(idCol)
                        val dateMod = cursor.getLong(dateModCol)
                        fileSnapshot[id] = dateMod
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error building snapshot for URI $uri: ${e.message}")
            }
        }

        Log.d(TAG, "Built snapshot with ${fileSnapshot.size} files")
    }

    /**
     * Compare current MediaStore state against snapshot to detect changes.
     */
    private fun detectChanges() {
        val currentFiles = ConcurrentHashMap<String, Long>()
        val uris = getUrisForWatchedTypes()
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DATE_MODIFIED
        )

        for (uri in uris) {
            try {
                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val dateModCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                    while (cursor.moveToNext()) {
                        val id = cursor.getString(idCol)
                        val dateMod = cursor.getLong(dateModCol)
                        currentFiles[id] = dateMod
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error querying URI $uri during change detection: ${e.message}")
            }
        }

        // Detect ADDED files (in current but not in snapshot)
        for ((id, dateMod) in currentFiles) {
            if (!fileSnapshot.containsKey(id)) {
                emitChange("added", id)
            }
        }

        // Detect DELETED files (in snapshot but not in current)
        for ((id, _) in fileSnapshot) {
            if (!currentFiles.containsKey(id)) {
                emitChange("deleted", id)
            }
        }

        // Detect MODIFIED files (same id, different dateModified)
        for ((id, dateMod) in currentFiles) {
            val oldDate = fileSnapshot[id]
            if (oldDate != null && oldDate != dateMod) {
                emitChange("modified", id)
            }
        }

        // Update snapshot
        fileSnapshot.clear()
        fileSnapshot.putAll(currentFiles)
    }

    /**
     * Emit a change event with the full file data back to JS.
     */
    private fun emitChange(changeType: String, mediaId: String) {
        try {
            val fileData = queryFileById(mediaId)
            val event = JSObject().apply {
                put("type", changeType)
                put("file", fileData ?: JSObject().apply { put("id", mediaId) })
            }
            listener.onMediaChanged(event)
        } catch (e: Exception) {
            // For deleted files, we may not be able to query — send minimal data
            val event = JSObject().apply {
                put("type", changeType)
                put("file", JSObject().apply { put("id", mediaId) })
            }
            listener.onMediaChanged(event)
        }
    }

    /**
     * Query full file details by its MediaStore ID.
     */
    private fun queryFileById(mediaId: String): JSObject? {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DATE_MODIFIED
        )

        val uri = MediaStore.Files.getContentUri("external")
        val selection = "${MediaStore.MediaColumns._ID} = ?"
        val selectionArgs = arrayOf(mediaId)

        context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val fileObj = JSObject()
                val idCol = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val dataCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                val sizeCol = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val mimeCol = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                val dateAddCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
                val dateModCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)

                if (idCol >= 0) fileObj.put("id", cursor.getString(idCol))
                if (nameCol >= 0) fileObj.put("displayName", cursor.getString(nameCol))
                if (dataCol >= 0) {
                    val filePath = cursor.getString(dataCol)
                    fileObj.put("uri", "file://$filePath")
                }
                if (sizeCol >= 0) fileObj.put("size", cursor.getLong(sizeCol))
                if (mimeCol >= 0) {
                    val mimeType = cursor.getString(mimeCol) ?: ""
                    fileObj.put("mimeType", mimeType)
                    fileObj.put("mediaType", when {
                        mimeType.startsWith("audio/") -> "audio"
                        mimeType.startsWith("video/") -> "video"
                        mimeType.startsWith("image/") -> "image"
                        else -> "document"
                    })
                }
                if (dateAddCol >= 0) fileObj.put("dateAdded", cursor.getLong(dateAddCol) * 1000L)
                if (dateModCol >= 0) fileObj.put("dateModified", cursor.getLong(dateModCol) * 1000L)

                return fileObj
            }
        }
        return null
    }

    private fun getUrisForWatchedTypes(): List<Uri> {
        val uris = mutableListOf<Uri>()

        if (watchedMediaTypes.contains("all")) {
            uris.add(MediaStore.Files.getContentUri("external"))
            return uris
        }

        if (watchedMediaTypes.contains("audio")) {
            uris.add(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
        }
        if (watchedMediaTypes.contains("image")) {
            uris.add(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        }
        if (watchedMediaTypes.contains("video")) {
            uris.add(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        }
        if (watchedMediaTypes.contains("document")) {
            uris.add(MediaStore.Files.getContentUri("external"))
        }

        if (uris.isEmpty()) {
            uris.add(MediaStore.Files.getContentUri("external"))
        }

        return uris
    }
}
