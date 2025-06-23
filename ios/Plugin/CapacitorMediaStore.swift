import Foundation
import Capacitor
import Photos
import AVFoundation

@objc public class CapacitorMediaStore: NSObject {
    
    @objc public func getMedias(_ call: CAPPluginCall) {
        call.reject("iOS implementation not yet available. This plugin is primarily designed for Android MediaStore access.")
    }
    
    @objc public func getMediasByType(_ call: CAPPluginCall) {
        call.reject("iOS implementation not yet available. This plugin is primarily designed for Android MediaStore access.")
    }
    
    @objc public func getAlbums(_ call: CAPPluginCall) {
        call.reject("iOS implementation not yet available. This plugin is primarily designed for Android MediaStore access.")
    }
    
    @objc public func saveMedia(_ call: CAPPluginCall) {
        call.reject("iOS implementation not yet available. This plugin is primarily designed for Android MediaStore access.")
    }
    
    @objc public func getMediaMetadata(_ call: CAPPluginCall) {
        call.reject("iOS implementation not yet available. This plugin is primarily designed for Android MediaStore access.")
    }
    
    @objc public func checkPermissions(_ call: CAPPluginCall) {
        let status = PHPhotoLibrary.authorizationStatus()
        var result: [String: Any] = [:]
        
        switch status {
        case .authorized, .limited:
            result["photos"] = "granted"
        case .denied, .restricted:
            result["photos"] = "denied"
        case .notDetermined:
            result["photos"] = "prompt"
        @unknown default:
            result["photos"] = "denied"
        }
        
        call.resolve(result)
    }
    
    @objc public func requestPermissions(_ call: CAPPluginCall) {
        PHPhotoLibrary.requestAuthorization { status in
            DispatchQueue.main.async {
                self.checkPermissions(call)
            }
        }
    }
}
