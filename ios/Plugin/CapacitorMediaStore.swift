import Foundation
import Photos
import Capacitor

@objc public class CapacitorMediaStore: NSObject {
    
    @objc public func getMedias(_ call: CAPPluginCall) {
        // Check permissions first
        guard PHPhotoLibrary.authorizationStatus() == .authorized else {
            call.reject("Permission denied. Please grant photo library access.")
            return
        }
        
        // Implementation for iOS media access
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
            // Already granted
            let result: [String: Any] = [
                "readPhotoLibrary": "granted"
            ]
            call.resolve(result)
            return
        }
        
        // Request permission
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
}