package com.capacitor.mediastore

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.annotation.Permission
import com.getcapacitor.annotation.PermissionCallback
import com.getcapacitor.PermissionState

@CapacitorPlugin(
    name = "CapacitorMediaStore",
    permissions = [
        Permission(
            strings = [Manifest.permission.READ_EXTERNAL_STORAGE],
            alias = "readExternalStorage"
        ),
        Permission(
            strings = [Manifest.permission.WRITE_EXTERNAL_STORAGE],
            alias = "writeExternalStorage"
        ),
        Permission(
            strings = [Manifest.permission.READ_MEDIA_IMAGES],
            alias = "readMediaImages"
        ),
        Permission(
            strings = [Manifest.permission.READ_MEDIA_AUDIO],
            alias = "readMediaAudio"
        ),
        Permission(
            strings = [Manifest.permission.READ_MEDIA_VIDEO],
            alias = "readMediaVideo"
        ),
        Permission(
            strings = ["android.permission.READ_MEDIA_VISUAL_USER_SELECTED"],
            alias = "readMediaVisualUserSelected"
        )
    ]
)
class MediaStorePlugin : Plugin() {
    
    private lateinit var mediaStoreHelper: MediaStoreHelper

    override fun load() {
        mediaStoreHelper = MediaStoreHelper(context)
    }

    @PluginMethod
    fun getMedias(call: PluginCall) {
        if (!hasRequiredPermissions()) {
            call.reject("Permission denied. Please grant media access permissions.")
            return
        }

        try {
            val options = parseMediaQueryOptions(call)
            val result = mediaStoreHelper.getMedias(options)
            call.resolve(result)
        } catch (e: Exception) {
            call.reject("Failed to get media files: ${e.message}", e)
        }
    }

    @PluginMethod
    fun getMediasByType(call: PluginCall) {
        if (!hasRequiredPermissions()) {
            call.reject("Permission denied. Please grant media access permissions.")
            return
        }

        try {
            val mediaType = call.getString("mediaType") ?: "all"
            val options = parseMediaQueryOptions(call)
            val result = mediaStoreHelper.getMediasByType(mediaType, options)
            call.resolve(result)
        } catch (e: Exception) {
            call.reject("Failed to get media files by type: ${e.message}", e)
        }
    }

    @PluginMethod
    fun getAlbums(call: PluginCall) {
        if (!hasRequiredPermissions()) {
            call.reject("Permission denied. Please grant media access permissions.")
            return
        }

        try {
            val result = mediaStoreHelper.getAlbums()
            call.resolve(result)
        } catch (e: Exception) {
            call.reject("Failed to get albums: ${e.message}", e)
        }
    }

    @PluginMethod
    fun saveMedia(call: PluginCall) {
        if (!hasWritePermissions()) {
            call.reject("Permission denied. Please grant write permissions.")
            return
        }

        try {
            val data = call.getString("data") ?: throw IllegalArgumentException("Data is required")
            val fileName = call.getString("fileName") ?: throw IllegalArgumentException("FileName is required")
            val mediaType = call.getString("mediaType") ?: "image"
            val albumName = call.getString("albumName")
            val relativePath = call.getString("relativePath")

            val result = mediaStoreHelper.saveMedia(data, fileName, mediaType, albumName, relativePath)
            call.resolve(result)
        } catch (e: Exception) {
            call.reject("Failed to save media: ${e.message}", e)
        }
    }

    @PluginMethod
    fun getMediaMetadata(call: PluginCall) {
        if (!hasRequiredPermissions()) {
            call.reject("Permission denied. Please grant media access permissions.")
            return
        }

        try {
            val filePath = call.getString("filePath") ?: throw IllegalArgumentException("FilePath is required")
            val result = mediaStoreHelper.getMediaMetadata(filePath)
            call.resolve(result)
        } catch (e: Exception) {
            call.reject("Failed to get media metadata: ${e.message}", e)
        }
    }

    // New method to create document picker intent for Android 13+
    @PluginMethod
    fun createDocumentPickerIntent(call: PluginCall) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            call.reject("Document picker via SAF is recommended for Android 13+. Use getMediasByType for older versions.")
            return
        }

        try {
            val intent = mediaStoreHelper.createDocumentPickerIntent()
            val result = JSObject()
            result.put("success", true)
            result.put("message", "Use the returned intent with startActivityForResult to pick documents")
            result.put("intentAction", intent.action)
            result.put("intentType", intent.type)
            
            // Since we can't directly return Intent objects, provide guidance
            result.put("guidance", "Use ACTION_OPEN_DOCUMENT intent in your app's activity to pick documents")
            call.resolve(result)
        } catch (e: Exception) {
            call.reject("Failed to create document picker intent: ${e.message}", e)
        }
    }

    // New method to get document metadata from SAF URI
    @PluginMethod
    fun getDocumentMetadataFromSAF(call: PluginCall) {
        try {
            val uriString = call.getString("uri") ?: throw IllegalArgumentException("URI is required")
            val uri = android.net.Uri.parse(uriString)
            val result = mediaStoreHelper.getDocumentMetadataFromSAF(uri)
            call.resolve(result)
        } catch (e: Exception) {
            call.reject("Failed to get document metadata: ${e.message}", e)
        }
    }

    // New method to check if the device should use SAF for documents
    @PluginMethod
    fun shouldUseSAFForDocuments(call: PluginCall) {
        val result = JSObject()
        val shouldUseSAF = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        result.put("shouldUseSAF", shouldUseSAF)
        result.put("androidVersion", Build.VERSION.SDK_INT)
        result.put("message", if (shouldUseSAF) {
            "Android 13+ detected. Use Storage Access Framework (SAF) for document access."
        } else {
            "Android 12 or below. MediaStore can be used for document access."
        })
        call.resolve(result)
    }

    @PluginMethod
    override fun checkPermissions(call: PluginCall) {
        val result = JSObject()
        
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                // Android 14+ (API 34+) - Visual media permissions
                result.put("readMediaImages", getPermissionState("readMediaImages").toString())
                result.put("readMediaAudio", getPermissionState("readMediaAudio").toString())
                result.put("readMediaVideo", getPermissionState("readMediaVideo").toString())
                result.put("readMediaVisualUserSelected", getPermissionState("readMediaVisualUserSelected").toString())
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+ (API 33+) - Granular media permissions
                result.put("readMediaImages", getPermissionState("readMediaImages").toString())
                result.put("readMediaAudio", getPermissionState("readMediaAudio").toString())
                result.put("readMediaVideo", getPermissionState("readMediaVideo").toString())
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                // Android 6+ (API 23+) - Runtime permissions
                result.put("readExternalStorage", getPermissionState("readExternalStorage").toString())
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    result.put("writeExternalStorage", getPermissionState("writeExternalStorage").toString())
                }
            }
            else -> {
                // Android 5.1 and below (API 22 and below) - Install-time permissions
                result.put("readExternalStorage", "granted")
                result.put("writeExternalStorage", "granted")
            }
        }

        call.resolve(result)
    }

    @PluginMethod
    override fun requestPermissions(call: PluginCall) {
        // Get requested permission types from options
        val requestedTypes = call.getArray("types")
        val aliasesToRequest = mutableListOf<String>()
        
        if (requestedTypes != null && requestedTypes.length() > 0) {
            // Request specific permission types using aliases
            for (i in 0 until requestedTypes.length()) {
                when (requestedTypes.getString(i)?.lowercase()) {
                    "images" -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            aliasesToRequest.add("readMediaImages")
                        } else {
                            aliasesToRequest.add("readExternalStorage")
                        }
                    }
                    "audio" -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            aliasesToRequest.add("readMediaAudio")
                        } else {
                            aliasesToRequest.add("readExternalStorage")
                        }
                    }
                    "video" -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            aliasesToRequest.add("readMediaVideo")
                        } else {
                            aliasesToRequest.add("readExternalStorage")
                        }
                    }
                }
            }
        } else {
            // Request all permissions using aliases
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                    // Android 14+ (API 34+) - Visual media permissions
                    aliasesToRequest.addAll(listOf(
                        "readMediaImages",
                        "readMediaAudio", 
                        "readMediaVideo",
                        "readMediaVisualUserSelected"
                    ))
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                    // Android 13+ (API 33+) - Granular media permissions
                    aliasesToRequest.addAll(listOf(
                        "readMediaImages",
                        "readMediaAudio",
                        "readMediaVideo"
                    ))
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    // Android 6+ (API 23+) - Runtime permissions
                    aliasesToRequest.add("readExternalStorage")
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        aliasesToRequest.add("writeExternalStorage")
                    }
                }
                else -> {
                    // Android 5.1 and below (API 22 and below) - No runtime permissions needed
                    checkPermissions(call)
                    return
                }
            }
        }

        // Remove duplicates and request permissions using aliases
        val uniqueAliases = aliasesToRequest.distinct().toTypedArray()
        if (uniqueAliases.isNotEmpty()) {
            requestPermissionForAliases(uniqueAliases, call, "permissionCallback")
        } else {
            checkPermissions(call)
        }
    }

    @PermissionCallback
    private fun permissionCallback(call: PluginCall) {
        checkPermissions(call)
    }

    override fun hasRequiredPermissions(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                // Android 14+ (API 34+) - Visual media permissions
                getPermissionState("readMediaImages") == PermissionState.GRANTED ||
                getPermissionState("readMediaAudio") == PermissionState.GRANTED ||
                getPermissionState("readMediaVideo") == PermissionState.GRANTED ||
                getPermissionState("readMediaVisualUserSelected") == PermissionState.GRANTED
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+ (API 33+) - Granular media permissions
                getPermissionState("readMediaImages") == PermissionState.GRANTED ||
                getPermissionState("readMediaAudio") == PermissionState.GRANTED ||
                getPermissionState("readMediaVideo") == PermissionState.GRANTED
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // Android 10-12 (API 29-32) - Scoped storage with READ_EXTERNAL_STORAGE
                getPermissionState("readExternalStorage") == PermissionState.GRANTED
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                // Android 6-9 (API 23-28) - Traditional storage permissions
                getPermissionState("readExternalStorage") == PermissionState.GRANTED
            }
            else -> {
                // Android 5.1 and below (API 22 and below) - Install-time permissions
                true
            }
        }
    }

    private fun hasWritePermissions(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // Android 10+ (API 29+) - Scoped storage doesn't require write permission for app-specific directories
                true
            }
            else -> {
                // Android 6-9 (API 23-28) - Traditional storage permissions required
                getPermissionState("writeExternalStorage") == PermissionState.GRANTED
            }
        }
    }

    private fun parseMediaQueryOptions(call: PluginCall): MediaStoreHelper.MediaQueryOptions {
        return MediaStoreHelper.MediaQueryOptions(
            limit = call.getInt("limit"),
            offset = call.getInt("offset") ?: 0,
            sortOrder = call.getString("sortOrder") ?: "DESC",
            sortBy = call.getString("sortBy") ?: "DATE_ADDED",
            albumName = call.getString("albumName"),
            artistName = call.getString("artistName"),
            includeExternal = call.getBoolean("includeExternal") ?: true
        )
    }

    override fun getPermissionState(alias: String): PermissionState {
        return super.getPermissionState(alias)
    }
}
