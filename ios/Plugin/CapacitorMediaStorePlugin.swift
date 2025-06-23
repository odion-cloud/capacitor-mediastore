import Foundation
import Capacitor
import Photos

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(CapacitorMediaStorePlugin)
public class CapacitorMediaStorePlugin: CAPPlugin {
    private let implementation = CapacitorMediaStore()

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
}
