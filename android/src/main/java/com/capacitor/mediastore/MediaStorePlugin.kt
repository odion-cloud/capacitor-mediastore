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

    @PluginMethod
    override fun checkPermissions(call: PluginCall) {
        val result = JSObject()
        
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                // Android 14+ (API 34+) - Visual media permissions
                result.put("readMediaImages", getPermissionState(Manifest.permission.READ_MEDIA_IMAGES).toString())
                result.put("readMediaAudio", getPermissionState(Manifest.permission.READ_MEDIA_AUDIO).toString())
                result.put("readMediaVideo", getPermissionState(Manifest.permission.READ_MEDIA_VIDEO).toString())
                result.put("readMediaVisualUserSelected", getPermissionState("android.permission.READ_MEDIA_VISUAL_USER_SELECTED").toString())
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+ (API 33+) - Granular media permissions
                result.put("readMediaImages", getPermissionState(Manifest.permission.READ_MEDIA_IMAGES).toString())
                result.put("readMediaAudio", getPermissionState(Manifest.permission.READ_MEDIA_AUDIO).toString())
                result.put("readMediaVideo", getPermissionState(Manifest.permission.READ_MEDIA_VIDEO).toString())
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                // Android 6+ (API 23+) - Runtime permissions
                result.put("readExternalStorage", getPermissionState(Manifest.permission.READ_EXTERNAL_STORAGE).toString())
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    result.put("writeExternalStorage", getPermissionState(Manifest.permission.WRITE_EXTERNAL_STORAGE).toString())
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
        val permissions = mutableListOf<String>()
        
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                // Android 14+ (API 34+) - Visual media permissions
                permissions.addAll(listOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"
                ))
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+ (API 33+) - Granular media permissions
                permissions.addAll(listOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_MEDIA_VIDEO
                ))
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                // Android 6+ (API 23+) - Runtime permissions
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
            else -> {
                // Android 5.1 and below (API 22 and below) - No runtime permissions needed
                checkPermissions(call)
                return
            }
        }

        requestPermissionForAliases(permissions.toTypedArray(), call, "permissionCallback")
    }

    @PermissionCallback
    private fun permissionCallback(call: PluginCall) {
        checkPermissions(call)
    }

    override fun hasRequiredPermissions(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                // Android 14+ (API 34+) - Visual media permissions
                getPermissionState(Manifest.permission.READ_MEDIA_IMAGES) == PermissionState.GRANTED ||
                getPermissionState(Manifest.permission.READ_MEDIA_AUDIO) == PermissionState.GRANTED ||
                getPermissionState(Manifest.permission.READ_MEDIA_VIDEO) == PermissionState.GRANTED ||
                getPermissionState("android.permission.READ_MEDIA_VISUAL_USER_SELECTED") == PermissionState.GRANTED
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+ (API 33+) - Granular media permissions
                getPermissionState(Manifest.permission.READ_MEDIA_IMAGES) == PermissionState.GRANTED ||
                getPermissionState(Manifest.permission.READ_MEDIA_AUDIO) == PermissionState.GRANTED ||
                getPermissionState(Manifest.permission.READ_MEDIA_VIDEO) == PermissionState.GRANTED
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // Android 10-12 (API 29-32) - Scoped storage with READ_EXTERNAL_STORAGE
                getPermissionState(Manifest.permission.READ_EXTERNAL_STORAGE) == PermissionState.GRANTED
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                // Android 6-9 (API 23-28) - Traditional storage permissions
                getPermissionState(Manifest.permission.READ_EXTERNAL_STORAGE) == PermissionState.GRANTED
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
                getPermissionState(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PermissionState.GRANTED
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

    override fun getPermissionState(permission: String): PermissionState {
        return if (context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            PermissionState.GRANTED
        } else {
            PermissionState.DENIED
        }
    }
}
