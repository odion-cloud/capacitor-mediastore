# Changelog

All notable changes to this project will be documented in this file.

## [Latest] - 2024-12-19

### 🐛 Fixed
- **Android 12 and below**: Fixed `getMediasByType()` with `mediaType: 'audio'` not returning audio files
  - Improved MIME type filtering for audio files
  - Added additional selection criteria for better audio file detection
  - Reduced minimum duration threshold from 5000ms to 1000ms to catch more audio files
  - Fixed MediaStore query logic for Files content URI on older Android versions

### ⚠️ Breaking Changes
- **Android 13+ Document Access**: `getMediasByType()` with `mediaType: 'document'` now returns an error/guidance message instead of attempting to use MediaStore
  - Documents on Android 13+ now require Storage Access Framework (SAF)
  - Added proper guidance and error messages for SAF implementation

### ✨ New Features
- **New Method**: `shouldUseSAFForDocuments()` - Check if Storage Access Framework should be used for documents
- **New Method**: `createDocumentPickerIntent()` - Get guidance for implementing SAF document picker
- **New Method**: `getDocumentMetadataFromSAF()` - Extract metadata from SAF document URIs
- **Enhanced Error Messages**: Clearer guidance when document access requires SAF on Android 13+

### 📚 Documentation
- Added comprehensive migration guide (`MIGRATION_GUIDE.md`)
- Updated README with Android version compatibility matrix
- Added examples for both Android 12 and Android 13+ document handling
- Added troubleshooting guide for common issues

### 🔧 Technical Changes
- Improved MediaStore selection logic for Android 12 and below
- Added proper Android 13+ detection and SAF routing
- Enhanced TypeScript definitions with new SAF interfaces
- Added web platform stubs for new methods

### 📱 Platform Support
- **Android 6-12 (API 23-32)**: Full MediaStore support including documents ✅
- **Android 13+ (API 33+)**: MediaStore for images/audio/video, SAF required for documents ⚠️
- **Web**: Not supported (shows appropriate warnings) ⚠️

### 🧪 Testing
- Verified audio file retrieval on Android 9 (API 28)
- Verified document SAF guidance on Android 14 (API 34)
- Tested permission handling across all supported Android versions

## Migration Summary

### For Existing Users:

1. **Audio Files (No Action Required)**:
   ```typescript
   // This now works correctly on Android 12 and below
   const audioFiles = await CapacitorMediaStore.getMediasByType({
     mediaType: 'audio'
   });
   ```

2. **Documents (Action Required for Android 13+)**:
   ```typescript
   // Check if SAF is needed
   const safStatus = await CapacitorMediaStore.shouldUseSAFForDocuments();
   
   if (safStatus.shouldUseSAF) {
     // Implement Storage Access Framework for Android 13+
     // See MIGRATION_GUIDE.md for details
   } else {
     // Use MediaStore for Android 12 and below (works as before)
     const documents = await CapacitorMediaStore.getMediasByType({
       mediaType: 'document'
     });
   }
   ```

### References
- [Android MediaStore Documentation](https://developer.android.com/training/data-storage/shared/media)
- [Android 13 Behavior Changes](https://developer.android.com/about/versions/13/behavior-changes-13)
- [Storage Access Framework Guide](https://developer.android.com/training/data-storage/shared/documents-files)

---

For detailed migration instructions, see [MIGRATION_GUIDE.md](MIGRATION_GUIDE.md).

## [1.0.0] - 2025-06-23

### Added
- Complete Capacitor plugin for Android MediaStore API access
- Support for Android API 21-35 (Android 5.0 - Android 15) with adaptive permissions
- Full TypeScript definitions with comprehensive interfaces
- JavaScript compatibility (ES6, CommonJS, Vanilla JS)
- SD card and external storage access across all Android versions
- Rich audio metadata extraction (artist, album, duration, bitrate, etc.)
- Album information retrieval with track counts
- Media file querying with filtering and sorting options
- Save media functionality with proper storage handling
- Android 15 (API 35) latest MediaStore enhancements
- Android 14 (API 34) visual media permissions support
- Granular permission handling for Android 13+ (READ_MEDIA_*)
- Multi-volume external storage support for Android 10+
- Scoped storage support for Android 10+
- Legacy storage compatibility for Android 5-9
- Enhanced SD card detection and access
- Improved media filtering (duration, size thresholds)
- Complete documentation with usage examples
- Vue.js integration examples
- React integration examples
- Vanilla JavaScript examples

### Features
- `getMedias()` - Get all media files from device storage
- `getMediasByType()` - Get media files filtered by type (audio, video, image, document)
- `getAlbums()` - Get all music albums with metadata
- `saveMedia()` - Save media files to device storage
- `getMediaMetadata()` - Get detailed metadata for specific files
- `checkPermissions()` - Check current permission status
- `requestPermissions()` - Request necessary media permissions

### Android Version Support
- Android 14 (API 34) - Full support with granular permissions
- Android 13 (API 33) - Full support with READ_MEDIA_* permissions
- Android 12 (API 31-32) - Full support with scoped storage
- Android 11 (API 30) - Full support with scoped storage
- Android 10 (API 29) - Full support with scoped storage introduction
- Android 9 (API 28) - Full support with traditional storage
- Android 8 (API 26-27) - Full support with runtime permissions
- Android 7 (API 24-25) - Full support with runtime permissions
- Android 6 (API 23) - Full support with runtime permissions
- Android 5 (API 21-22) - Basic support with install-time permissions

### Capacitor Compatibility
- Capacitor 7.x - Recommended (latest features, best performance)
- Capacitor 6.x - Fully supported (excellent compatibility)
- Capacitor 5.x - Fully supported
- Capacitor 4.x - Supported with minor API differences
- Capacitor 3.x - Limited support (upgrade recommended)

### Platform Support
- Android: Full implementation
- iOS: Placeholder implementation (not supported)
- Web: Not supported (browser limitations)