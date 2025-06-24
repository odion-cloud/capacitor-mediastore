# @odion-cloud/capacitor-mediastore

A Capacitor plugin that provides comprehensive access to Android MediaStore API for media file access and metadata retrieval. This plugin is specifically designed to overcome the limitations of Capacitor's filesystem API, particularly for accessing SD card storage and retrieving rich media metadata.

## Features

- ✅ Access all media types (audio, video, images, documents)
- ✅ Scan both internal storage and SD card/external storage
- ✅ Retrieve comprehensive metadata for audio files (title, artist, album, duration, etc.)
- ✅ Query media files with filtering and sorting options
- ✅ Save media files to device storage
- ✅ Get album information
- ✅ Proper Android permissions handling
- ✅ TypeScript definitions included

## Installation

```bash
npm install @odion-cloud/capacitor-mediastore
npx cap sync
```

## Android Setup

### 1. Add Permissions to AndroidManifest.xml

Add the following permissions to your `android/app/src/main/AndroidManifest.xml`:

```xml
<!-- Legacy permissions for Android 6-12 -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" 
                 android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" 
                 android:maxSdkVersion="29" />

<!-- Granular media permissions for Android 13+ (API 33+) -->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />

<!-- Visual media permissions for Android 14+ (API 34+) -->
<uses-permission android:name="android.permission.READ_MEDIA_VISUAL_USER_SELECTED" />
```

### 2. Permission Behavior by Android Version

- **Android 6-12 (API 23-32)**: Uses `READ_EXTERNAL_STORAGE` for all media access
- **Android 13+ (API 33+)**: Uses granular permissions (`READ_MEDIA_IMAGES`, `READ_MEDIA_AUDIO`, `READ_MEDIA_VIDEO`)
- **Android 14+ (API 34+)**: Adds `READ_MEDIA_VISUAL_USER_SELECTED` for selective media access

## iOS Setup

### 1. Add Usage Description to Info.plist

Add the following to your `ios/App/App/Info.plist`:

```xml
<key>NSPhotoLibraryUsageDescription</key>
<string>This app needs access to your photo library to manage media files</string>
<key>NSPhotoLibraryAddUsageDescription</key>
<string>This app needs access to save media files to your photo library</string>
```

### 2. iOS Permission Behavior

- iOS uses the Photos framework for media access
- Permission is requested automatically when calling media methods
- Limited access (iOS 14+) is supported and treated as granted

## Usage

### Basic Setup

```typescript
import { CapacitorMediaStore } from '@odion-cloud/capacitor-mediastore';

// Check current permission status
const permissions = await CapacitorMediaStore.checkPermissions();
console.log('Current permissions:', permissions);

// Request all permissions
const requestResult = await CapacitorMediaStore.requestPermissions();
console.log('Permission request result:', requestResult);

// Request specific permission types
const specificPermissions = await CapacitorMediaStore.requestPermissions({
  types: ['audio', 'images'] // Only request audio and image permissions
});
console.log('Specific permissions:', specificPermissions);
```

### Permission Types

Available permission types for `requestPermissions()`:
- `'images'` - Access to image files
- `'audio'` - Access to audio files  
- `'video'` - Access to video files

If no types are specified, all available permissions will be requested.

### Android Permission Examples

```typescript
// For music/audio apps - request only audio permission
await CapacitorMediaStore.requestPermissions({ types: ['audio'] });

// For photo/gallery apps - request images and videos
await CapacitorMediaStore.requestPermissions({ types: ['images', 'video'] });

// For full media access - request all permissions
await CapacitorMediaStore.requestPermissions();
```

### Permission Status Values

The plugin returns these permission states:
- `'granted'` - Permission is granted and you can access media
- `'denied'` - Permission was denied by the user
- `'prompt'` - Permission needs to be requested (first time)
- `'prompt-with-rationale'` - User denied before, show rationale

### Platform Differences

**Android Behavior:**
- Shows native Android permission dialog
- Handles different permission types based on Android version
- Android 13+ shows separate dialogs for images, audio, video
- Android 6-12 shows single storage permission dialog

**iOS Behavior:**
- Shows native iOS photo library permission dialog
- Single permission covers all media types
- Limited access (iOS 14+) is treated as granted

### Troubleshooting Permissions

If permission dialogs don't appear:

1. **Check AndroidManifest.xml** - Ensure all required permissions are declared
2. **Verify iOS Info.plist** - Ensure usage descriptions are present  
3. **Test on real device** - Permission dialogs don't show in some simulators
4. **Check app settings** - User may have denied permissions permanently

```typescript
// Debug permission issues
const currentPermissions = await CapacitorMediaStore.checkPermissions();
console.log('Current permission state:', currentPermissions);

// If permissions are denied, you may need to guide users to app settings
if (currentPermissions.readMediaAudio === 'denied') {
  // Show instructions to manually enable in device settings
}
```

// Get all media files (including from SD card)
const allMedia = await CapacitorMediaStore.getMedias({
  limit: 50,
  offset: 0,
  sortOrder: 'DESC',
  sortBy: 'DATE_ADDED',
  includeExternal: true // This enables SD card access
});

console.log('Total media files:', allMedia.totalCount);
console.log('Media files:', allMedia.media);

// Get only audio files with metadata
const audioFiles = await CapacitorMediaStore.getMediasByType({
  mediaType: 'audio',
  limit: 100,
  includeExternal: true
});

// Each audio file includes rich metadata
audioFiles.media.forEach(file => {
  console.log(`${file.title} by ${file.artist} (${file.album})`);
  console.log(`Duration: ${file.duration}ms, Bitrate: ${file.bitrate}`);
});

// Get detailed metadata for a specific file
const metadata = await CapacitorMediaStore.getMediaMetadata({
  filePath: 'content://media/external/audio/media/123'
});

// Get all music albums
const albums = await CapacitorMediaStore.getAlbums();
console.log('Albums found:', albums.albums);

// Save a media file
const saveResult = await CapacitorMediaStore.saveMedia({
  data: 'base64-encoded-data-here',
  fileName: 'my-song.mp3',
  mediaType: 'audio',
  albumName: 'My Album'
});
```

## API

### `getMedias(options?)`

Get all media files from device storage.

**Parameters:**
- `options` (optional): MediaQueryOptions

```typescript
interface MediaQueryOptions {
  limit?: number;           // Limit number of results
  offset?: number;          // Offset for pagination (default: 0)
  sortOrder?: 'ASC' | 'DESC'; // Sort order (default: 'DESC')
  sortBy?: 'DATE_ADDED' | 'DATE_MODIFIED' | 'DISPLAY_NAME' | 'SIZE' | 'TITLE';
  albumName?: string;       // Filter by album name
  artistName?: string;      // Filter by artist name
  includeExternal?: boolean; // Include SD card files (default: true)
}
```

**Returns:** `Promise<MediaResponse>`

### `getMediasByType(options)`

Get media files filtered by type.

**Parameters:**
- `options`: MediaTypeOptions (extends MediaQueryOptions)
  - `mediaType`: 'image' | 'audio' | 'video' | 'document' | 'all'

**Returns:** `Promise<MediaResponse>`

### `getAlbums()`

Get all music albums from the device.

**Returns:** `Promise<AlbumResponse>`

### `saveMedia(options)`

Save a media file to device storage.

**Parameters:**
- `options`: SaveMediaOptions

```typescript
interface SaveMediaOptions {
  data: string;           // Base64 encoded data or file URI
  fileName: string;       // File name
  mediaType: 'image' | 'audio' | 'video' | 'document';
  albumName?: string;     // Album name (optional)
  relativePath?: string;  // Relative path within media directory
}
```

**Returns:** `Promise<SaveMediaResponse>`

### `getMediaMetadata(options)`

Get detailed metadata for a specific media file.

**Parameters:**
- `options`: MediaMetadataOptions
  - `filePath`: string (URI of the media file)

**Returns:** `Promise<MediaMetadataResponse>`

### `checkPermissions()`

Check current permission status.

**Returns:** `Promise<PermissionStatus>`

### `requestPermissions()`

Request necessary permissions for media access.

**Returns:** `Promise<PermissionStatus>`

## Data Types

### MediaFile

```typescript
interface MediaFile {
  id: string;
  uri: string;
  displayName: string;
  size: number;
  mimeType: string;
  dateAdded: number;
  dateModified: number;
  mediaType: 'image' | 'audio' | 'video' | 'document';
  
  // For images/videos
  width?: number;
  height?: number;
  
  // For audio/video
  duration?: number;
  
  // Rich audio metadata
  title?: string;
  artist?: string;
  album?: string;
  albumArtist?: string;
  composer?: string;
  genre?: string;
  track?: number;
  year?: number;
  bitrate?: number;
  sampleRate?: number;
  channels?: number;
  albumArtUri?: string;
  
  // Storage location
  isExternal?: boolean; // true if file is on SD card
}
```

## Why Use This Plugin?

### Problem with Capacitor Filesystem

The standard Capacitor Filesystem API has several limitations when working with media files:

1. **No SD Card Access**: Cannot reliably access external storage/SD cards
2. **Limited Metadata**: Only provides basic file information
3. **No Media Organization**: Cannot access system media database
4. **Performance Issues**: Scanning large directories is slow

### MediaStore Advantages

This plugin uses Android's MediaStore API which provides:

1. **Full Storage Access**: Includes both internal storage and SD cards
2. **Rich Metadata**: Complete audio metadata (artist, album, duration, bitrate, etc.)
3. **System Integration**: Uses Android's built-in media database
4. **Performance**: Fast queries with built-in indexing
5. **Proper Permissions**: Handles Android 13+ granular media permissions

## Example: Building a Music Player

```typescript
import { CapacitorMediaStore } from '@odion-cloud/capacitor-mediastore';

class MusicService {
  async loadAllSongs() {
    // Request permissions
    await CapacitorMediaStore.requestPermissions();
    
    // Get all audio files including from SD card
    const result = await CapacitorMediaStore.getMediasByType({
      mediaType: 'audio',
      sortBy: 'TITLE',
      sortOrder: 'ASC',
      includeExternal: true // Critical for SD card access
    });
    
    return result.media.map(file => ({
      id: file.id,
      title: file.title || file.displayName,
      artist: file.artist || 'Unknown Artist',
      album: file.album || 'Unknown Album',
      duration: file.duration || 0,
      uri: file.uri,
      isOnSDCard: file.isExternal
    }));
  }
  
  async getAlbums() {
    const result = await CapacitorMediaStore.getAlbums();
    return result.albums;
  }
  
  async getDetailedMetadata(fileUri: string) {
    return await CapacitorMediaStore.getMediaMetadata({
      filePath: fileUri
    });
  }
}
```

## Android Version Behavior Differences

### Document File Access

Due to Android's evolving storage security model, document file access behavior differs by Android version:

**Android 12 and below (API ≤ 32):**
- ✅ `getMedias()` returns audio, video, image, **and document files**
- ✅ `getMediasByType({ mediaType: 'document' })` **works correctly**
- ✅ Documents accessible via MediaStore Files API

**Android 13 and above (API ≥ 33):**
- ⚠️ `getMedias()` returns audio, video, and image files **only (documents excluded)**
- ✅ `getMediasByType({ mediaType: 'document' })` **works with limited access**
- ⚙️ Documents use alternative Files API approach (limited but functional)

### Audio File Access

**Android 12 and below (API ≤ 32):**
- ✅ All audio files accessible, including short clips and ringtones
- ✅ No duration restrictions applied

**Android 13 and above (API ≥ 33):**
- ✅ All audio files accessible with `READ_MEDIA_AUDIO` permission
- ✅ Better performance with dedicated audio content URI

### Media Type Support Matrix

| Media Type | Android ≤ 12 | Android ≥ 13 | Notes |
|------------|--------------|--------------|-------|
| **Audio** | ✅ Full Support | ✅ Enhanced Support | Fixed: Now works correctly on all versions |
| **Video** | ✅ Full Support | ✅ Enhanced Support | Better performance on Android 13+ |
| **Image** | ✅ Full Support | ✅ Enhanced Support | Better performance on Android 13+ |
| **Document** | ✅ MediaStore Access | ⚠️ Limited Support | Uses alternative Files API on Android 13+ |

### Recommended Document Handling

For cross-version document access, consider these alternatives:

```typescript
// ✅ Recommended: Version-specific handling
import { CapacitorMediaStore } from '@odion-cloud/capacitor-mediastore';
import { Device } from '@capacitor/device';

async function getDocuments() {
  const info = await Device.getInfo();
  const androidVersion = parseInt(info.osVersion);
  
  if (androidVersion >= 13) {
    // Use Storage Access Framework or document picker
    // Documents not supported via MediaStore on Android 13+
    console.log('Use document picker for Android 13+');
    return [];
  } else {
    // Use MediaStore for Android 12 and below
    const result = await CapacitorMediaStore.getMediasByType({
      mediaType: 'document'
    });
    return result.media;
  }
}
```

## Android Version Support

This plugin supports a wide range of Android versions with adaptive permission handling:

| Android Version | API Level | Support Status | Features |
|----------------|-----------|----------------|----------|
| **Android 15** | API 35 | ✅ Full Support | Latest MediaStore enhancements, optimal performance |
| **Android 14** | API 34 | ✅ Full Support | Visual media permissions, partial media access |
| **Android 13** | API 33 | ✅ Full Support | Granular media permissions (READ_MEDIA_*) |
| **Android 12** | API 31-32 | ✅ Full Support | Scoped storage with READ_EXTERNAL_STORAGE |
| **Android 11** | API 30 | ✅ Full Support | Scoped storage with READ_EXTERNAL_STORAGE |
| **Android 10** | API 29 | ✅ Full Support | Scoped storage introduction, multi-volume support |
| **Android 9** | API 28 | ✅ Full Support | Traditional storage with runtime permissions |
| **Android 8** | API 26-27 | ✅ Full Support | Traditional storage with runtime permissions |
| **Android 7** | API 24-25 | ✅ Full Support | Traditional storage with runtime permissions |
| **Android 6** | API 23 | ✅ Full Support | Runtime permissions introduced |
| **Android 5** | API 21-22 | ✅ Basic Support | Install-time permissions, limited SD card access |

### SD Card Access by Version

- **Android 5-9**: Full external storage access with proper permissions
- **Android 10+**: Scoped storage with MediaStore API, multi-volume support
- **Android 13+**: Granular media permissions for enhanced privacy
- **Android 14+**: Visual media permissions with user-selected access

## Capacitor Version Support

| Capacitor Version | Support Status | Notes |
|-------------------|----------------|-------|
| **Capacitor 7.x** | ✅ Recommended | Latest features, best performance, full compatibility |
| **Capacitor 6.x** | ✅ Fully Supported | Excellent compatibility |
| **Capacitor 5.x** | ✅ Supported | Fully compatible |
| **Capacitor 4.x** | ✅ Supported | Compatible with minor API differences |
| **Capacitor 3.x** | ⚠️ Limited | Basic functionality, upgrade recommended |

## JavaScript Compatibility

This plugin works with:
- ✅ **TypeScript** - Full type definitions included
- ✅ **JavaScript (ES6+)** - Modern JavaScript with import/export
- ✅ **CommonJS** - Node.js style require()
- ✅ **Vanilla JavaScript** - No framework required
- ✅ **React/Vue/Angular** - All major frameworks supported

### JavaScript Usage Example

```javascript
// ES6 Import
import { CapacitorMediaStore } from '@odion-cloud/capacitor-mediastore';

// CommonJS Require
const { CapacitorMediaStore } = require('@odion-cloud/capacitor-mediastore');

// Get all songs including SD card
const songs = await CapacitorMediaStore.getMediasByType({
  mediaType: 'audio',
  includeExternal: true // Enables SD card access
});

console.log(`Found ${songs.totalCount} songs`);
songs.media.forEach(song => {
  console.log(`${song.title} - ${song.artist} ${song.isExternal ? '(SD)' : ''}`);
});
```

## Platform Support

- ✅ **Android**: Full implementation using MediaStore API (API 21+)
- ❌ **iOS**: Not supported (iOS uses different media access patterns)
- ❌ **Web**: Not supported (browser security restrictions)

## Migration from Capacitor Filesystem

If you're currently using Capacitor Filesystem for media access, here's why you should migrate:

### Filesystem Limitations
```javascript
// ❌ Capacitor Filesystem - Limited access
import { Filesystem } from '@capacitor/filesystem';

// Cannot access SD card reliably
// No metadata (artist, album, duration)
// Slow directory scanning
// No album organization
const files = await Filesystem.readdir({
  path: 'Music',
  directory: Directory.ExternalStorage
});
```

### MediaStore Advantages
```javascript
// ✅ MediaStore Plugin - Full access
import { CapacitorMediaStore } from '@odion-cloud/capacitor-mediastore';

// Accesses both internal and SD card storage
// Rich metadata included
// Fast indexed queries
// Album organization
const songs = await CapacitorMediaStore.getMediasByType({
  mediaType: 'audio',
  includeExternal: true,
  sortBy: 'TITLE'
});
```

## Installation Requirements

### Minimum Requirements
- **Android**: API 21 (Android 5.0) or higher
- **Capacitor**: 3.0 or higher (6.0 recommended)
- **Node.js**: 14.0 or higher
- **NPM**: 6.0 or higher

### Recommended Setup
- **Android**: API 29+ for best SD card compatibility
- **Capacitor**: 6.0 for latest features
- **Target SDK**: 34 (Android 14)

## License

MIT
