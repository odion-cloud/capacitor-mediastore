import Foundation
import Capacitor
import Photos
import UniformTypeIdentifiers

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(CapacitorMediaStorePlugin)
public class CapacitorMediaStorePlugin: CAPPlugin, UIDocumentPickerDelegate {
    /// The implementation is public so other native plugins can access the Native API
    /// (e.g. `setNativeActionListener`, `setMediaSessionNative`, etc.)
    public let implementation = CapacitorMediaStore()

    /// Pending calls for file picker operations
    private var pendingSaveCall: CAPPluginCall?
    private var pendingOpenCall: CAPPluginCall?
    private var tempSaveURL: URL?

    public override func load() {
        implementation.plugin = self
    }

    // ----------------------------------------------------------------
    // Existing Media Store methods
    // ----------------------------------------------------------------

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

    // ----------------------------------------------------------------
    // File Change Watcher
    // ----------------------------------------------------------------

    @objc func startWatching(_ call: CAPPluginCall) {
        implementation.startWatching(call)
    }

    @objc func stopWatching(_ call: CAPPluginCall) {
        implementation.stopWatching(call)
    }

    // ----------------------------------------------------------------
    // Native Media Session
    // ----------------------------------------------------------------

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

    // ----------------------------------------------------------------
    // File Picker — Save As & Open (UIDocumentPicker)
    // ----------------------------------------------------------------

    @objc func saveFileAs(_ call: CAPPluginCall) {
        guard let fileName = call.getString("fileName") else {
            call.reject("fileName is required")
            return
        }
        guard let base64Data = call.getString("data"),
              let data = Data(base64Encoded: base64Data) else {
            call.reject("data (base64 string) is required")
            return
        }

        let tempDir = FileManager.default.temporaryDirectory
        let tempURL = tempDir.appendingPathComponent(fileName)
        do {
            try data.write(to: tempURL)
        } catch {
            call.reject("Failed to create temp file: \(error.localizedDescription)")
            return
        }

        self.pendingSaveCall = call
        self.tempSaveURL = tempURL

        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            if #available(iOS 14.0, *) {
                let picker = UIDocumentPickerViewController(forExporting: [tempURL], asCopy: true)
                picker.delegate = self
                self.bridge?.viewController?.present(picker, animated: true)
            } else {
                let picker = UIDocumentPickerViewController(urls: [tempURL], in: .exportToService)
                picker.delegate = self
                self.bridge?.viewController?.present(picker, animated: true)
            }
        }
    }

    @objc func openFile(_ call: CAPPluginCall) {
        self.pendingOpenCall = call

        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            if #available(iOS 14.0, *) {
                let types: [UTType] = [.data, .archive, .item]
                let picker = UIDocumentPickerViewController(forOpeningContentTypes: types, asCopy: true)
                picker.delegate = self
                picker.allowsMultipleSelection = false
                self.bridge?.viewController?.present(picker, animated: true)
            } else {
                let picker = UIDocumentPickerViewController(documentTypes: ["public.data"], in: .import)
                picker.delegate = self
                picker.allowsMultipleSelection = false
                self.bridge?.viewController?.present(picker, animated: true)
            }
        }
    }

    // MARK: - UIDocumentPickerDelegate

    public func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
        // Handle "Save As" completion
        if let saveCall = pendingSaveCall {
            if let tempURL = tempSaveURL {
                try? FileManager.default.removeItem(at: tempURL)
            }
            tempSaveURL = nil
            pendingSaveCall = nil

            var result = JSObject()
            result["success"] = true
            if let firstURL = urls.first {
                result["uri"] = firstURL.absoluteString
            }
            saveCall.resolve(result)
            return
        }

        // Handle "Open" completion
        if let openCall = pendingOpenCall {
            pendingOpenCall = nil
            guard let url = urls.first else {
                openCall.reject("No file selected")
                return
            }

            do {
                let data = try Data(contentsOf: url)
                let base64 = data.base64EncodedString()
                let fileName = url.lastPathComponent
                let fileSize = data.count

                var mimeType = "application/octet-stream"
                if #available(iOS 14.0, *) {
                    if let typeId = try? url.resourceValues(forKeys: [.typeIdentifierKey]).typeIdentifier,
                       let utType = UTType(typeId),
                       let mime = utType.preferredMIMEType {
                        mimeType = mime
                    }
                }

                var result = JSObject()
                result["data"] = base64
                result["name"] = fileName
                result["mimeType"] = mimeType
                result["size"] = fileSize
                openCall.resolve(result)
            } catch {
                openCall.reject("Failed to read file: \(error.localizedDescription)")
            }
            return
        }
    }

    public func documentPickerWasCancelled(_ controller: UIDocumentPickerViewController) {
        if let saveCall = pendingSaveCall {
            if let tempURL = tempSaveURL {
                try? FileManager.default.removeItem(at: tempURL)
            }
            tempSaveURL = nil
            pendingSaveCall = nil

            var result = JSObject()
            result["cancelled"] = true
            saveCall.resolve(result)
            return
        }

        if let openCall = pendingOpenCall {
            pendingOpenCall = nil
            var result = JSObject()
            result["cancelled"] = true
            openCall.resolve(result)
            return
        }
    }
}
