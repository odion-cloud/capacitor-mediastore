package com.capacitor.mediastore

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Size
import androidx.core.app.ComponentActivity
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MediaStoreHelper(private val context: Context, private val activity: ComponentActivity?) {

    data class MediaQueryOptions(
        val limit: Int? = null,
        val offset: Int = 0,
        val sortOrder: String = "DESC",
        val sortBy: String = "DATE_ADDED",
        val albumName: String? = null,
        val artistName: String? = null,
        val includeExternal: Boolean = true
    )

    fun getMedias(options: MediaQueryOptions): JSObject {
        val result = JSObject()
        val mediaArray = JSArray()
        var totalCount = 0

        // Get all media types
        val imageFiles = queryMediaStore(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image", options)
        val audioFiles = queryMediaStore(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "audio", options)
        val videoFiles = queryMediaStore(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "video", options)

        val allFiles = mutableListOf<JSObject>()
        allFiles.addAll(imageFiles)
        allFiles.addAll(audioFiles)
        allFiles.addAll(videoFiles)

        // Sort combined results
        allFiles.sortWith { a, b ->
            val dateA = a.getLong("dateAdded")
            val dateB = b.getLong("dateAdded")
            if (options.sortOrder == "ASC") dateA.compareTo(dateB) else dateB.compareTo(dateA)
        }

        totalCount = allFiles.size

        // Apply pagination
        val startIndex = options.offset
        val endIndex = if (options.limit != null) {
            minOf(startIndex + options.limit, allFiles.size)
        } else {
            allFiles.size
        }

        for (i in startIndex until endIndex) {
            mediaArray.put(allFiles[i])
        }

        result.put("media", mediaArray)
        result.put("totalCount", totalCount)
        result.put("hasMore", endIndex < allFiles.size)

        return result
    }

    fun getMediasByType(mediaType: String, options: MediaQueryOptions): JSObject {
        val result = JSObject()
        val mediaArray = JSArray()

        val contentUri = when (mediaType.lowercase()) {
            "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Files.getContentUri("external")
        }

        val files = queryMediaStore(contentUri, mediaType, options)
        
        // Apply pagination
        val startIndex = options.offset
        val endIndex = if (options.limit != null) {
            minOf(startIndex + options.limit, files.size)
        } else {
            files.size
        }

        for (i in startIndex until endIndex) {
            mediaArray.put(files[i])
        }

        result.put("media", mediaArray)
        result.put("totalCount", files.size)
        result.put("hasMore", endIndex < files.size)

        return result
    }

    fun getAlbums(): JSObject {
        val result = JSObject()
        val albumsArray = JSArray()

        val projection = arrayOf(
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS,
            MediaStore.Audio.Albums.FIRST_YEAR,
            MediaStore.Audio.Albums.ALBUM_ART
        )

        val cursor = context.contentResolver.query(
            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            MediaStore.Audio.Albums.ALBUM + " ASC"
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
            val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)
            val trackCountColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS)
            val albumArtColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ART)

            while (it.moveToNext()) {
                val album = JSObject()
                album.put("id", it.getString(idColumn))
                album.put("name", it.getString(albumColumn) ?: "Unknown Album")
                album.put("artist", it.getString(artistColumn) ?: "Unknown Artist")
                album.put("trackCount", it.getInt(trackCountColumn))
                
                val albumArtPath = it.getString(albumArtColumn)
                if (albumArtPath != null) {
                    album.put("albumArtUri", "file://$albumArtPath")
                }

                albumsArray.put(album)
            }
        }

        result.put("albums", albumsArray)
        result.put("totalCount", albumsArray.length())

        return result
    }

    fun saveMedia(data: String, fileName: String, mediaType: String, albumName: String?, relativePath: String?): JSObject {
        val result = JSObject()

        try {
            val contentUri = when (mediaType.lowercase()) {
                "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                else -> MediaStore.Files.getContentUri("external")
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, getMimeType(mediaType, fileName))
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+ (API 29+) - Use scoped storage
                    val directory = when (mediaType.lowercase()) {
                        "image" -> Environment.DIRECTORY_PICTURES
                        "audio" -> Environment.DIRECTORY_MUSIC
                        "video" -> Environment.DIRECTORY_MOVIES
                        else -> Environment.DIRECTORY_DOCUMENTS
                    }
                    
                    val path = if (albumName != null) {
                        "$directory/$albumName"
                    } else if (relativePath != null) {
                        "$directory/$relativePath"
                    } else {
                        directory
                    }
                    
                    put(MediaStore.MediaColumns.RELATIVE_PATH, path)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                } else {
                    // Android 5-9 (API 21-28) - Use legacy external storage
                    val directory = when (mediaType.lowercase()) {
                        "image" -> Environment.DIRECTORY_PICTURES
                        "audio" -> Environment.DIRECTORY_MUSIC
                        "video" -> Environment.DIRECTORY_MOVIES
                        else -> Environment.DIRECTORY_DOCUMENTS
                    }
                    
                    val externalDir = Environment.getExternalStoragePublicDirectory(directory)
                    val targetDir = if (albumName != null) {
                        File(externalDir, albumName)
                    } else if (relativePath != null) {
                        File(externalDir, relativePath)
                    } else {
                        externalDir
                    }
                    
                    if (!targetDir.exists()) {
                        targetDir.mkdirs()
                    }
                    
                    put(MediaStore.MediaColumns.DATA, File(targetDir, fileName).absolutePath)
                }
            }

            val uri = context.contentResolver.insert(contentUri, contentValues)
            
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    if (data.startsWith("data:")) {
                        // Handle base64 data URL
                        val base64Data = data.substringAfter(",")
                        val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
                        outputStream.write(decodedBytes)
                    } else if (data.startsWith("file://")) {
                        // Handle file URI
                        val file = File(data.removePrefix("file://"))
                        file.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    } else {
                        // Assume base64 encoded data
                        val decodedBytes = Base64.decode(data, Base64.DEFAULT)
                        outputStream.write(decodedBytes)
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    context.contentResolver.update(uri, contentValues, null, null)
                }

                result.put("success", true)
                result.put("uri", uri.toString())
            } else {
                result.put("success", false)
                result.put("error", "Failed to create media file")
            }

        } catch (e: Exception) {
            result.put("success", false)
            result.put("error", e.message)
        }

        return result
    }

    fun getMediaMetadata(filePath: String): JSObject {
        val result = JSObject()
        val mediaObject = JSObject()

        try {
            val uri = Uri.parse(filePath)
            val contentResolver = context.contentResolver

            // First get basic file info
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    populateMediaObject(mediaObject, it, "audio") // Assume audio for detailed metadata
                }
            }

            // Get detailed audio metadata using MediaMetadataRetriever
            if (filePath.contains("audio") || isAudioFile(filePath)) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, uri)
                    
                    mediaObject.put("title", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "Unknown")
                    mediaObject.put("artist", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Artist")
                    mediaObject.put("album", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Unknown Album")
                    mediaObject.put("albumArtist", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST))
                    mediaObject.put("composer", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER))
                    mediaObject.put("genre", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE))
                    mediaObject.put("year", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.toIntOrNull())
                    mediaObject.put("track", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)?.toIntOrNull())
                    
                    val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    durationStr?.toLongOrNull()?.let { duration ->
                        mediaObject.put("duration", duration)
                    }

                    val bitrateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                    bitrateStr?.toIntOrNull()?.let { bitrate ->
                        mediaObject.put("bitrate", bitrate)
                    }

                    // Get album art
                    val albumArt = retriever.embeddedPicture
                    if (albumArt != null) {
                        // You could save this as a temporary file and return the URI
                        // For now, we'll indicate that album art is available
                        mediaObject.put("hasAlbumArt", true)
                    }

                    retriever.release()
                } catch (e: Exception) {
                    // Fallback to MediaStore metadata if MediaMetadataRetriever fails
                }
            }

            result.put("media", mediaObject)

        } catch (e: Exception) {
            throw Exception("Failed to get metadata: ${e.message}")
        }

        return result
    }

    private fun queryMediaStore(contentUri: Uri, mediaType: String, options: MediaQueryOptions): List<JSObject> {
        val files = mutableListOf<JSObject>()
        
        val projection = getProjectionForMediaType(mediaType)
        val selection = buildSelection(options, mediaType)
        val selectionArgs = buildSelectionArgs(options)
        val sortOrder = buildSortOrder(options)

        // Include both internal and external content URIs for better coverage
        val urisToQuery = mutableListOf(contentUri)
        
        // Add external volumes for Android 10+ (API 29+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && options.includeExternal) {
            try {
                val externalVolumeNames = MediaStore.getExternalVolumeNames(context)
                externalVolumeNames.forEach { volumeName ->
                    if (volumeName != MediaStore.VOLUME_EXTERNAL_PRIMARY) {
                        val externalUri = when (mediaType.lowercase()) {
                            "image" -> MediaStore.Images.Media.getContentUri(volumeName)
                            "audio" -> MediaStore.Audio.Media.getContentUri(volumeName)
                            "video" -> MediaStore.Video.Media.getContentUri(volumeName)
                            else -> MediaStore.Files.getContentUri(volumeName)
                        }
                        urisToQuery.add(externalUri)
                    }
                }
            } catch (e: Exception) {
                // Fall back to default URI if external volumes are not accessible
            }
        }

        urisToQuery.forEach { uri ->
            try {
                val cursor = context.contentResolver.query(
                    uri,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )

                cursor?.use {
                    while (it.moveToNext()) {
                        val mediaObject = JSObject()
                        populateMediaObject(mediaObject, it, mediaType)
                        files.add(mediaObject)
                    }
                }
            } catch (e: Exception) {
                // Continue with other URIs if one fails
            }
        }

        return files
    }

    private fun getProjectionForMediaType(mediaType: String): Array<String> {
        val baseProjection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DATE_MODIFIED
        )

        return when (mediaType.lowercase()) {
            "image" -> baseProjection + arrayOf(
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT
            )
            "audio" -> baseProjection + arrayOf(
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ARTIST,
                MediaStore.Audio.Media.COMPOSER,
                MediaStore.Audio.Media.GENRE,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.BOOKMARK
            )
            "video" -> baseProjection + arrayOf(
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT,
                MediaStore.Video.Media.DURATION
            )
            else -> baseProjection
        }
    }

    private fun populateMediaObject(mediaObject: JSObject, cursor: Cursor, mediaType: String) {
        val idColumn = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
        val displayNameColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
        val dataColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
        val sizeColumn = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
        val mimeTypeColumn = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
        val dateAddedColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
        val dateModifiedColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)

        if (idColumn >= 0) mediaObject.put("id", cursor.getString(idColumn))
        if (displayNameColumn >= 0) mediaObject.put("displayName", cursor.getString(displayNameColumn))
        if (dataColumn >= 0) {
            val filePath = cursor.getString(dataColumn)
            mediaObject.put("uri", "file://$filePath")
            mediaObject.put("isExternal", filePath.contains("/storage/") && !filePath.contains("/storage/emulated/0/"))
        }
        if (sizeColumn >= 0) mediaObject.put("size", cursor.getLong(sizeColumn))
        if (mimeTypeColumn >= 0) mediaObject.put("mimeType", cursor.getString(mimeTypeColumn))
        if (dateAddedColumn >= 0) mediaObject.put("dateAdded", cursor.getLong(dateAddedColumn) * 1000L)
        if (dateModifiedColumn >= 0) mediaObject.put("dateModified", cursor.getLong(dateModifiedColumn) * 1000L)
        
        mediaObject.put("mediaType", mediaType)

        // Add type-specific fields
        when (mediaType.lowercase()) {
            "image" -> {
                val widthColumn = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
                val heightColumn = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)
                if (widthColumn >= 0) mediaObject.put("width", cursor.getInt(widthColumn))
                if (heightColumn >= 0) mediaObject.put("height", cursor.getInt(heightColumn))
            }
            "audio" -> {
                val durationColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val albumArtistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ARTIST)
                val composerColumn = cursor.getColumnIndex(MediaStore.Audio.Media.COMPOSER)
                val genreColumn = cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)
                val trackColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK)
                val yearColumn = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)

                if (durationColumn >= 0) mediaObject.put("duration", cursor.getLong(durationColumn))
                if (titleColumn >= 0) mediaObject.put("title", cursor.getString(titleColumn) ?: "Unknown")
                if (artistColumn >= 0) mediaObject.put("artist", cursor.getString(artistColumn) ?: "Unknown Artist")
                if (albumColumn >= 0) mediaObject.put("album", cursor.getString(albumColumn) ?: "Unknown Album")
                if (albumArtistColumn >= 0) mediaObject.put("albumArtist", cursor.getString(albumArtistColumn))
                if (composerColumn >= 0) mediaObject.put("composer", cursor.getString(composerColumn))
                if (genreColumn >= 0) mediaObject.put("genre", cursor.getString(genreColumn))
                if (trackColumn >= 0) mediaObject.put("track", cursor.getInt(trackColumn))
                if (yearColumn >= 0) mediaObject.put("year", cursor.getInt(yearColumn))

                // Get album art URI
                val albumId = getAlbumId(cursor.getString(idColumn))
                if (albumId != null) {
                    val albumArtUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    )
                    mediaObject.put("albumArtUri", albumArtUri.toString())
                }
            }
            "video" -> {
                val widthColumn = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightColumn = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                val durationColumn = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                
                if (widthColumn >= 0) mediaObject.put("width", cursor.getInt(widthColumn))
                if (heightColumn >= 0) mediaObject.put("height", cursor.getInt(heightColumn))
                if (durationColumn >= 0) mediaObject.put("duration", cursor.getLong(durationColumn))
            }
        }
    }

    private fun buildSelection(options: MediaQueryOptions, mediaType: String): String? {
        val conditions = mutableListOf<String>()

        // Add media type specific conditions
        when (mediaType.lowercase()) {
            "audio" -> {
                if (options.albumName != null) {
                    conditions.add("${MediaStore.Audio.Media.ALBUM} = ?")
                }
                if (options.artistName != null) {
                    conditions.add("${MediaStore.Audio.Media.ARTIST} = ?")
                }
                // Filter out very short audio files (likely system sounds)
                conditions.add("${MediaStore.Audio.Media.DURATION} > 5000")
            }
            "image" -> {
                // Filter out very small images (likely thumbnails)
                conditions.add("${MediaStore.Images.Media.SIZE} > 1024")
            }
            "video" -> {
                // Filter out very short videos
                conditions.add("${MediaStore.Video.Media.DURATION} > 1000")
            }
        }

        return if (conditions.isEmpty()) null else conditions.joinToString(" AND ")
    }

    private fun buildSelectionArgs(options: MediaQueryOptions): Array<String>? {
        val args = mutableListOf<String>()

        if (options.albumName != null) {
            args.add(options.albumName)
        }

        if (options.artistName != null) {
            args.add(options.artistName)
        }

        return if (args.isEmpty()) null else args.toTypedArray()
    }

    private fun buildSortOrder(options: MediaQueryOptions): String {
        val sortColumn = when (options.sortBy) {
            "DATE_ADDED" -> MediaStore.MediaColumns.DATE_ADDED
            "DATE_MODIFIED" -> MediaStore.MediaColumns.DATE_MODIFIED
            "DISPLAY_NAME" -> MediaStore.MediaColumns.DISPLAY_NAME
            "SIZE" -> MediaStore.MediaColumns.SIZE
            "TITLE" -> MediaStore.Audio.Media.TITLE
            else -> MediaStore.MediaColumns.DATE_ADDED
        }

        val order = if (options.sortOrder == "ASC") "ASC" else "DESC"
        return "$sortColumn $order"
    }

    private fun getAlbumId(mediaId: String?): Long? {
        if (mediaId == null) return null
        
        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Audio.Media.ALBUM_ID),
            "${MediaStore.Audio.Media._ID} = ?",
            arrayOf(mediaId),
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val albumIdColumn = it.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                if (albumIdColumn >= 0) {
                    return it.getLong(albumIdColumn)
                }
            }
        }

        return null
    }

    private fun getMimeType(mediaType: String, fileName: String): String {
        return when (mediaType.lowercase()) {
            "image" -> {
                when (fileName.substringAfterLast('.').lowercase()) {
                    "jpg", "jpeg" -> "image/jpeg"
                    "png" -> "image/png"
                    "gif" -> "image/gif"
                    "webp" -> "image/webp"
                    else -> "image/*"
                }
            }
            "audio" -> {
                when (fileName.substringAfterLast('.').lowercase()) {
                    "mp3" -> "audio/mpeg"
                    "wav" -> "audio/wav"
                    "m4a" -> "audio/mp4"
                    "ogg" -> "audio/ogg"
                    "flac" -> "audio/flac"
                    else -> "audio/*"
                }
            }
            "video" -> {
                when (fileName.substringAfterLast('.').lowercase()) {
                    "mp4" -> "video/mp4"
                    "avi" -> "video/avi"
                    "mkv" -> "video/mkv"
                    "mov" -> "video/quicktime"
                    else -> "video/*"
                }
            }
            else -> "application/octet-stream"
        }
    }

    private fun isAudioFile(filePath: String): Boolean {
        val audioExtensions = listOf("mp3", "wav", "m4a", "ogg", "flac", "aac", "wma")
        val extension = filePath.substringAfterLast('.').lowercase()
        return audioExtensions.contains(extension)
    }
}
