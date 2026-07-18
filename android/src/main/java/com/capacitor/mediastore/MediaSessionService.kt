package com.capacitor.mediastore

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.palette.graphics.Palette
import java.net.URL
import kotlin.concurrent.thread

// Cross-plugin native bridge: direct control of GhostWebView audio
// without going through the Capacitor JS bridge (which Android throttles
// when the screen is off). Uses REFLECTION to call MediaBackgroundPlugin
// so there is zero compile-time Gradle dependency between the two modules.

/**
 * Native Media Session Foreground Service.
 * Creates a persistent media notification with configurable buttons.
 *
 * Buttons support a `position` field: "left" (before prev) or "right" (after next).
 * Each button's `iconName` must match a drawable resource in the consuming app's
 * `res/drawable/` folder (e.g., `ic_shuffle` → `res/drawable/ic_shuffle.xml`).
 *
 * Icon Resources Guide:
 * - Place vector drawables (24dp, XML format) in: `android/app/src/main/res/drawable/`
 * - Naming convention: `ic_<action_name>.xml` (e.g., `ic_shuffle.xml`, `ic_favorite.xml`)
 * - Recommended size: 24×24dp vector or 48×48px PNG (mdpi)
 * - For the small notification icon (`smallIconName`): use a monochrome white-on-transparent
 *   PNG or vector at 24×24dp. Place it as `ic_notification.xml` in `res/drawable/`.
 *
 * Communicates back to the Capacitor plugin via a listener interface.
 */
class MediaSessionService : Service() {

    companion object {
        private const val TAG = "MediaSessionService"
        const val CHANNEL_ID = "media_session_channel"
        const val NOTIFICATION_ID = 9999

        const val ACTION_PLAY = "com.capacitor.mediastore.ACTION_PLAY"
        const val ACTION_PAUSE = "com.capacitor.mediastore.ACTION_PAUSE"
        const val ACTION_NEXT = "com.capacitor.mediastore.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.capacitor.mediastore.ACTION_PREVIOUS"
        const val ACTION_STOP = "com.capacitor.mediastore.ACTION_STOP"
        const val ACTION_SEEK_TO = "com.capacitor.mediastore.ACTION_SEEK_TO"
        const val ACTION_SEEK_FORWARD = "com.capacitor.mediastore.ACTION_SEEK_FORWARD"
        const val ACTION_SEEK_BACKWARD = "com.capacitor.mediastore.ACTION_SEEK_BACKWARD"
        const val EXTRA_SEEK_POSITION = "seekPosition"

        // Prefix for custom button actions
        const val ACTION_CUSTOM_PREFIX = "com.capacitor.mediastore.ACTION_CUSTOM_"
    }

    interface MediaSessionActionListener {
        fun onAction(action: String, extras: Map<String, Any>? = null)
    }

    /**
     * Reflection-based helper to call MediaBackgroundPlugin methods
     * without a compile-time Gradle dependency on capacitor-media-background.
     * Returns the singleton instance of MediaBackgroundPlugin, or null.
     */
    private fun getMediaBackgroundInstance(): Any? {
        return try {
            val clazz = Class.forName("com.odioncloud.plugins.mediabackground.MediaBackgroundPlugin")
            val field = clazz.getDeclaredField("instance")
            field.isAccessible = true
            // Kotlin companion object: the field is on the Companion class
            val companion = clazz.getDeclaredField("Companion").get(null)
            val instanceGetter = companion.javaClass.getMethod("getInstance")
            instanceGetter.invoke(companion)
        } catch (e: Exception) {
            null
        }
    }

    /** Directly pause the GhostWebView audio via reflection. */
    private fun nativeBridgePause() {
        try {
            val instance = getMediaBackgroundInstance() ?: return
            instance.javaClass.getMethod("nativePause").invoke(instance)
        } catch (e: Exception) {
            Log.w(TAG, "nativeBridgePause failed: ${e.message}")
        }
    }

    /** Directly resume the GhostWebView audio via reflection. */
    private fun nativeBridgeResume() {
        try {
            val instance = getMediaBackgroundInstance() ?: return
            instance.javaClass.getMethod("nativeResume").invoke(instance)
        } catch (e: Exception) {
            Log.w(TAG, "nativeBridgeResume failed: ${e.message}")
        }
    }

    private val binder = MediaSessionBinder()
    private var mediaSession: MediaSessionCompat? = null
    private var notificationManager: NotificationManager? = null

    // Current state
    private var currentTitle: String = ""
    private var currentArtist: String = ""
    private var currentAlbum: String = ""
    private var currentArtwork: Bitmap? = null
    private var currentDuration: Long = 0L
    private var currentPosition: Long = 0L
    private var currentPlaybackRate: Float = 1.0f
    private var isPlaying: Boolean = false
    private var smallIconResId: Int = 0

    // Palette color extracted from artwork (for colorized notification)
    private var artworkDominantColor: Int = 0

    // Custom buttons state
    private var customButtons: List<CustomButton> = emptyList()

    var actionListener: MediaSessionActionListener? = null

    /**
     * Represents a custom button in the notification.
     * @param action    Unique action identifier (e.g., "like", "shuffle", "repeat")
     * @param displayName  Accessibility label shown to users
     * @param iconName  Name of the drawable resource WITHOUT extension (e.g., "ic_shuffle")
     * @param enabled   Whether this button is visible/enabled
     * @param position  "left" = before transport controls, "right" = after transport controls
     */
    data class CustomButton(
        val action: String,
        val displayName: String,
        val iconName: String,
        val enabled: Boolean = true,
        val position: String = "right" // "left" or "right"
    )

    inner class MediaSessionBinder : Binder() {
        fun getService(): MediaSessionService = this@MediaSessionService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        setupMediaSession()
        Log.d(TAG, "MediaSessionService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                ACTION_PLAY -> {
                    isPlaying = true
                    updatePlaybackStateInternal()
                    updateNotification()
                    // NATIVE BRIDGE: directly resume GhostWebView audio.
                    // This works even when the Capacitor JS bridge is throttled/frozen
                    // by Android after prolonged background playback.
                    nativeBridgeResume()
                    // Also notify JS (works when JS bridge is responsive)
                    actionListener?.onAction("play")
                }
                ACTION_PAUSE -> {
                    isPlaying = false
                    updatePlaybackStateInternal()
                    updateNotification()
                    // NATIVE BRIDGE: directly pause GhostWebView audio
                    nativeBridgePause()
                    // Also notify JS
                    actionListener?.onAction("pause")
                }
                ACTION_NEXT -> actionListener?.onAction("nexttrack")
                ACTION_PREVIOUS -> actionListener?.onAction("previoustrack")
                ACTION_STOP -> {
                    isPlaying = false
                    updatePlaybackStateInternal()
                    actionListener?.onAction("stop")
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
                ACTION_SEEK_TO -> {
                    val pos = intent.getLongExtra(EXTRA_SEEK_POSITION, 0L)
                    actionListener?.onAction("seekto", mapOf("seekTime" to pos))
                }
                ACTION_SEEK_FORWARD -> actionListener?.onAction("seekforward")
                ACTION_SEEK_BACKWARD -> actionListener?.onAction("seekbackward")
                else -> {
                    // Handle custom button actions (ACTION_CUSTOM_<ACTION_NAME>)
                    if (action.startsWith(ACTION_CUSTOM_PREFIX)) {
                        val customAction = action.removePrefix(ACTION_CUSTOM_PREFIX).lowercase()
                        actionListener?.onAction(customAction)
                    }
                }
            }
        }
        return START_NOT_STICKY // Don't auto-restart if the app process is killed
    }

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        Log.d(TAG, "MediaSessionService destroyed")
        super.onDestroy()
    }

    /**
     * Called when the user swipes the app away from the recent apps list.
     * Stop the foreground service so the notification doesn't linger with dead buttons.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d(TAG, "App task removed — stopping media session service")
        isPlaying = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Media Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Media playback controls"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, "CapacitorMediaStore").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )

            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    isPlaying = true
                    updatePlaybackStateInternal()
                    updateNotification()
                    // NATIVE BRIDGE: directly resume GhostWebView audio
                    nativeBridgeResume()
                    actionListener?.onAction("play")
                }

                override fun onPause() {
                    isPlaying = false
                    updatePlaybackStateInternal()
                    updateNotification()
                    // NATIVE BRIDGE: directly pause GhostWebView audio
                    nativeBridgePause()
                    actionListener?.onAction("pause")
                }

                override fun onSkipToNext() {
                    actionListener?.onAction("nexttrack")
                }

                override fun onSkipToPrevious() {
                    actionListener?.onAction("previoustrack")
                }

                override fun onStop() {
                    isPlaying = false
                    updatePlaybackStateInternal()
                    actionListener?.onAction("stop")
                }

                override fun onSeekTo(pos: Long) {
                    currentPosition = pos
                    updatePlaybackStateInternal()
                    actionListener?.onAction("seekto", mapOf("seekTime" to (pos / 1000.0)))
                }

                override fun onFastForward() {
                    actionListener?.onAction("seekforward")
                }

                override fun onRewind() {
                    actionListener?.onAction("seekbackward")
                }

                override fun onCustomAction(action: String?, extras: android.os.Bundle?) {
                    action?.let {
                        actionListener?.onAction(it)
                    }
                }
            })

            isActive = true
        }
    }

    /**
     * Set or update the media metadata shown in the notification.
     */
    fun setMetadata(
        title: String,
        artist: String,
        album: String?,
        artworkUrl: String?,
        duration: Long?,
        smallIconName: String?
    ) {
        currentTitle = title
        currentArtist = artist
        currentAlbum = album ?: ""
        currentDuration = (duration ?: 0L) * 1000L // Convert seconds to ms

        // Resolve the small icon
        smallIconResId = if (!smallIconName.isNullOrEmpty()) {
            resources.getIdentifier(smallIconName, "drawable", packageName).takeIf { it != 0 }
                ?: resources.getIdentifier("ic_notification", "drawable", packageName).takeIf { it != 0 }
                ?: android.R.drawable.ic_media_play
        } else {
            resources.getIdentifier("ic_notification", "drawable", packageName).takeIf { it != 0 }
                ?: android.R.drawable.ic_media_play
        }

        // Update MediaSession metadata
        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, currentAlbum)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, currentDuration)

        if (currentArtwork != null) {
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, currentArtwork)
        }

        mediaSession?.setMetadata(metadataBuilder.build())

        // Fetch artwork in background
        if (!artworkUrl.isNullOrEmpty()) {
            thread {
                try {
                    val bitmap = loadArtworkBitmap(artworkUrl)

                    if (bitmap != null) {
                        currentArtwork = bitmap

                        // Extract dominant color for notification background
                        try {
                            val palette = Palette.from(bitmap).generate()
                            artworkDominantColor = palette.getDarkMutedColor(
                                palette.getMutedColor(0x333333)
                            )
                        } catch (e: Exception) {
                            Log.w(TAG, "Palette extraction failed: ${e.message}")
                        }

                        // Re-update metadata with artwork
                        val updatedMetadata = MediaMetadataCompat.Builder()
                            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentTitle)
                            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentArtist)
                            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, currentAlbum)
                            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, currentDuration)
                            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
                            .build()

                        mediaSession?.setMetadata(updatedMetadata)
                        updateNotification()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load artwork: ${e.message}")
                }
            }
        }

        updatePlaybackStateInternal()
        updateNotification()
    }

    /**
     * Update the playback state (playing, paused, buffering, stopped).
     */
    fun updatePlaybackState(state: String) {
        isPlaying = state == "playing"
        updatePlaybackStateInternal()
        updateNotification()
    }

    /**
     * Update the position state for the progress bar.
     */
    fun updatePositionState(position: Double, duration: Double, playbackRate: Float) {
        currentPosition = (position * 1000).toLong()
        currentDuration = (duration * 1000).toLong()
        currentPlaybackRate = playbackRate

        // Update metadata duration
        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentTitle)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentArtist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, currentAlbum)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, currentDuration)
        if (currentArtwork != null) {
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, currentArtwork)
        }
        mediaSession?.setMetadata(metadataBuilder.build())

        updatePlaybackStateInternal()
    }

    /**
     * Update the custom buttons shown in the notification.
     */
    fun updateButtons(buttons: List<CustomButton>) {
        customButtons = buttons
        updatePlaybackStateInternal()
        updateNotification()
    }

    private fun updatePlaybackStateInternal() {
        val stateValue = if (isPlaying) {
            PlaybackStateCompat.STATE_PLAYING
        } else {
            PlaybackStateCompat.STATE_PAUSED
        }

        val actions = PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_STOP or
                PlaybackStateCompat.ACTION_SEEK_TO or
                PlaybackStateCompat.ACTION_FAST_FORWARD or
                PlaybackStateCompat.ACTION_REWIND

        val stateBuilder = PlaybackStateCompat.Builder()
            .setState(stateValue, currentPosition, currentPlaybackRate)
            .setActions(actions)

        // Add custom actions (for MediaSession system UI on some devices)
        for (btn in customButtons) {
            if (btn.enabled) {
                val iconRes = resolveIconResource(btn.iconName)
                stateBuilder.addCustomAction(
                    PlaybackStateCompat.CustomAction.Builder(btn.action, btn.displayName, iconRes)
                        .build()
                )
            }
        }

        mediaSession?.setPlaybackState(stateBuilder.build())
    }

    /**
     * Load artwork bitmap from various sources:
     * - data: URI (base64 encoded)
     * - Local file path (file:///storage/..., /storage/...)
     * - content:// URI
     * - Embedded album art (for audio files with no separate thumbnail)
     * - HTTP/HTTPS URL
     */
    private fun loadArtworkBitmap(artworkUrl: String): Bitmap? {
        return try {
            when {
                // 1. Base64 data URI
                artworkUrl.startsWith("data:") -> {
                    val base64Data = artworkUrl.substringAfter(",")
                    val bytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }

                // 2. Local file path (file:// or absolute /storage/ path)
                artworkUrl.startsWith("file://") || artworkUrl.startsWith("/") -> {
                    val filePath = if (artworkUrl.startsWith("file://")) {
                        artworkUrl.removePrefix("file://")
                    } else {
                        artworkUrl
                    }
                    // First try direct decode (works for image files)
                    var bitmap = BitmapFactory.decodeFile(filePath)
                    // If null, try extracting embedded album art (audio files)
                    if (bitmap == null) {
                        bitmap = extractEmbeddedArt(filePath)
                    }
                    bitmap
                }

                // 3. Content URI
                artworkUrl.startsWith("content://") -> {
                    try {
                        val uri = android.net.Uri.parse(artworkUrl)
                        val inputStream = contentResolver.openInputStream(uri)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream?.close()
                        bitmap
                    } catch (e: Exception) {
                        Log.w(TAG, "Content URI artwork failed: ${e.message}")
                        null
                    }
                }

                // 4. HTTP/HTTPS URL
                else -> {
                    val connection = URL(artworkUrl).openConnection()
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    BitmapFactory.decodeStream(connection.getInputStream())
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "loadArtworkBitmap failed for '$artworkUrl': ${e.message}")
            null
        }
    }

    /**
     * Extract embedded album art from an audio file using MediaMetadataRetriever.
     * Works for MP3 (ID3), FLAC, OGG, M4A, etc.
     */
    private fun extractEmbeddedArt(filePath: String): Bitmap? {
        return try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val artBytes = retriever.embeddedPicture
            retriever.release()
            if (artBytes != null) {
                BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Embedded art extraction failed: ${e.message}")
            null
        }
    }

    /**
     * Resolve a drawable resource by name from the consuming app's res/drawable folder.
     * Falls back to a generic Android icon if not found.
     */
    private fun resolveIconResource(iconName: String): Int {
        if (iconName.isEmpty()) return android.R.drawable.ic_media_play

        // Try exact name first (e.g., "ic_shuffle")
        val exactRes = resources.getIdentifier(iconName, "drawable", packageName)
        if (exactRes != 0) return exactRes

        // Try with ic_ prefix (e.g., "shuffle" → "ic_shuffle")
        val prefixedRes = resources.getIdentifier("ic_$iconName", "drawable", packageName)
        if (prefixedRes != 0) return prefixedRes

        // Try with ms_ prefix for mediastore-specific icons
        val msRes = resources.getIdentifier("ms_$iconName", "drawable", packageName)
        if (msRes != 0) return msRes

        // Log a warning to help developers debug missing icons
        Log.w(TAG, "Icon drawable '$iconName' not found in res/drawable. " +
            "Please add it as 'res/drawable/$iconName.xml' (24dp vector) or " +
            "'res/drawable/ic_$iconName.xml'. Using fallback icon.")

        return android.R.drawable.ic_media_play
    }

    /**
     * Build and display the notification.
     */
    private fun updateNotification() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val sessionToken = mediaSession?.sessionToken

        // Create pending intent that launches the app when notification is tapped
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build action list
        val notificationActions = mutableListOf<NotificationCompat.Action>()
        // Indices of actions to show in compact view (max 3)
        val compactViewIndices = mutableListOf<Int>()

        // --- LEFT-positioned custom buttons (before transport controls) ---
        for (btn in customButtons) {
            if (btn.enabled && btn.position == "left") {
                val iconRes = resolveIconResource(btn.iconName)
                val actionStr = ACTION_CUSTOM_PREFIX + btn.action.uppercase()
                notificationActions.add(
                    NotificationCompat.Action.Builder(iconRes, btn.displayName, buildActionPendingIntent(actionStr))
                        .build()
                )
            }
        }

        // --- Previous ---
        val prevIconRes = resolveIconResource("ic_skip_previous")
        notificationActions.add(
            NotificationCompat.Action.Builder(prevIconRes, "Previous", buildActionPendingIntent(ACTION_PREVIOUS))
                .build()
        )
        compactViewIndices.add(notificationActions.size - 1)

        // --- Play / Pause ---
        if (isPlaying) {
            val pauseIconRes = resolveIconResource("ic_pause")
            notificationActions.add(
                NotificationCompat.Action.Builder(pauseIconRes, "Pause", buildActionPendingIntent(ACTION_PAUSE))
                    .build()
            )
        } else {
            val playIconRes = resolveIconResource("ic_play")
            notificationActions.add(
                NotificationCompat.Action.Builder(playIconRes, "Play", buildActionPendingIntent(ACTION_PLAY))
                    .build()
            )
        }
        compactViewIndices.add(notificationActions.size - 1)

        // --- Next ---
        val nextIconRes = resolveIconResource("ic_skip_next")
        notificationActions.add(
            NotificationCompat.Action.Builder(nextIconRes, "Next", buildActionPendingIntent(ACTION_NEXT))
                .build()
        )
        compactViewIndices.add(notificationActions.size - 1)

        // --- RIGHT-positioned custom buttons (after transport controls) ---
        for (btn in customButtons) {
            if (btn.enabled && btn.position == "right") {
                val iconRes = resolveIconResource(btn.iconName)
                val actionStr = ACTION_CUSTOM_PREFIX + btn.action.uppercase()
                notificationActions.add(
                    NotificationCompat.Action.Builder(iconRes, btn.displayName, buildActionPendingIntent(actionStr))
                        .build()
                )
            }
        }

        val resolvedSmallIcon = if (smallIconResId != 0) smallIconResId else android.R.drawable.ic_media_play

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(currentTitle)
            .setContentText(currentArtist)
            .setSubText(currentAlbum.ifEmpty { null })
            .setSmallIcon(resolvedSmallIcon)
            .setContentIntent(contentPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .setShowWhen(false)
            .setStyle(
                MediaStyle()
                    .setMediaSession(sessionToken)
                    .setShowActionsInCompactView(*compactViewIndices.toIntArray())
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(buildActionPendingIntent(ACTION_STOP))
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)

        if (currentArtwork != null) {
            builder.setLargeIcon(currentArtwork)
        }

        // Colorized notification background (artwork-tinted, like YouTube Music / Spotify)
        if (artworkDominantColor != 0) {
            builder.setColorized(true)
            builder.setColor(artworkDominantColor)
        }

        // Add all notification actions
        for (action in notificationActions) {
            builder.addAction(action)
        }

        // Add delete intent (when notification is swiped away)
        builder.setDeleteIntent(buildActionPendingIntent(ACTION_STOP))

        return builder.build()
    }

    private fun buildActionPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, MediaSessionService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this, action.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
