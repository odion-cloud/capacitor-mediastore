import Foundation
import Photos
import Capacitor
import MediaPlayer
import AVFoundation

@objc public class CapacitorMediaStore: NSObject, PHPhotoLibraryChangeObserver {
    
    // Reference to the plugin for event emission
    weak var plugin: CAPPlugin?
    
    // ──────────────────────────────────────────────
    // File Watcher State
    // ──────────────────────────────────────────────
    
    private var isWatching = false
    private var watchedMediaTypes: [String] = ["all"]
    private var previousFetchResult: PHFetchResult<PHAsset>?
    
    // ──────────────────────────────────────────────
    // Media Session State
    // ──────────────────────────────────────────────
    
    private var isMediaSessionActive = false
    private var nowPlayingInfo: [String: Any] = [:]
    private var currentDuration: Double = 0
    private var currentPosition: Double = 0
    private var currentPlaybackRate: Float = 1.0
    private var isPlaying = false
    
    // Native action listener for other native plugins (no JS bridge needed)
    private var nativeActionListener: ((String, [String: Any]?) -> Void)?
    
    // ──────────────────────────────────────────────
    // Existing Media Store methods
    // ──────────────────────────────────────────────
    
    @objc public func getMedias(_ call: CAPPluginCall) {
        guard PHPhotoLibrary.authorizationStatus() == .authorized else {
            call.reject("Permission denied. Please grant photo library access.")
            return
        }
        
        let result: [String: Any] = [
            "media": [],
            "totalCount": 0,
            "hasMore": false
        ]
        call.resolve(result)
    }
    
    @objc public func getMediasByType(_ call: CAPPluginCall) {
        guard PHPhotoLibrary.authorizationStatus() == .authorized else {
            call.reject("Permission denied. Please grant photo library access.")
            return
        }
        
        let result: [String: Any] = [
            "media": [],
            "totalCount": 0,
            "hasMore": false
        ]
        call.resolve(result)
    }
    
    @objc public func getAlbums(_ call: CAPPluginCall) {
        guard PHPhotoLibrary.authorizationStatus() == .authorized else {
            call.reject("Permission denied. Please grant photo library access.")
            return
        }
        
        let result: [String: Any] = [
            "albums": [],
            "totalCount": 0
        ]
        call.resolve(result)
    }
    
    @objc public func saveMedia(_ call: CAPPluginCall) {
        guard PHPhotoLibrary.authorizationStatus() == .authorized else {
            call.reject("Permission denied. Please grant photo library access.")
            return
        }
        
        let result: [String: Any] = [
            "success": false,
            "error": "Not implemented on iOS"
        ]
        call.resolve(result)
    }
    
    @objc public func getMediaMetadata(_ call: CAPPluginCall) {
        guard PHPhotoLibrary.authorizationStatus() == .authorized else {
            call.reject("Permission denied. Please grant photo library access.")
            return
        }
        
        let result: [String: Any] = [
            "media": [:]
        ]
        call.resolve(result)
    }
    
    @objc public func checkPermissions(_ call: CAPPluginCall) {
        let status = PHPhotoLibrary.authorizationStatus()
        let permissionState: String
        
        switch status {
        case .authorized:
            permissionState = "granted"
        case .denied, .restricted:
            permissionState = "denied"
        case .notDetermined:
            permissionState = "prompt"
        case .limited:
            if #available(iOS 14, *) {
                permissionState = "granted" // Limited access is still functional
            } else {
                permissionState = "denied"
            }
        @unknown default:
            permissionState = "denied"
        }
        
        let result: [String: Any] = [
            "readPhotoLibrary": permissionState
        ]
        call.resolve(result)
    }
    
    @objc public func requestPermissions(_ call: CAPPluginCall) {
        let currentStatus = PHPhotoLibrary.authorizationStatus()
        
        if currentStatus == .authorized {
            let result: [String: Any] = [
                "readPhotoLibrary": "granted"
            ]
            call.resolve(result)
            return
        }
        
        if #available(iOS 14, *) {
            PHPhotoLibrary.requestAuthorization(for: .readWrite) { status in
                DispatchQueue.main.async {
                    self.resolvePermissionResult(call, status: status)
                }
            }
        } else {
            PHPhotoLibrary.requestAuthorization { status in
                DispatchQueue.main.async {
                    self.resolvePermissionResult(call, status: status)
                }
            }
        }
    }
    
    private func resolvePermissionResult(_ call: CAPPluginCall, status: PHAuthorizationStatus) {
        let permissionState: String
        
        switch status {
        case .authorized:
            permissionState = "granted"
        case .denied, .restricted:
            permissionState = "denied"
        case .notDetermined:
            permissionState = "prompt"
        case .limited:
            if #available(iOS 14, *) {
                permissionState = "granted"
            } else {
                permissionState = "denied"
            }
        @unknown default:
            permissionState = "denied"
        }
        
        let result: [String: Any] = [
            "readPhotoLibrary": permissionState
        ]
        call.resolve(result)
    }
    
    // ──────────────────────────────────────────────
    // File Change Watcher
    // ──────────────────────────────────────────────
    
    @objc public func startWatching(_ call: CAPPluginCall) {
        guard PHPhotoLibrary.authorizationStatus() == .authorized else {
            call.reject("Permission denied. Please grant photo library access to watch for changes.")
            return
        }
        
        if isWatching {
            stopWatchingInternal()
        }
        
        // Parse media types
        if let types = call.getArray("mediaTypes") as? [String], !types.isEmpty {
            watchedMediaTypes = types
        } else {
            watchedMediaTypes = ["all"]
        }
        
        // Build initial fetch result for comparison
        previousFetchResult = buildFetchResult()
        
        // Register as observer
        PHPhotoLibrary.shared().register(self)
        isWatching = true
        
        call.resolve()
    }
    
    @objc public func stopWatching(_ call: CAPPluginCall) {
        stopWatchingInternal()
        call.resolve()
    }
    
    private func stopWatchingInternal() {
        if isWatching {
            PHPhotoLibrary.shared().unregisterChangeObserver(self)
            isWatching = false
            previousFetchResult = nil
        }
    }
    
    private func buildFetchResult() -> PHFetchResult<PHAsset> {
        let fetchOptions = PHFetchOptions()
        fetchOptions.sortDescriptors = [NSSortDescriptor(key: "creationDate", ascending: false)]
        
        // Build predicate based on watched types
        if watchedMediaTypes.contains("all") {
            return PHAsset.fetchAssets(with: fetchOptions)
        }
        
        var predicates: [NSPredicate] = []
        if watchedMediaTypes.contains("image") {
            predicates.append(NSPredicate(format: "mediaType == %d", PHAssetMediaType.image.rawValue))
        }
        if watchedMediaTypes.contains("video") {
            predicates.append(NSPredicate(format: "mediaType == %d", PHAssetMediaType.video.rawValue))
        }
        if watchedMediaTypes.contains("audio") {
            predicates.append(NSPredicate(format: "mediaType == %d", PHAssetMediaType.audio.rawValue))
        }
        
        if !predicates.isEmpty {
            fetchOptions.predicate = NSCompoundPredicate(orPredicateWithSubpredicates: predicates)
        }
        
        return PHAsset.fetchAssets(with: fetchOptions)
    }
    
    // PHPhotoLibraryChangeObserver
    public func photoLibraryDidChange(_ changeInstance: PHChange) {
        guard let previousResult = previousFetchResult else { return }
        
        guard let changes = changeInstance.changeDetails(for: previousResult) else { return }
        
        // Update snapshot
        previousFetchResult = changes.fetchResultAfterChanges
        
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            
            // Inserted assets
            if let insertedObjects = changes.insertedObjects as? [PHAsset] {
                for asset in insertedObjects {
                    let fileData = self.assetToJSObject(asset)
                    let event: [String: Any] = [
                        "type": "added",
                        "file": fileData
                    ]
                    self.plugin?.notifyListeners("mediaChanged", data: event)
                }
            }
            
            // Removed assets
            if let removedObjects = changes.removedObjects as? [PHAsset] {
                for asset in removedObjects {
                    let fileData = self.assetToJSObject(asset)
                    let event: [String: Any] = [
                        "type": "deleted",
                        "file": fileData
                    ]
                    self.plugin?.notifyListeners("mediaChanged", data: event)
                }
            }
            
            // Changed assets
            if let changedObjects = changes.changedObjects as? [PHAsset] {
                for asset in changedObjects {
                    let fileData = self.assetToJSObject(asset)
                    let event: [String: Any] = [
                        "type": "modified",
                        "file": fileData
                    ]
                    self.plugin?.notifyListeners("mediaChanged", data: event)
                }
            }
        }
    }
    
    private func assetToJSObject(_ asset: PHAsset) -> [String: Any] {
        var data: [String: Any] = [
            "id": asset.localIdentifier,
            "displayName": (asset.value(forKey: "filename") as? String) ?? "Unknown",
            "dateAdded": (asset.creationDate?.timeIntervalSince1970 ?? 0) * 1000,
            "dateModified": (asset.modificationDate?.timeIntervalSince1970 ?? 0) * 1000,
            "size": 0
        ]
        
        switch asset.mediaType {
        case .image:
            data["mediaType"] = "image"
            data["mimeType"] = "image/jpeg"
            data["width"] = asset.pixelWidth
            data["height"] = asset.pixelHeight
        case .video:
            data["mediaType"] = "video"
            data["mimeType"] = "video/mp4"
            data["width"] = asset.pixelWidth
            data["height"] = asset.pixelHeight
            data["duration"] = asset.duration * 1000 // Convert to ms
        case .audio:
            data["mediaType"] = "audio"
            data["mimeType"] = "audio/mpeg"
            data["duration"] = asset.duration * 1000
        default:
            data["mediaType"] = "document"
            data["mimeType"] = "application/octet-stream"
        }
        
        return data
    }
    
    // ──────────────────────────────────────────────
    // Native Media Session (iOS: MPNowPlayingInfoCenter + MPRemoteCommandCenter)
    // ──────────────────────────────────────────────
    
    @objc public func setMediaSession(_ call: CAPPluginCall) {
        let title = call.getString("title") ?? "Unknown Title"
        let artist = call.getString("artist") ?? "Unknown Artist"
        let album = call.getString("album") ?? ""
        let artworkUrl = call.getString("artworkUrl")
        let duration = call.getDouble("duration") ?? 0
        
        currentDuration = duration
        
        // Set up audio session for background playback
        do {
            try AVAudioSession.sharedInstance().setCategory(.playback, mode: .default)
            try AVAudioSession.sharedInstance().setActive(true)
        } catch {
            print("CapacitorMediaStore: Failed to configure audio session: \(error)")
        }
        
        // Build now playing info
        nowPlayingInfo = [
            MPMediaItemPropertyTitle: title,
            MPMediaItemPropertyArtist: artist,
            MPMediaItemPropertyAlbumTitle: album,
            MPMediaItemPropertyPlaybackDuration: duration,
            MPNowPlayingInfoPropertyElapsedPlaybackTime: currentPosition,
            MPNowPlayingInfoPropertyPlaybackRate: isPlaying ? currentPlaybackRate : 0.0
        ]
        
        MPNowPlayingInfoCenter.default().nowPlayingInfo = nowPlayingInfo
        
        // Set up remote command handlers
        if !isMediaSessionActive {
            setupRemoteCommandCenter()
            isMediaSessionActive = true
        }
        
        // Parse and configure custom buttons
        if let buttonsArray = call.getArray("buttons") as? [[String: Any]] {
            configureCustomButtons(buttonsArray)
        }
        
        // Load artwork asynchronously
        if let artworkUrl = artworkUrl, !artworkUrl.isEmpty {
            loadArtwork(from: artworkUrl)
        }
        
        call.resolve()
    }
    
    @objc public func updatePlaybackState(_ call: CAPPluginCall) {
        let state = call.getString("state") ?? "none"
        isPlaying = (state == "playing")
        
        nowPlayingInfo[MPNowPlayingInfoPropertyPlaybackRate] = isPlaying ? NSNumber(value: currentPlaybackRate) : NSNumber(value: 0.0)
        nowPlayingInfo[MPNowPlayingInfoPropertyElapsedPlaybackTime] = currentPosition
        MPNowPlayingInfoCenter.default().nowPlayingInfo = nowPlayingInfo
        
        call.resolve()
    }
    
    @objc public func updatePositionState(_ call: CAPPluginCall) {
        currentPosition = call.getDouble("position") ?? 0
        currentDuration = call.getDouble("duration") ?? currentDuration
        currentPlaybackRate = call.getFloat("playbackRate") ?? 1.0
        
        nowPlayingInfo[MPMediaItemPropertyPlaybackDuration] = currentDuration
        nowPlayingInfo[MPNowPlayingInfoPropertyElapsedPlaybackTime] = currentPosition
        nowPlayingInfo[MPNowPlayingInfoPropertyPlaybackRate] = isPlaying ? NSNumber(value: currentPlaybackRate) : NSNumber(value: 0.0)
        MPNowPlayingInfoCenter.default().nowPlayingInfo = nowPlayingInfo
        
        call.resolve()
    }
    
    @objc public func updateMediaSessionButtons(_ call: CAPPluginCall) {
        if let buttonsArray = call.getArray("buttons") as? [[String: Any]] {
            configureCustomButtons(buttonsArray)
        }
        call.resolve()
    }
    
    @objc public func destroyMediaSession(_ call: CAPPluginCall) {
        let commandCenter = MPRemoteCommandCenter.shared()
        
        // Disable all commands
        commandCenter.playCommand.isEnabled = false
        commandCenter.pauseCommand.isEnabled = false
        commandCenter.togglePlayPauseCommand.isEnabled = false
        commandCenter.nextTrackCommand.isEnabled = false
        commandCenter.previousTrackCommand.isEnabled = false
        commandCenter.changePlaybackPositionCommand.isEnabled = false
        commandCenter.skipForwardCommand.isEnabled = false
        commandCenter.skipBackwardCommand.isEnabled = false
        commandCenter.likeCommand.isEnabled = false
        commandCenter.changeShuffleModeCommand.isEnabled = false
        commandCenter.changeRepeatModeCommand.isEnabled = false
        
        // Remove all targets
        commandCenter.playCommand.removeTarget(nil)
        commandCenter.pauseCommand.removeTarget(nil)
        commandCenter.togglePlayPauseCommand.removeTarget(nil)
        commandCenter.nextTrackCommand.removeTarget(nil)
        commandCenter.previousTrackCommand.removeTarget(nil)
        commandCenter.changePlaybackPositionCommand.removeTarget(nil)
        commandCenter.skipForwardCommand.removeTarget(nil)
        commandCenter.skipBackwardCommand.removeTarget(nil)
        commandCenter.likeCommand.removeTarget(nil)
        commandCenter.changeShuffleModeCommand.removeTarget(nil)
        commandCenter.changeRepeatModeCommand.removeTarget(nil)
        
        // Clear now playing info
        MPNowPlayingInfoCenter.default().nowPlayingInfo = nil
        nowPlayingInfo = [:]
        isMediaSessionActive = false
        isPlaying = false
        currentPosition = 0
        currentDuration = 0
        
        call.resolve()
    }
    
    // ──────────────────────────────────────────────
    // Native API — for other plugins to call directly
    // (No CAPPluginCall/JS bridge needed)
    // ──────────────────────────────────────────────
    
    /**
     * Register a native action listener. When a media button is tapped,
     * this listener fires alongside the JS "mediaSessionAction" event.
     */
    public func setNativeActionListener(_ listener: ((String, [String: Any]?) -> Void)?) {
        nativeActionListener = listener
    }
    
    /**
     * Set media session metadata from native code (no CAPPluginCall needed).
     */
    public func setMediaSessionNative(
        title: String,
        artist: String,
        album: String? = nil,
        artworkUrl: String? = nil,
        duration: Double? = nil,
        buttons: [[String: Any]]? = nil
    ) {
        currentDuration = duration ?? 0
        
        do {
            try AVAudioSession.sharedInstance().setCategory(.playback, mode: .default)
            try AVAudioSession.sharedInstance().setActive(true)
        } catch {
            print("CapacitorMediaStore: Failed to configure audio session: \(error)")
        }
        
        nowPlayingInfo = [
            MPMediaItemPropertyTitle: title,
            MPMediaItemPropertyArtist: artist,
            MPMediaItemPropertyAlbumTitle: album ?? "",
            MPMediaItemPropertyPlaybackDuration: currentDuration,
            MPNowPlayingInfoPropertyElapsedPlaybackTime: currentPosition,
            MPNowPlayingInfoPropertyPlaybackRate: isPlaying ? currentPlaybackRate : 0.0
        ]
        
        MPNowPlayingInfoCenter.default().nowPlayingInfo = nowPlayingInfo
        
        if !isMediaSessionActive {
            setupRemoteCommandCenter()
            isMediaSessionActive = true
        }
        
        if let buttons = buttons {
            configureCustomButtons(buttons)
        }
        
        if let artworkUrl = artworkUrl, !artworkUrl.isEmpty {
            loadArtwork(from: artworkUrl)
        }
    }
    
    /**
     * Update playback state from native code.
     */
    public func updatePlaybackStateNative(state: String) {
        isPlaying = (state == "playing")
        nowPlayingInfo[MPNowPlayingInfoPropertyPlaybackRate] = isPlaying ? NSNumber(value: currentPlaybackRate) : NSNumber(value: 0.0)
        nowPlayingInfo[MPNowPlayingInfoPropertyElapsedPlaybackTime] = currentPosition
        MPNowPlayingInfoCenter.default().nowPlayingInfo = nowPlayingInfo
    }
    
    /**
     * Update position/duration from native code.
     */
    public func updatePositionStateNative(position: Double, duration: Double, playbackRate: Float) {
        currentPosition = position
        currentDuration = duration
        currentPlaybackRate = playbackRate
        
        nowPlayingInfo[MPMediaItemPropertyPlaybackDuration] = currentDuration
        nowPlayingInfo[MPNowPlayingInfoPropertyElapsedPlaybackTime] = currentPosition
        nowPlayingInfo[MPNowPlayingInfoPropertyPlaybackRate] = isPlaying ? NSNumber(value: currentPlaybackRate) : NSNumber(value: 0.0)
        MPNowPlayingInfoCenter.default().nowPlayingInfo = nowPlayingInfo
    }
    
    /**
     * Update custom buttons from native code.
     */
    public func updateMediaSessionButtonsNative(buttons: [[String: Any]]) {
        configureCustomButtons(buttons)
    }
    
    /**
     * Destroy media session from native code.
     */
    public func destroyMediaSessionNative() {
        let commandCenter = MPRemoteCommandCenter.shared()
        commandCenter.playCommand.isEnabled = false
        commandCenter.pauseCommand.isEnabled = false
        commandCenter.togglePlayPauseCommand.isEnabled = false
        commandCenter.nextTrackCommand.isEnabled = false
        commandCenter.previousTrackCommand.isEnabled = false
        commandCenter.changePlaybackPositionCommand.isEnabled = false
        commandCenter.skipForwardCommand.isEnabled = false
        commandCenter.skipBackwardCommand.isEnabled = false
        commandCenter.likeCommand.isEnabled = false
        commandCenter.changeShuffleModeCommand.isEnabled = false
        commandCenter.changeRepeatModeCommand.isEnabled = false
        commandCenter.playCommand.removeTarget(nil)
        commandCenter.pauseCommand.removeTarget(nil)
        commandCenter.togglePlayPauseCommand.removeTarget(nil)
        commandCenter.nextTrackCommand.removeTarget(nil)
        commandCenter.previousTrackCommand.removeTarget(nil)
        commandCenter.changePlaybackPositionCommand.removeTarget(nil)
        commandCenter.skipForwardCommand.removeTarget(nil)
        commandCenter.skipBackwardCommand.removeTarget(nil)
        commandCenter.likeCommand.removeTarget(nil)
        commandCenter.changeShuffleModeCommand.removeTarget(nil)
        commandCenter.changeRepeatModeCommand.removeTarget(nil)
        MPNowPlayingInfoCenter.default().nowPlayingInfo = nil
        nowPlayingInfo = [:]
        isMediaSessionActive = false
        isPlaying = false
        currentPosition = 0
        currentDuration = 0
    }
    
    private func setupRemoteCommandCenter() {
        let commandCenter = MPRemoteCommandCenter.shared()
        
        // Play
        commandCenter.playCommand.isEnabled = true
        commandCenter.playCommand.addTarget { [weak self] _ in
            self?.isPlaying = true
            self?.nowPlayingInfo[MPNowPlayingInfoPropertyPlaybackRate] = NSNumber(value: self?.currentPlaybackRate ?? 1.0)
            MPNowPlayingInfoCenter.default().nowPlayingInfo = self?.nowPlayingInfo
            self?.plugin?.notifyListeners("mediaSessionAction", data: ["action": "play"])
            self?.nativeActionListener?("play", nil)
            return .success
        }
        
        // Pause
        commandCenter.pauseCommand.isEnabled = true
        commandCenter.pauseCommand.addTarget { [weak self] _ in
            self?.isPlaying = false
            self?.nowPlayingInfo[MPNowPlayingInfoPropertyPlaybackRate] = NSNumber(value: 0.0)
            MPNowPlayingInfoCenter.default().nowPlayingInfo = self?.nowPlayingInfo
            self?.plugin?.notifyListeners("mediaSessionAction", data: ["action": "pause"])
            self?.nativeActionListener?("pause", nil)
            return .success
        }
        
        // Toggle Play/Pause (headphone button)
        commandCenter.togglePlayPauseCommand.isEnabled = true
        commandCenter.togglePlayPauseCommand.addTarget { [weak self] _ in
            let action = (self?.isPlaying == true) ? "pause" : "play"
            self?.isPlaying = !(self?.isPlaying ?? false)
            let rate: Float = (self?.isPlaying == true) ? (self?.currentPlaybackRate ?? 1.0) : 0.0
            self?.nowPlayingInfo[MPNowPlayingInfoPropertyPlaybackRate] = NSNumber(value: rate)
            MPNowPlayingInfoCenter.default().nowPlayingInfo = self?.nowPlayingInfo
            self?.plugin?.notifyListeners("mediaSessionAction", data: ["action": action])
            self?.nativeActionListener?(action, nil)
            return .success
        }
        
        // Next Track
        commandCenter.nextTrackCommand.isEnabled = true
        commandCenter.nextTrackCommand.addTarget { [weak self] _ in
            self?.plugin?.notifyListeners("mediaSessionAction", data: ["action": "nexttrack"])
            self?.nativeActionListener?("nexttrack", nil)
            return .success
        }
        
        // Previous Track
        commandCenter.previousTrackCommand.isEnabled = true
        commandCenter.previousTrackCommand.addTarget { [weak self] _ in
            self?.plugin?.notifyListeners("mediaSessionAction", data: ["action": "previoustrack"])
            self?.nativeActionListener?("previoustrack", nil)
            return .success
        }
        
        // Seek (scrubbing on lock screen)
        commandCenter.changePlaybackPositionCommand.isEnabled = true
        commandCenter.changePlaybackPositionCommand.addTarget { [weak self] event in
            if let posEvent = event as? MPChangePlaybackPositionCommandEvent {
                self?.currentPosition = posEvent.positionTime
                self?.nowPlayingInfo[MPNowPlayingInfoPropertyElapsedPlaybackTime] = posEvent.positionTime
                MPNowPlayingInfoCenter.default().nowPlayingInfo = self?.nowPlayingInfo
                self?.plugin?.notifyListeners("mediaSessionAction", data: [
                    "action": "seekto",
                    "extras": ["seekTime": posEvent.positionTime]
                ])
                self?.nativeActionListener?("seekto", ["seekTime": posEvent.positionTime])
            }
            return .success
        }
        
        // Skip Forward (default 15s)
        commandCenter.skipForwardCommand.isEnabled = true
        commandCenter.skipForwardCommand.preferredIntervals = [15]
        commandCenter.skipForwardCommand.addTarget { [weak self] _ in
            self?.plugin?.notifyListeners("mediaSessionAction", data: ["action": "seekforward"])
            self?.nativeActionListener?("seekforward", nil)
            return .success
        }
        
        // Skip Backward (default 15s)
        commandCenter.skipBackwardCommand.isEnabled = true
        commandCenter.skipBackwardCommand.preferredIntervals = [15]
        commandCenter.skipBackwardCommand.addTarget { [weak self] _ in
            self?.plugin?.notifyListeners("mediaSessionAction", data: ["action": "seekbackward"])
            self?.nativeActionListener?("seekbackward", nil)
            return .success
        }
    }
    
    private func configureCustomButtons(_ buttons: [[String: Any]]) {
        let commandCenter = MPRemoteCommandCenter.shared()
        
        for btn in buttons {
            guard let action = btn["action"] as? String else { continue }
            let enabled = btn["enabled"] as? Bool ?? true
            
            switch action {
            case "like":
                commandCenter.likeCommand.isEnabled = enabled
                if enabled {
                    commandCenter.likeCommand.removeTarget(nil)
                    commandCenter.likeCommand.localizedTitle = (btn["displayName"] as? String) ?? "Like"
                    commandCenter.likeCommand.addTarget { [weak self] _ in
                        self?.plugin?.notifyListeners("mediaSessionAction", data: ["action": "like"])
                        self?.nativeActionListener?("like", nil)
                        return .success
                    }
                }
            case "shuffle":
                commandCenter.changeShuffleModeCommand.isEnabled = enabled
                if enabled {
                    commandCenter.changeShuffleModeCommand.removeTarget(nil)
                    commandCenter.changeShuffleModeCommand.addTarget { [weak self] _ in
                        self?.plugin?.notifyListeners("mediaSessionAction", data: ["action": "shuffle"])
                        self?.nativeActionListener?("shuffle", nil)
                        return .success
                    }
                }
            case "repeat":
                commandCenter.changeRepeatModeCommand.isEnabled = enabled
                if enabled {
                    commandCenter.changeRepeatModeCommand.removeTarget(nil)
                    commandCenter.changeRepeatModeCommand.addTarget { [weak self] _ in
                        self?.plugin?.notifyListeners("mediaSessionAction", data: ["action": "repeat"])
                        self?.nativeActionListener?("repeat", nil)
                        return .success
                    }
                }
            default:
                // Custom actions not directly supported on iOS lock screen,
                // but we log it for debugging purposes
                print("CapacitorMediaStore: Custom action '\(action)' not natively supported on iOS lock screen")
            }
        }
    }
    
    private func loadArtwork(from urlString: String) {
        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            guard let self = self else { return }
            
            var image: UIImage? = nil
            
            if urlString.hasPrefix("data:") {
                // Base64 data URI
                if let commaIndex = urlString.firstIndex(of: ",") {
                    let base64String = String(urlString[urlString.index(after: commaIndex)...])
                    if let data = Data(base64Encoded: base64String) {
                        image = UIImage(data: data)
                    }
                }
            } else if urlString.hasPrefix("file://") || urlString.hasPrefix("/") {
                // Local file path
                let filePath = urlString.hasPrefix("file://") ? String(urlString.dropFirst(7)) : urlString
                // Try direct image decode first
                image = UIImage(contentsOfFile: filePath)
                // If nil, try extracting embedded album art (for audio files)
                if image == nil {
                    image = self.extractEmbeddedArt(from: filePath)
                }
            } else if let url = URL(string: urlString) {
                // HTTP/HTTPS URL
                if let data = try? Data(contentsOf: url) {
                    image = UIImage(data: data)
                }
            }
            
            if let image = image {
                DispatchQueue.main.async {
                    let artwork = MPMediaItemArtwork(boundsSize: image.size) { _ in image }
                    self.nowPlayingInfo[MPMediaItemPropertyArtwork] = artwork
                    MPNowPlayingInfoCenter.default().nowPlayingInfo = self.nowPlayingInfo
                }
            }
        }
    }
    
    /**
     * Extract embedded album art from an audio file using AVAsset.
     * Works for MP3 (ID3), M4A, FLAC, etc.
     */
    private func extractEmbeddedArt(from filePath: String) -> UIImage? {
        let url = URL(fileURLWithPath: filePath)
        let asset = AVAsset(url: url)
        for metadata in asset.commonMetadata {
            if metadata.commonKey == .commonKeyArtwork,
               let data = metadata.dataValue {
                return UIImage(data: data)
            }
        }
        return nil
    }
    
    deinit {
        if isWatching {
            PHPhotoLibrary.shared().unregisterChangeObserver(self)
        }
    }
}