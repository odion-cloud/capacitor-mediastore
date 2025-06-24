# Capacitor MediaStore Plugin

## Project Overview
A comprehensive Capacitor plugin that provides native access to Android MediaStore API and iOS Photos framework for media file access and metadata retrieval. Specifically designed to overcome Capacitor filesystem limitations for SD card access and rich media metadata.

## Recent Changes
- **2024-12-24**: Fixed all Android Kotlin compilation errors and permission system
- **2024-12-24**: Implemented proper native permission dialogs for Android and iOS
- **2024-12-24**: Added granular permission types (images, audio, video)
- **2024-12-24**: Enhanced permission handling for Android 6-14+ with proper API compatibility
- **2024-12-24**: Fixed permission system to use Capacitor aliases correctly
- **2024-12-24**: Rebuilt permission requests with proper native dialog triggering
- **2024-12-24**: Fixed MediaStore API for Android version differences
- **2024-12-24**: Resolved audio/document file access issues for Android 12 and below
- **2024-12-24**: Improved document filtering to exclude media files correctly
- **2024-12-24**: Created comprehensive HTML documentation with tabbed interface
- **2024-12-24**: Updated README with complete setup instructions

## Key Features
- Cross-platform media access (Android MediaStore + iOS Photos)
- SD card and external storage scanning
- Rich metadata extraction for audio files
- Granular permission system with type-specific requests
- Native permission dialogs that actually show to users
- Support for Android 6-14+ with adaptive permission handling

## Architecture
- **Android**: Uses MediaStore API with proper activity context handling
- **iOS**: Uses Photos framework with PHPhotoLibrary authorization
- **Web**: Fallback implementation for development
- **Permission System**: Type-specific requests (images, audio, video) with native dialogs

## User Preferences
- Clean, professional code structure
- Comprehensive documentation with real-world examples
- Platform-specific implementation details
- Error handling with detailed logging