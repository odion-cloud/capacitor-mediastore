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

import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MediaStoreHelper(private val context: Context) {

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

        val allFiles = mutableListOf<JSObject>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+) - Use specific content URIs for each media type
            val imageFiles = queryMediaStore(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image", options)
            val audioFiles = queryMediaStore(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "audio", options)
            val videoFiles = queryMediaStore(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "video", options)
            val documentFiles = queryMediaStore(MediaStore.Files.getContentUri("external"), "document", options)

            allFiles.addAll(imageFiles)
            allFiles.addAll(audioFiles)
            allFiles.addAll(videoFiles)
            allFiles.addAll(documentFiles)
        } else {
            // Android 12 and below (API 32 and below) - Use Files content URI for broader access
            val allMediaFiles = queryMediaStore(MediaStore.Files.getContentUri("external"), "all", options)
            allFiles.addAll(allMediaFiles)
        }

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

        val files = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+) - Use specific content URIs
            val contentUri = when (mediaType.lowercase()) {
                "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                "document" -> MediaStore.Files.getContentUri("external")
                else -> MediaStore.Files.getContentUri("external")
            }
            queryMediaStore(contentUri, mediaType, options)
        } else {
            // Android 12 and below (API 32 and below) - Use appropriate strategy
            when (mediaType.lowercase()) {
                "audio" -> {
                    // For audio files on older Android, use Audio content URI directly for better compatibility
                    val audioFiles = mutableListOf<JSObject>()
                    
                    // Query primary external storage
                    try {
                        audioFiles.addAll(queryMediaStore(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaType, options))
                    } catch (e: Exception) {
                        // If Audio URI fails, fallback to Files URI with compatible projection
                        audioFiles.addAll(queryMediaStore(MediaStore.Files.getContentUri("external"), mediaType, options))
                    }
                    
                    // For Android 9 and below, try to access external SD card manually
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && options.includeExternal) {
                        try {
                            // Try alternative external storage paths for SD cards
                            val alternativeUris = listOf(
                                MediaStore.Files.getContentUri("external_primary"),
                                MediaStore.Audio.Media.getContentUri("external_primary")
                            )
                            
                            alternativeUris.forEach { uri ->
                                try {
                                    audioFiles.addAll(queryMediaStore(uri, mediaType, options))
                                } catch (e: Exception) {
                                    // Continue if this URI fails
                                }
                            }
                        } catch (e: Exception) {
                            // Continue if external SD card access fails
                        }
                    }
                    
                    audioFiles
                }
                else -> {
                    // For other media types, use Files content URI
                    queryMediaStore(MediaStore.Files.getContentUri("external"), mediaType, options)
                }
            }
        }
        
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
        val selection = buildSelection(options, mediaType, contentUri)
        val selectionArgs = buildSelectionArgs(options)
        val sortOrder = buildSortOrder(options)

        // Include both internal and external content URIs for better coverage
        val urisToQuery = mutableListOf(contentUri)
        
        // Add external volumes based on Android version
        if (options.includeExternal) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ (API 29+) - Use getExternalVolumeNames
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
            } else {
                // Android 9 and below - Try alternative approaches for external storage
                // This is already handled in the getMediasByType method for audio files
                // For other media types, we just use the primary external URI
            }
        }

        urisToQuery.forEach { uri ->
            try {
                // Recalculate selection for each URI since Files vs Audio URIs need different selection
                val uriSpecificSelection = buildSelection(options, mediaType, uri)
                
                // Debug logging for Android 12 and below
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    android.util.Log.d("MediaStoreHelper", "Querying URI: $uri for mediaType: $mediaType")
                    android.util.Log.d("MediaStoreHelper", "Selection: $uriSpecificSelection")
                    android.util.Log.d("MediaStoreHelper", "Projection count: ${projection.size}")
                }
                
                val cursor = context.contentResolver.query(
                    uri,
                    projection,
                    uriSpecificSelection,
                    selectionArgs,
                    sortOrder
                )

                cursor?.use {
                    var count = 0
                    while (it.moveToNext()) {
                        val mediaObject = JSObject()
                        populateMediaObject(mediaObject, it, mediaType)
                        files.add(mediaObject)
                        count++
                    }
                    
                    // Debug logging for results
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        android.util.Log.d("MediaStoreHelper", "Found $count files from URI: $uri")
                    }
                }
            } catch (e: Exception) {
                // Continue with other URIs if one fails
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    android.util.Log.e("MediaStoreHelper", "Error querying URI: $uri", e)
                }
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
            "audio" -> {
                // Check if we're on Android 13+ or can use Audio content URI
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Android 13+ can safely use audio-specific columns
                    baseProjection + arrayOf(
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
                } else {
                    // Android 12 and below - use only universally available columns when querying Files URI
                    // Audio-specific columns will be handled in populateMediaObject by checking column existence
                    baseProjection
                }
            }
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

                // For Android 12 and below, if audio columns are not available, set default values
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    if (titleColumn < 0) {
                        mediaObject.put("title", mediaObject.getString("displayName") ?: "Unknown")
                    }
                    if (artistColumn < 0) {
                        mediaObject.put("artist", "Unknown Artist")
                    }
                    if (albumColumn < 0) {
                        mediaObject.put("album", "Unknown Album")
                    }
                    if (durationColumn < 0) {
                        mediaObject.put("duration", 0)
                    }
                    if (trackColumn < 0) {
                        mediaObject.put("track", 0)
                    }
                    if (yearColumn < 0) {
                        mediaObject.put("year", 0)
                    }
                }

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

    private fun buildSelection(options: MediaQueryOptions, mediaType: String, contentUri: Uri? = null): String? {
        val conditions = mutableListOf<String>()

        // Add media type filtering only when using Files content URI
        val isFilesUri = contentUri?.toString()?.contains("files") == true
        
        when (mediaType.lowercase()) {
            "image" -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && isFilesUri) {
                    conditions.add("${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}")
                }
                conditions.add("${MediaStore.MediaColumns.SIZE} > 1024")
            }
            "audio" -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && isFilesUri) {
                    conditions.add("${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO}")
                }
                if (options.albumName != null) {
                    conditions.add("${MediaStore.Audio.Media.ALBUM} = ?")
                }
                if (options.artistName != null) {
                    conditions.add("${MediaStore.Audio.Media.ARTIST} = ?")
                }
            }
            "video" -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && isFilesUri) {
                    conditions.add("${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO}")
                }
                conditions.add("${MediaStore.Video.Media.DURATION} > 1000")
            }
            "document" -> {
                if (isFilesUri) {
                    conditions.add("${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_NONE}")
                    conditions.add("(${MediaStore.MediaColumns.MIME_TYPE} LIKE 'application/%' OR ${MediaStore.MediaColumns.MIME_TYPE} LIKE 'text/%')")
                    conditions.add("${MediaStore.MediaColumns.MIME_TYPE} NOT LIKE 'audio/%'")
                    conditions.add("${MediaStore.MediaColumns.MIME_TYPE} NOT LIKE 'video/%'")
                    conditions.add("${MediaStore.MediaColumns.MIME_TYPE} NOT LIKE 'image/%'")
                }
            }
            "all" -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && isFilesUri) {
                    conditions.add("(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE} OR " +
                                 "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO} OR " +
                                 "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO} OR " +
                                 "(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_NONE} AND " +
                                 "(${MediaStore.MediaColumns.MIME_TYPE} LIKE 'application/%' OR ${MediaStore.MediaColumns.MIME_TYPE} LIKE 'text/%')))")
                }
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
