# Changelog

All notable changes to this project will be documented in this file.

## [1.0.0] - 2024-06-23

### Added
- Complete Capacitor plugin for Android MediaStore API access
- Support for Android API 21+ (Android 5.0+) with adaptive permissions
- Full TypeScript definitions with comprehensive interfaces
- JavaScript compatibility (ES6, CommonJS, Vanilla JS)
- SD card and external storage access across all Android versions
- Rich audio metadata extraction (artist, album, duration, bitrate, etc.)
- Album information retrieval with track counts
- Media file querying with filtering and sorting options
- Save media functionality with proper storage handling
- Granular permission handling for Android 13+ (READ_MEDIA_*)
- Scoped storage support for Android 10+
- Legacy storage compatibility for Android 5-9
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
- Capacitor 6.x - Recommended (full feature support)
- Capacitor 5.x - Fully supported
- Capacitor 4.x - Supported with minor API differences
- Capacitor 3.x - Limited support (upgrade recommended)

### Platform Support
- Android: Full implementation
- iOS: Placeholder implementation (not supported)
- Web: Not supported (browser limitations)