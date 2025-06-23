# @capacitor/mediastore

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
npm install @capacitor/mediastore
npx cap sync
```

## Android Setup

Add the following permissions to your `android/app/src/main/AndroidManifest.xml`:

```xml
<!-- Required for accessing media files -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" 
                 android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" 
                 android:maxSdkVersion="29" />

<!-- Granular media permissions for Android 13+ -->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
```

## Usage

```typescript
import { CapacitorMediaStore } from '@capacitor/mediastore';

// Request permissions first
const permissions = await CapacitorMediaStore.requestPermissions();
console.log('Permissions:', permissions);

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
import { CapacitorMediaStore } from '@capacitor/mediastore';

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

## Platform Support

- ✅ **Android**: Full implementation using MediaStore API
- ❌ **iOS**: Not supported (iOS uses different media access patterns)
- ❌ **Web**: Not supported (browser security restrictions)

## License

MIT
