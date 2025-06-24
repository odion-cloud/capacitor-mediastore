# Changelog

All notable changes to this project will be documented in this file.

## [1.0.10] - 2025-01-27

### Fixed
- **Android 12 and below**: Fixed audio files not being returned by `getMediasByType({ mediaType: 'audio' })`
  - Root cause: Audio-specific columns (ALBUM, ARTIST) were being used with Files content URI where they're not available
  - Solution: Removed audio-specific column filtering when querying Files content URI for Android ≤ 12
  - Result: Audio files now correctly returned by `getMediasByType()` on Android 12 and below
- **Android 13 and above**: Implemented limited document access via alternative Files API approach
  - Root cause: MediaStore doesn't provide document access on Android 13+ due to scoped storage
  - Solution: Added `queryDocumentsWithSAF()` method using Files API with document mime type filtering
  - Result: `getMediasByType({ mediaType: 'document' })` now returns documents on Android 13+ (limited but functional)

### Changed
- **Audio column filtering**: Updated to only use audio-specific columns when appropriate
  - Android 13+: Uses audio columns when querying Audio content URI directly
  - Android 12-: Avoids audio columns when querying Files content URI for specific audio type
  - "All" media type: Can use audio columns for filtering on all Android versions
- **Document access strategy**: 
  - Android 12 and below: Uses MediaStore Files API (unchanged, full access)
  - Android 13 and above: Uses alternative Files API with mime type filtering (limited access)

### Added
- New `queryDocumentsWithSAF()` method for Android 13+ document access
- Enhanced column compatibility checking in `buildSelectionArgs()`
- Comprehensive logging for document query operations
- Improved error handling for document access across Android versions

### Technical Details
- Fixed `buildSelection()` method to handle audio columns correctly per Android version
- Updated `buildSelectionArgs()` method signature to include mediaType parameter
- Added proper mime type filtering for document discovery on Android 13+
- Enhanced debugging logs to track document query success/failure

### Documentation
- Updated README with corrected Android version behavior
- Fixed media type support matrix to reflect current working state
- Updated inline code comments with technical explanations

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