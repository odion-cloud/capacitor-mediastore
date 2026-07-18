import Foundation
import Capacitor
import Photos

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(CapacitorMediaStorePlugin)
public class CapacitorMediaStorePlugin: CAPPlugin {
    /// The implementation is public so other native plugins can access the Native API
    /// (e.g. `setNativeActionListener`, `setMediaSessionNative`, etc.)
    public let implementation = CapacitorMediaStore()

    public override func load() {
        implementation.plugin = self
    }

    // ──────────────────────────────────────────────
    // Existing Media Store methods
    // ──────────────────────────────────────────────

    @objc func getMedias(_ call: CAPPluginCall) {
        implementation.getMedias(call)
    }

    @objc func getMediasByType(_ call: CAPPluginCall) {
        implementation.getMediasByType(call)
    }

    @objc func getAlbums(_ call: CAPPluginCall) {
        implementation.getAlbums(call)
    }

    @objc func saveMedia(_ call: CAPPluginCall) {
        implementation.saveMedia(call)
    }

    @objc func getMediaMetadata(_ call: CAPPluginCall) {
        implementation.getMediaMetadata(call)
    }

    @objc func checkPermissions(_ call: CAPPluginCall) {
        implementation.checkPermissions(call)
    }

    @objc func requestPermissions(_ call: CAPPluginCall) {
        implementation.requestPermissions(call)
    }

    // ──────────────────────────────────────────────
    // File Change Watcher
    // ──────────────────────────────────────────────

    @objc func startWatching(_ call: CAPPluginCall) {
        implementation.startWatching(call)
    }

    @objc func stopWatching(_ call: CAPPluginCall) {
        implementation.stopWatching(call)
    }

    // ──────────────────────────────────────────────
    // Native Media Session
    // ──────────────────────────────────────────────

    @objc func setMediaSession(_ call: CAPPluginCall) {
        implementation.setMediaSession(call)
    }

    @objc func updatePlaybackState(_ call: CAPPluginCall) {
        implementation.updatePlaybackState(call)
    }

    @objc func updatePositionState(_ call: CAPPluginCall) {
        implementation.updatePositionState(call)
    }

    @objc func updateMediaSessionButtons(_ call: CAPPluginCall) {
        implementation.updateMediaSessionButtons(call)
    }

    @objc func destroyMediaSession(_ call: CAPPluginCall) {
        implementation.destroyMediaSession(call)
    }
}
