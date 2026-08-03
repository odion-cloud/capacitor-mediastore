package com.capacitor.mediastore

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import com.getcapacitor.JSArray
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
            strings = [Manifest.permission.READ_MEDIA_AUDIO],
            alias = "readMediaAudio"
        ),
        Permission(
            strings = ["android.permission.POST_NOTIFICATIONS"],
            alias = "postNotifications"
        ),
        Permission(
            strings = ["android.permission.FOREGROUND_SERVICE"],
            alias = "foregroundService"
        ),
        Permission(
            strings = ["android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK"],
            alias = "foregroundServiceMediaPlayback"
        )
    ]
)
class MediaStorePlugin : Plugin() {
    
    private lateinit var mediaStoreHelper: MediaStoreHelper

    // File change watcher
    private var mediaFileObserver: MediaFileObserver? = null

    // Media session service
    private var mediaSessionService: MediaSessionService? = null
    private var isServiceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MediaSessionService.MediaSessionBinder
            mediaSessionService = binder.getService()
            mediaSessionService?.actionListener = object : MediaSessionService.MediaSessionActionListener {
                override fun onAction(action: String, extras: Map<String, Any>?) {
                    // Always emit JS event (works when JS is alive)
                    val event = JSObject().apply {
                        put("action", action)
                        if (extras != null) {
                            val extrasObj = JSObject()
                            for ((key, value) in extras) {
                                when (value) {
                                    is Long -> extrasObj.put(key, value)
                                    is Double -> extrasObj.put(key, value)
                                    is String -> extrasObj.put(key, value)
                                    is Boolean -> extrasObj.put(key, value)
                                    else -> extrasObj.put(key, value.toString())
                                }
                            }
                            put("extras", extrasObj)
                        }
                    }
                    notifyListeners("mediaSessionAction", event)

                    // Also fire native listener (works even in Doze mode)
                    nativeActionListener?.invoke(action, extras)
                }
            }
            isServiceBound = true

            // Execute any pending JS metadata call
            pendingMetadataCall?.let { call ->
                executeSetMediaSession(call)
                pendingMetadataCall = null
            }

            // Execute any pending native metadata call
            pendingNativeMetadata?.let { meta ->
                executeSetMediaSessionNative(
                    meta.title, meta.artist, meta.album,
                    meta.artworkUrl, meta.duration, meta.smallIconName, meta.buttons
                )
                pendingNativeMetadata = null
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mediaSessionService = null
            isServiceBound = false
        }
    }

    private var pendingMetadataCall: PluginCall? = null

    override fun load() {
        mediaStoreHelper = MediaStoreHelper(context)
    }

    override fun handleOnDestroy() {
        mediaFileObserver?.stopWatching()
        mediaFileObserver = null

        if (isServiceBound) {
            try {
                context.unbindService(serviceConnection)
            } catch (e: Exception) { }
            isServiceBound = false
        }
        mediaSessionService = null
        super.handleOnDestroy()
    }

    // ──────────────────────────────────────────────
    // Existing Media Store methods
    // ──────────────────────────────────────────────

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
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+ (API 33+) - Audio-only media permission
                result.put("readMediaAudio", getPermissionState("readMediaAudio").toString())
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
                    "audio" -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            aliasesToRequest.add("readMediaAudio")
                        } else {
                            aliasesToRequest.add("readExternalStorage")
                        }
                    }
                    "images", "video" -> {
                        // Image/Video permissions removed per Google Play policy
                        // Fall back to readExternalStorage on older Android versions
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                            aliasesToRequest.add("readExternalStorage")
                        }
                    }
                    "notifications" -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            aliasesToRequest.add("postNotifications")
                        }
                    }
                }
            }
        } else {
            // Request all permissions using aliases — AUDIO ONLY
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                    // Android 13+ (API 33+) - Audio-only media permission
                    aliasesToRequest.addAll(listOf(
                        "readMediaAudio"
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

    // ──────────────────────────────────────────────
    // File Change Watcher
    // ──────────────────────────────────────────────

    @PluginMethod
    fun startWatching(call: PluginCall) {
        try {
            val mediaTypesArray = call.getArray("mediaTypes")
            val mediaTypes = if (mediaTypesArray != null && mediaTypesArray.length() > 0) {
                (0 until mediaTypesArray.length()).map { mediaTypesArray.getString(it) }
            } else {
                listOf("all")
            }

            mediaFileObserver?.stopWatching()
            mediaFileObserver = MediaFileObserver(
                context,
                mediaStoreHelper,
                object : MediaFileObserver.OnMediaChangeListener {
                    override fun onMediaChanged(event: JSObject) {
                        notifyListeners("mediaChanged", event)
                    }
                }
            )
            mediaFileObserver?.startWatching(mediaTypes)
            call.resolve()
        } catch (e: Exception) {
            call.reject("Failed to start watching: ${e.message}", e)
        }
    }

    @PluginMethod
    fun stopWatching(call: PluginCall) {
        try {
            mediaFileObserver?.stopWatching()
            mediaFileObserver = null
            call.resolve()
        } catch (e: Exception) {
            call.reject("Failed to stop watching: ${e.message}", e)
        }
    }

    // ──────────────────────────────────────────────
    // Native Media Session
    // ──────────────────────────────────────────────

    @PluginMethod
    fun setMediaSession(call: PluginCall) {
        try {
            if (!isServiceBound) {
                // Start and bind the service
                pendingMetadataCall = call
                val intent = Intent(context, MediaSessionService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            } else {
                executeSetMediaSession(call)
            }
        } catch (e: Exception) {
            call.reject("Failed to set media session: ${e.message}", e)
        }
    }

    private fun executeSetMediaSession(call: PluginCall) {
        val title = call.getString("title") ?: "Unknown Title"
        val artist = call.getString("artist") ?: "Unknown Artist"
        val album = call.getString("album")
        val artworkUrl = call.getString("artworkUrl")
        val duration = call.getDouble("duration")?.toLong()
        val smallIconName = call.getString("smallIconName")

        mediaSessionService?.setMetadata(title, artist, album, artworkUrl, duration, smallIconName)

        // Parse custom buttons if provided
        val buttonsArray = call.getArray("buttons")
        if (buttonsArray != null && buttonsArray.length() > 0) {
            val buttons = (0 until buttonsArray.length()).map { i ->
                val btnObj = buttonsArray.getJSONObject(i)
                MediaSessionService.CustomButton(
                    action = btnObj.optString("action", ""),
                    displayName = btnObj.optString("displayName", ""),
                    iconName = btnObj.optString("iconName", ""),
                    enabled = btnObj.optBoolean("enabled", true),
                    position = btnObj.optString("position", "right")
                )
            }
            mediaSessionService?.updateButtons(buttons)
        }

        call.resolve()
    }

    @PluginMethod
    fun updatePlaybackState(call: PluginCall) {
        try {
            val state = call.getString("state") ?: "none"
            mediaSessionService?.updatePlaybackState(state)
            call.resolve()
        } catch (e: Exception) {
            call.reject("Failed to update playback state: ${e.message}", e)
        }
    }

    @PluginMethod
    fun updatePositionState(call: PluginCall) {
        try {
            val position = call.getDouble("position") ?: 0.0
            val duration = call.getDouble("duration") ?: 0.0
            val playbackRate = call.getFloat("playbackRate") ?: 1.0f
            mediaSessionService?.updatePositionState(position, duration, playbackRate)
            call.resolve()
        } catch (e: Exception) {
            call.reject("Failed to update position state: ${e.message}", e)
        }
    }

    @PluginMethod
    fun updateMediaSessionButtons(call: PluginCall) {
        try {
            val buttonsArray = call.getArray("buttons")
            if (buttonsArray != null) {
                val buttons = (0 until buttonsArray.length()).map { i ->
                    val btnObj = buttonsArray.getJSONObject(i)
                    MediaSessionService.CustomButton(
                        action = btnObj.optString("action", ""),
                        displayName = btnObj.optString("displayName", ""),
                        iconName = btnObj.optString("iconName", ""),
                        enabled = btnObj.optBoolean("enabled", true),
                        position = btnObj.optString("position", "right")
                    )
                }
                mediaSessionService?.updateButtons(buttons)
            }
            call.resolve()
        } catch (e: Exception) {
            call.reject("Failed to update media session buttons: ${e.message}", e)
        }
    }

    @PluginMethod
    fun destroyMediaSession(call: PluginCall) {
        try {
            if (isServiceBound) {
                try {
                    context.unbindService(serviceConnection)
                } catch (e: Exception) { }
                isServiceBound = false
            }

            val intent = Intent(context, MediaSessionService::class.java)
            context.stopService(intent)
            mediaSessionService = null

            call.resolve()
        } catch (e: Exception) {
            call.reject("Failed to destroy media session: ${e.message}", e)
        }
    }

    // ──────────────────────────────────────────────
    // Native API — for other plugins to call directly
    // (No PluginCall/JS bridge needed — works in Doze mode)
    // ──────────────────────────────────────────────

    /**
     * Secondary action listener for native plugin callers.
     * Fires alongside the JS "mediaSessionAction" event, but doesn't need JS to be alive.
     */
    private var nativeActionListener: ((String, Map<String, Any>?) -> Unit)? = null

    /**
     * Register a native action listener. When a notification button is tapped,
     * this listener fires with the action name — no JS bridge involved.
     *
     * Usage from another plugin:
     *   val mediaStore = bridge.getPlugin("CapacitorMediaStore").instance as MediaStorePlugin
     *   mediaStore.setNativeActionListener { action, extras -> ... }
     */
    fun setNativeActionListener(listener: ((String, Map<String, Any>?) -> Unit)?) {
        nativeActionListener = listener
        android.util.Log.d("MediaStorePlugin", "[NativeAPI] Action listener ${if (listener != null) "registered" else "cleared"}")
    }

    /**
     * Start/bind the foreground service and set metadata.
     * Native equivalent of setMediaSession() @PluginMethod.
     */
    fun setMediaSessionNative(
        title: String,
        artist: String,
        album: String? = null,
        artworkUrl: String? = null,
        duration: Long? = null,
        smallIconName: String? = null,
        buttons: List<MediaSessionService.CustomButton>? = null
    ) {
        try {
            if (!isServiceBound) {
                // Start and bind the service, then execute once connected
                pendingNativeMetadata = NativeMetadata(title, artist, album, artworkUrl, duration, smallIconName, buttons)
                val intent = android.content.Intent(context, MediaSessionService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                context.bindService(intent, serviceConnection, android.content.Context.BIND_AUTO_CREATE)
            } else {
                executeSetMediaSessionNative(title, artist, album, artworkUrl, duration, smallIconName, buttons)
            }
        } catch (e: Exception) {
            android.util.Log.e("MediaStorePlugin", "[NativeAPI] setMediaSessionNative failed: ${e.message}")
        }
    }

    /** Internal data class for pending native metadata */
    private data class NativeMetadata(
        val title: String,
        val artist: String,
        val album: String?,
        val artworkUrl: String?,
        val duration: Long?,
        val smallIconName: String?,
        val buttons: List<MediaSessionService.CustomButton>?
    )
    private var pendingNativeMetadata: NativeMetadata? = null

    private fun executeSetMediaSessionNative(
        title: String, artist: String, album: String?,
        artworkUrl: String?, duration: Long?,
        smallIconName: String?,
        buttons: List<MediaSessionService.CustomButton>?
    ) {
        mediaSessionService?.setMetadata(title, artist, album, artworkUrl, duration, smallIconName)
        if (buttons != null && buttons.isNotEmpty()) {
            mediaSessionService?.updateButtons(buttons)
        }
        android.util.Log.d("MediaStorePlugin", "[NativeAPI] ✅ MediaSession set: $title - $artist")
    }

    /**
     * Update playback state (playing/paused).
     * Native equivalent of updatePlaybackState() @PluginMethod.
     */
    fun updatePlaybackStateNative(state: String) {
        if (isServiceBound && mediaSessionService != null) {
            mediaSessionService?.updatePlaybackState(state)
        }
    }

    /**
     * Update position/duration for the progress bar.
     * Native equivalent of updatePositionState() @PluginMethod.
     */
    fun updatePositionStateNative(position: Double, duration: Double, playbackRate: Float) {
        if (isServiceBound && mediaSessionService != null) {
            mediaSessionService?.updatePositionState(position, duration, playbackRate)
        }
    }

    /**
     * Update notification button icons (merge strategy — only matching actions are updated).
     * Native equivalent of updateMediaSessionButtons() @PluginMethod.
     */
    fun updateMediaSessionButtonsNative(buttons: List<MediaSessionService.CustomButton>) {
        if (isServiceBound && mediaSessionService != null) {
            mediaSessionService?.updateButtons(buttons)
        }
    }

    /**
     * Destroy the MediaSession service.
     * Native equivalent of destroyMediaSession() @PluginMethod.
     */
    fun destroyMediaSessionNative() {
        try {
            if (isServiceBound) {
                try { context.unbindService(serviceConnection) } catch (_: Exception) {}
                isServiceBound = false
            }
            val intent = android.content.Intent(context, MediaSessionService::class.java)
            context.stopService(intent)
            mediaSessionService = null
            android.util.Log.d("MediaStorePlugin", "[NativeAPI] ✅ MediaSession destroyed")
        } catch (e: Exception) {
            android.util.Log.e("MediaStorePlugin", "[NativeAPI] Destroy failed: ${e.message}")
        }
    }

    // ──────────────────────────────────────────────
    // Permission helpers
    // ──────────────────────────────────────────────

    override fun hasRequiredPermissions(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+ (API 33+) - Audio-only media permission
                getPermissionState("readMediaAudio") == PermissionState.GRANTED
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
