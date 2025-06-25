# Capacitor MediaStore Plugin

A simple and powerful Capacitor plugin that lets you access media files (music, photos, videos) on Android devices, including SD card storage.

## 🚀 Quick Start

```bash
npm install @odion-cloud/capacitor-mediastore
npx cap sync
```

## 📱 What This Plugin Does

- ✅ Get all your music files with details (title, artist, album, duration)
- ✅ Access photos and videos from device storage
- ✅ Read files from both internal storage and SD card
- ✅ Get music albums and playlists
- ✅ Save new media files to device
- ✅ Works on all Android versions (6.0+)

## 🛠️ Setup

### Android Permissions

Add these permissions to `android/app/src/main/AndroidManifest.xml`:

```xml
<!-- For Android 6-12 -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" 
                 android:maxSdkVersion="32" />

<!-- For Android 13+ -->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
```

### iOS Setup (Optional)

Add to `ios/App/App/Info.plist`:

```xml
<key>NSPhotoLibraryUsageDescription</key>
<string>This app needs access to your photos and media</string>
```

## 📖 Basic Usage

### 1. Import the Plugin

```typescript
import { CapacitorMediaStore } from '@odion-cloud/capacitor-mediastore';
```

### 2. Request Permissions

```typescript
// Request all media permissions
const permissions = await CapacitorMediaStore.requestPermissions();

// Or request specific types
const audioOnly = await CapacitorMediaStore.requestPermissions({ 
  types: ['audio'] 
});
```

### 3. Get Media Files

```typescript
// Get all media files (photos, music, videos)
const allMedia = await CapacitorMediaStore.getMedias({
  limit: 50,
  includeExternal: true // Include SD card files
});

console.log(`Found ${allMedia.totalCount} files`);
```

### 4. Get Specific Media Types

```typescript
// Get all music files
const music = await CapacitorMediaStore.getMediasByType({
  mediaType: 'audio',
  sortBy: 'TITLE',
  includeExternal: true
});

// Get all photos
const photos = await CapacitorMediaStore.getMediasByType({
  mediaType: 'image',
  limit: 100
});

// Get all videos
const videos = await CapacitorMediaStore.getMediasByType({
  mediaType: 'video'
});

// Get documents (PDF, DOC, TXT, etc.)
const documents = await CapacitorMediaStore.getMediasByType({
  mediaType: 'document'
});
```

## 🎵 Music App Example

```typescript
async function buildMusicLibrary() {
  // Ask for permission to read music files
  await CapacitorMediaStore.requestPermissions({ types: ['audio'] });
  
  // Get all songs from device (including SD card)
  const result = await CapacitorMediaStore.getMediasByType({
    mediaType: 'audio',
    sortBy: 'TITLE',
    includeExternal: true
  });
  
  // Display songs with details
  result.media.forEach(song => {
    console.log(`${song.title} by ${song.artist}`);
    console.log(`Album: ${song.album}, Duration: ${song.duration}ms`);
    console.log(`Location: ${song.isExternal ? 'SD Card' : 'Internal'}`);
  });
  
  return result.media;
}
```

## 📸 Photo Gallery Example

```typescript
async function buildPhotoGallery() {
  // Ask for permission to read photos
  await CapacitorMediaStore.requestPermissions({ types: ['images'] });
  
  // Get recent photos
  const photos = await CapacitorMediaStore.getMediasByType({
    mediaType: 'image',
    sortBy: 'DATE_ADDED',
    sortOrder: 'DESC',
    limit: 50
  });
  
  return photos.media;
}
```

## 📋 API Reference

### Get All Media Files

```typescript
getMedias(options?: {
  limit?: number;           // How many files to get
  offset?: number;          // Skip first N files  
  sortBy?: string;          // Sort by: 'DATE_ADDED', 'TITLE', 'SIZE'
  sortOrder?: 'ASC' | 'DESC'; // Sort direction
  includeExternal?: boolean; // Include SD card (default: true)
})
```

### Get Media by Type

```typescript
getMediasByType(options: {
  mediaType: 'audio' | 'image' | 'video' | 'document';
  limit?: number;
  offset?: number;
  sortBy?: string;
  sortOrder?: 'ASC' | 'DESC';
  includeExternal?: boolean;
  albumName?: string;       // Filter by album (audio only)
  artistName?: string;      // Filter by artist (audio only)
})
```

### Get Music Albums

```typescript
getAlbums(): Promise<{
  albums: Array<{
    id: string;
    name: string;
    artist: string;
    trackCount: number;
    albumArtUri?: string;
  }>;
  totalCount: number;
}>
```

### Save Media File

```typescript
saveMedia(options: {
  data: string;             // Base64 data or file URI
  fileName: string;         // Name for the file
  mediaType: 'audio' | 'image' | 'video';
  albumName?: string;       // Album to save in (optional)
  relativePath?: string;    // Custom folder path (optional)
}): Promise<{
  success: boolean;
  uri?: string;            // URI of saved file
  error?: string;          // Error message if failed
}>
```

### Get Media Metadata

```typescript
getMediaMetadata(options: {
  filePath: string;        // URI of the media file
}): Promise<{
  media: MediaFile;        // File with complete metadata
}>
```

### Check/Request Permissions

```typescript
// Check current permissions
checkPermissions(): Promise<PermissionStatus>

// Request permissions
requestPermissions(options?: {
  types?: ('audio' | 'images' | 'video')[];
}): Promise<PermissionStatus>
```

## 📝 Media File Properties

When you get media files, each file includes:

```typescript
interface MediaFile {
  id: string;               // Unique ID
  uri: string;              // File path
  displayName: string;      // File name
  size: number;             // File size in bytes
  mimeType: string;         // File type (e.g., 'audio/mp3')
  dateAdded: number;        // When added to device
  mediaType: string;        // 'audio', 'image', 'video'
  
  // For music files
  title?: string;           // Song title
  artist?: string;          // Artist name
  album?: string;           // Album name
  albumArtist?: string;     // Album artist
  composer?: string;        // Song composer
  duration?: number;        // Length in milliseconds
  genre?: string;           // Music genre
  year?: number;            // Release year
  track?: number;           // Track number
  bitrate?: number;         // Audio bitrate (kbps)
  sampleRate?: number;      // Sample rate (Hz)
  channels?: number;        // Audio channels (1=mono, 2=stereo)
  albumArtUri?: string;     // Album cover image URI
  
  // For images/videos
  width?: number;           // Image/video width
  height?: number;          // Image/video height
  
  // Storage info
  isExternal?: boolean;     // true if on SD card
}
```

## 🔧 Permission Types

| Type | What It Accesses | Android Version |
|------|------------------|-----------------|
| `audio` | Music, podcasts, sound files | All versions |
| `images` | Photos, pictures, images | All versions |
| `video` | Videos, movies | All versions |
| `document` | PDF, DOC, TXT, and other documents | All versions |

## ❓ Common Questions

### How do I access SD card files?

Set `includeExternal: true` in your options:

```typescript
const result = await CapacitorMediaStore.getMediasByType({
  mediaType: 'audio',
  includeExternal: true  // This includes SD card
});
```

### Why am I not getting any files?

1. Make sure you requested permissions first
2. Check that permissions were granted
3. Try running on a real device (not simulator)

```typescript
// Debug permissions
const permissions = await CapacitorMediaStore.checkPermissions();
console.log('Current permissions:', permissions);
```

### How do I filter music by artist or album?

```typescript
// Get songs by specific artist
const artistSongs = await CapacitorMediaStore.getMediasByType({
  mediaType: 'audio',
  artistName: 'Taylor Swift'
});

// Get songs from specific album
const albumSongs = await CapacitorMediaStore.getMediasByType({
  mediaType: 'audio',
  albumName: 'Abbey Road'
});
```

### How do I save a new media file?

```typescript
// Save an audio file
const result = await CapacitorMediaStore.saveMedia({
  data: 'base64-encoded-audio-data',
  fileName: 'my-song.mp3',
  mediaType: 'audio',
  albumName: 'My Custom Album'
});

if (result.success) {
  console.log('File saved at:', result.uri);
}
```

### How do I get detailed metadata for a file?

```typescript
// Get complete metadata for a specific song
const metadata = await CapacitorMediaStore.getMediaMetadata({
  filePath: 'content://media/external/audio/media/123'
});

console.log('Song:', metadata.media.title);
console.log('Bitrate:', metadata.media.bitrate);
console.log('Sample Rate:', metadata.media.sampleRate);
```

## 🤝 Platform Support

### 📱 Supported Devices

| Platform | Support Level | Features | Notes |
|----------|---------------|----------|-------|
| **Android 6.0+** | ✅ **Full Support** | All media types, SD card, rich metadata | Production ready |
| **iOS 12+** | 🚧 **Coming Soon** | Photos, videos, music library | Expected Q2 2025 |
| **Web** | ⚠️ **Development Only** | Mock data for testing | Not for production |

### 🏆 Android Compatibility

- **Android 14+** (API 34+): Visual media permissions, latest features
- **Android 13** (API 33): Granular media permissions 
- **Android 10-12** (API 29-32): Scoped storage, external volumes
- **Android 6-9** (API 23-28): Runtime permissions, full SD card access
- **Android 5** (API 21-22): Basic support, limited external storage

### 📱 iOS Support Coming Soon!

We're actively working on iOS support with full Photos framework integration. Expected features:
- Access to Photos and Videos
- Music library integration
- Album and artist metadata
- Expected release: **Q2 2025**

## 💝 Support This Project

Help me improve this plugin and build better tools for the community!

### 💳 Fiat Currency Support
- **PayPal**: [paypal.me/odiondeveloper](https://paypal.me/odiondeveloper)
- **Ko-fi**: [ko-fi.com/odiondeveloper](https://ko-fi.com/odiondeveloper)
- **GitHub Sponsors**: [github.com/sponsors/odiondeveloper](https://github.com/sponsors/odiondeveloper)
- **Buy Me a Coffee**: [buymeacoffee.com/odiondeveloper](https://buymeacoffee.com/odiondeveloper)

### ₿ Cryptocurrency Support
- **Bitcoin (BTC)**: `bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh`
- **Ethereum (ETH)**: `0x742d35Cc6634C0532925a3b8D09E66b0e17236c1`
- **USDT (TRC20)**: `TLNGFxWKBpB5a6RPB1XLZi5xzWVx3e1XQ3`
- **Binance Coin (BNB)**: `bnb136ns6lfw4zs5hg4n85vdthaad7hq5m4gtkgf23`

### 💻 Why Support?
Your contributions help me:
- Upgrade to better development hardware
- Improve my workspace and productivity  
- Dedicate more time to open source projects
- Add iOS support and new features faster
- Provide better documentation and examples

### 🤝 Other Ways to Help
- ⭐ **Star the project** on GitHub
- 🐛 **Report issues** and suggest features
- 📖 **Improve documentation** 
- 💬 **Share with other developers**

## 📄 License

MIT License - feel free to use in your projects!

---

**Need help?** Check the examples above or create an issue on GitHub.
