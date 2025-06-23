# Capacitor MediaStore Plugin - Package Summary

## Overview
A comprehensive Capacitor plugin that provides full access to Android's MediaStore API, specifically designed to overcome Capacitor Filesystem limitations for music apps and media applications.

## Key Capabilities
- **SD Card Access**: Full external storage scanning across all Android versions
- **Rich Metadata**: Complete audio file information (artist, album, duration, bitrate, genre, etc.)
- **Version Compatibility**: Android 5.0+ (API 21+) with adaptive permission handling
- **JavaScript Support**: Works with TypeScript, ES6, CommonJS, and vanilla JavaScript
- **Performance**: Uses Android's indexed MediaStore database for fast queries

## Package Structure
```
@capacitor/mediastore/
├── dist/                          # Built plugin files
│   ├── esm/                      # ESM modules with TypeScript definitions
│   ├── plugin.js                 # IIFE bundle for browsers
│   └── plugin.cjs.js             # CommonJS bundle
├── android/                       # Android implementation
│   ├── src/main/java/com/capacitor/mediastore/
│   │   ├── MediaStorePlugin.kt   # Main plugin class
│   │   └── MediaStoreHelper.kt   # MediaStore API wrapper
│   └── build.gradle              # Android build configuration
├── ios/                          # iOS placeholder implementation
├── src/                          # TypeScript source
│   ├── definitions.ts            # Interface definitions
│   ├── index.ts                  # Plugin registration
│   └── web.ts                    # Web fallback implementation
├── README.md                     # Comprehensive documentation
├── CHANGELOG.md                  # Version history
├── example-usage.md              # Vue.js integration examples
├── javascript-usage.md           # JavaScript/React examples
└── CapacitorMediaStore.podspec   # iOS CocoaPods specification
```

## Android Version Support Matrix

| Version | API | Status | SD Card | Permissions | Notes |
|---------|-----|--------|---------|-------------|-------|
| Android 15 | 35 | ✅ Full | Yes | Latest MediaStore API | Optimal performance |
| Android 14 | 34 | ✅ Full | Yes | Visual media permissions | Partial access support |
| Android 13 | 33 | ✅ Full | Yes | Granular (READ_MEDIA_*) | Enhanced privacy |
| Android 12 | 31-32 | ✅ Full | Yes | READ_EXTERNAL_STORAGE | Scoped storage |
| Android 11 | 30 | ✅ Full | Yes | READ_EXTERNAL_STORAGE | Scoped storage |
| Android 10 | 29 | ✅ Full | Yes | READ_EXTERNAL_STORAGE | Multi-volume support |
| Android 9 | 28 | ✅ Full | Yes | READ_EXTERNAL_STORAGE | Traditional storage |
| Android 8 | 26-27 | ✅ Full | Yes | Runtime permissions | Traditional storage |
| Android 7 | 24-25 | ✅ Full | Yes | Runtime permissions | Traditional storage |
| Android 6 | 23 | ✅ Full | Yes | Runtime permissions | Permission model intro |
| Android 5 | 21-22 | ✅ Basic | Limited | Install-time | Legacy compatibility |

## API Methods

### Core Methods
- `getMedias(options?)` - Get all media files with optional filtering
- `getMediasByType(options)` - Get media files by type (audio, video, image, document)
- `getAlbums()` - Get all music albums with metadata
- `saveMedia(options)` - Save media files to device storage
- `getMediaMetadata(options)` - Get detailed metadata for specific files

### Permission Methods
- `checkPermissions()` - Check current permission status
- `requestPermissions()` - Request necessary media permissions

## Usage Examples

### TypeScript
```typescript
import { CapacitorMediaStore, MediaFile } from '@capacitor/mediastore';

const songs: MediaFile[] = await CapacitorMediaStore.getMediasByType({
  mediaType: 'audio',
  includeExternal: true,
  sortBy: 'TITLE'
});
```

### JavaScript (ES6)
```javascript
import { CapacitorMediaStore } from '@capacitor/mediastore';

const songs = await CapacitorMediaStore.getMediasByType({
  mediaType: 'audio',
  includeExternal: true
});
```

### JavaScript (CommonJS)
```javascript
const { CapacitorMediaStore } = require('@capacitor/mediastore');

const songs = await CapacitorMediaStore.getMediasByType({
  mediaType: 'audio',
  includeExternal: true
});
```

## Installation

### For New Projects
```bash
npm install @capacitor/mediastore
npx cap sync android
```

### Android Setup
Add to `android/app/src/main/AndroidManifest.xml`:
```xml
<!-- For Android 13+ -->
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />

<!-- For Android 6-12 -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" 
                 android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" 
                 android:maxSdkVersion="29" />
```

## Framework Integration

### Vue.js
- Complete composable provided (`useMusicService`)
- Reactive data binding
- Permission handling
- Error management

### React
- Hook-based implementation
- State management examples
- Component integration

### Vanilla JavaScript
- No framework dependencies
- DOM manipulation examples
- Event handling

## Advantages Over Capacitor Filesystem

| Feature | Filesystem API | MediaStore Plugin |
|---------|----------------|-------------------|
| SD Card Access | ❌ Limited/Unreliable | ✅ Full Access |
| Audio Metadata | ❌ None | ✅ Complete (artist, album, etc.) |
| Performance | ❌ Slow directory scanning | ✅ Fast indexed queries |
| Album Organization | ❌ Manual parsing | ✅ Built-in album support |
| Permission Handling | ❌ Basic | ✅ Adaptive across Android versions |
| External Storage | ❌ Inconsistent | ✅ Reliable across all versions |

## Build and Verification

The plugin includes comprehensive build verification:
- TypeScript compilation
- Rollup bundling
- Android compilation check
- iOS specification validation
- API completeness verification

All verification checks pass successfully, confirming the plugin is ready for production use.

## License
MIT License - Free for commercial and personal use.

## Support
- GitHub Issues for bug reports
- Documentation includes comprehensive examples
- Framework-specific integration guides provided