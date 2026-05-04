# Capacitor MediaStore Plugin

A powerful Capacitor plugin for accessing device media files (music, photos, videos), real-time file change detection, and native media session notifications on Android & iOS.

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
- ✅ **Real-time file change detection** (added, deleted, modified)
- ✅ **Native media session notifications** with custom buttons (like, shuffle, repeat)
- ✅ **Custom app logo** in media notifications
- ✅ **Lock screen controls** with progress bar
- ✅ Works on all Android versions (5.0+) and iOS (12+)

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

<!-- For Media Session notifications (Android 13+) -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

### iOS Setup

Add to `ios/App/App/Info.plist`:

```xml
<key>NSPhotoLibraryUsageDescription</key>
<string>This app needs access to your photos and media</string>
```

For media session background audio, enable **Audio, AirPlay, and Picture in Picture** in your Xcode project's Background Modes.

## 📖 Basic Usage

### 1. Import the Plugin

```typescript
import { CapacitorMediaStore } from "@odion-cloud/capacitor-mediastore";
```

### 2. Request Permissions

```typescript
// Request all media permissions
const permissions = await CapacitorMediaStore.requestPermissions();

// Or request specific types
const audioOnly = await CapacitorMediaStore.requestPermissions({
  types: ["audio"],
});
```

### 3. Get Media Files

```typescript
// Get all media files (photos, music, videos)
const allMedia = await CapacitorMediaStore.getMedias({
  limit: 50,
  includeExternal: true, // Include SD card files
});

console.log(`Found ${allMedia.totalCount} files`);
```

### 4. Get Specific Media Types

```typescript
// Get all music files
const music = await CapacitorMediaStore.getMediasByType({
  mediaType: "audio",
  sortBy: "TITLE",
  includeExternal: true,
});

// Get all photos
const photos = await CapacitorMediaStore.getMediasByType({
  mediaType: "image",
  limit: 100,
});

// Get all videos
const videos = await CapacitorMediaStore.getMediasByType({
  mediaType: "video",
});

// Get documents (PDF, DOC, TXT, etc.)
const documents = await CapacitorMediaStore.getMediasByType({
  mediaType: "document",
});
```

---

## 👁️ File Change Watcher

Detect when files are added, deleted, or modified on the device in real-time.

### Start Watching

```typescript
// Listen for file changes
CapacitorMediaStore.addListener("mediaChanged", (event) => {
  console.log(`File ${event.type}:`, event.file);
  // event.type: 'added' | 'deleted' | 'modified'
  // event.file: { id, displayName, uri, mimeType, mediaType, ... }
});

// Start watching all media types
await CapacitorMediaStore.startWatching();

// Or watch specific types only
await CapacitorMediaStore.startWatching({
  mediaTypes: ["audio", "image"],
});
```

### Stop Watching

```typescript
await CapacitorMediaStore.stopWatching();
```

### Example: Auto-refresh Music Library

```typescript
async function setupAutoRefresh() {
  CapacitorMediaStore.addListener("mediaChanged", (event) => {
    if (event.file.mediaType === "audio") {
      if (event.type === "added") {
        console.log("New song downloaded:", event.file.displayName);
        refreshMusicLibrary();
      } else if (event.type === "deleted") {
        console.log("Song removed:", event.file.displayName);
        removeFromLibrary(event.file.id);
      }
    }
  });

  await CapacitorMediaStore.startWatching({ mediaTypes: ["audio"] });
}
```

---

## 🎵 Native Media Session

Create native media playback notifications with lock screen controls, just like Spotify or Apple Music.

### 🎨 Icon Setup (Android)

Before using media session buttons, you need to add icon drawables to your Android project:

1. **Location:** `android/app/src/main/res/drawable/`
2. **Format:** Vector XML (24×24dp) or PNG (48×48px mdpi / 96×96px xhdpi)
3. **Naming:** Lowercase with underscores, prefixed with `ic_`

**Required icon files:**

| File                     | Purpose                                                                  |
| ------------------------ | ------------------------------------------------------------------------ |
| `ic_notification.xml`    | Small notification icon (your app logo, monochrome white-on-transparent) |
| `ic_play.xml`            | Play button                                                              |
| `ic_pause.xml`           | Pause button                                                             |
| `ic_skip_previous.xml`   | Previous track                                                           |
| `ic_skip_next.xml`       | Next track                                                               |
| `ic_shuffle.xml`         | Shuffle off                                                              |
| `ic_shuffle_on.xml`      | Shuffle on                                                               |
| `ic_favorite_border.xml` | Not liked (outline heart)                                                |
| `ic_favorite.xml`        | Liked (filled heart)                                                     |
| `ic_repeat.xml`          | Repeat off / normal                                                      |
| `ic_repeat_one.xml`      | Repeat one                                                               |
| `ic_repeat_all.xml`      | Repeat all (optional)                                                    |

> **Tip:** Use Android Studio → Right-click `res/drawable` → New → Vector Asset → search Material Icons.

### Basic Usage

```typescript
await CapacitorMediaStore.setMediaSession({
  title: "Bohemian Rhapsody",
  artist: "Queen",
  album: "A Night at the Opera",
  artworkUrl: "https://example.com/album-art.jpg",
  duration: 354, // seconds
  smallIconName: "ic_notification", // Your app logo (24dp, monochrome)
  buttons: [
    // position: 'left' = before Previous button
    {
      action: "shuffle",
      displayName: "Shuffle",
      iconName: "ic_shuffle",
      enabled: true,
      position: "left",
    },
    // position: 'right' = after Next button (default)
    {
      action: "like",
      displayName: "Like",
      iconName: "ic_favorite_border",
      enabled: true,
      position: "right",
    },
    {
      action: "repeat",
      displayName: "Repeat",
      iconName: "ic_repeat",
      enabled: true,
      position: "right",
    },
  ],
});

// Update playback state
await CapacitorMediaStore.updatePlaybackState({ state: "playing" });
```

> The notification background automatically tints with the dominant color from the album artwork (like YouTube Music / Spotify on Android 13+).

### Listen for Button Events

```typescript
CapacitorMediaStore.addListener("mediaSessionAction", (event) => {
  switch (event.action) {
    case "play":
      player.play();
      break;
    case "pause":
      player.pause();
      break;
    case "nexttrack":
      player.next();
      break;
    case "previoustrack":
      player.previous();
      break;
    case "like":
      toggleLike();
      break;
    case "shuffle":
      toggleShuffle();
      break;
    case "repeat":
      toggleRepeat();
      break;
    case "seekto":
      player.seekTo(event.extras.seekTime);
      break;
  }
});
```

### Update Progress Bar

```typescript
// Update position periodically (e.g., every second)
setInterval(async () => {
  await CapacitorMediaStore.updatePositionState({
    position: player.getCurrentTime(),
    duration: player.getDuration(),
    playbackRate: 1.0,
  });
}, 1000);
```

### Dynamically Update Buttons

```typescript
// Toggle like icon (outline → filled)
await CapacitorMediaStore.updateMediaSessionButtons({
  buttons: [
    {
      action: "like",
      displayName: "Unlike",
      iconName: "ic_favorite",
      enabled: true,
      position: "right",
    },
  ],
});

// Cycle repeat modes (4 states)
// Mode 1: Repeat Off
await CapacitorMediaStore.updateMediaSessionButtons({
  buttons: [
    {
      action: "repeat",
      displayName: "Repeat Off",
      iconName: "ic_repeat",
      position: "right",
    },
  ],
});
// Mode 2: Repeat All
await CapacitorMediaStore.updateMediaSessionButtons({
  buttons: [
    {
      action: "repeat",
      displayName: "Repeat All",
      iconName: "ic_repeat_all",
      position: "right",
    },
  ],
});
// Mode 3: Repeat One
await CapacitorMediaStore.updateMediaSessionButtons({
  buttons: [
    {
      action: "repeat",
      displayName: "Repeat One",
      iconName: "ic_repeat_one",
      position: "right",
    },
  ],
});
// Mode 4: Shuffle
await CapacitorMediaStore.updateMediaSessionButtons({
  buttons: [
    {
      action: "shuffle",
      displayName: "Shuffle On",
      iconName: "ic_shuffle_on",
      position: "left",
    },
  ],
});
```

### Destroy Session

```typescript
await CapacitorMediaStore.destroyMediaSession();
```

---

## 🎵 Music App Example

```typescript
async function buildMusicLibrary() {
  await CapacitorMediaStore.requestPermissions({ types: ["audio"] });

  const result = await CapacitorMediaStore.getMediasByType({
    mediaType: "audio",
    sortBy: "TITLE",
    includeExternal: true,
  });

  result.media.forEach((song) => {
    console.log(`${song.title} by ${song.artist}`);
    console.log(`Album: ${song.album}, Duration: ${song.duration}ms`);
    console.log(`Location: ${song.isExternal ? "SD Card" : "Internal"}`);
  });

  return result.media;
}
```

## 📋 API Reference

### Media Store Methods

| Method                         | Description                                             |
| ------------------------------ | ------------------------------------------------------- |
| `getMedias(options?)`          | Get all media files from device                         |
| `getMediasByType(options)`     | Get media files by type (audio, image, video, document) |
| `getAlbums()`                  | Get all music albums                                    |
| `saveMedia(options)`           | Save a media file to device storage                     |
| `getMediaMetadata(options)`    | Get detailed metadata for a specific file               |
| `checkPermissions()`           | Check current permission status                         |
| `requestPermissions(options?)` | Request media access permissions                        |

### File Watcher Methods

| Method                    | Description                     |
| ------------------------- | ------------------------------- |
| `startWatching(options?)` | Start watching for file changes |
| `stopWatching()`          | Stop watching for file changes  |

### Media Session Methods

| Method                               | Description                             |
| ------------------------------------ | --------------------------------------- |
| `setMediaSession(options)`           | Create/update native media notification |
| `updatePlaybackState(options)`       | Update playing/paused/buffering state   |
| `updatePositionState(options)`       | Update progress bar position            |
| `updateMediaSessionButtons(options)` | Dynamically update button icons         |
| `destroyMediaSession()`              | Dismiss the media notification          |

### Events

| Event                | Description                                      |
| -------------------- | ------------------------------------------------ |
| `mediaChanged`       | Fired when a file is added, deleted, or modified |
| `mediaSessionAction` | Fired when a notification button is pressed      |

## 📝 Media File Properties

```typescript
interface MediaFile {
  id: string; // Unique ID
  uri: string; // File path
  displayName: string; // File name
  size: number; // File size in bytes
  mimeType: string; // File type (e.g., 'audio/mp3')
  dateAdded: number; // When added to device
  mediaType: string; // 'audio', 'image', 'video'

  // For music files
  title?: string; // Song title
  artist?: string; // Artist name
  album?: string; // Album name
  albumArtist?: string; // Album artist
  composer?: string; // Song composer
  duration?: number; // Length in milliseconds
  genre?: string; // Music genre
  year?: number; // Release year
  track?: number; // Track number
  albumArtUri?: string; // Album cover image URI

  // For images/videos
  width?: number; // Width in pixels
  height?: number; // Height in pixels

  // Storage info
  isExternal?: boolean; // true if on SD card
}
```

## 🤝 Platform Support

| Platform         | Support Level       | Features                                              |
| ---------------- | ------------------- | ----------------------------------------------------- |
| **Android 5.0+** | ✅ **Full Support** | All media types, SD card, media session, file watcher |
| **iOS 12+**      | ✅ **Supported**    | File watcher, media session, photo library            |
| **Web**          | ⚠️ **Partial**      | Media session (via Web API), no file watcher          |

## 💝 Support This Project

### 🤝 GitHub Sponsors

- **GitHub Sponsors**: [github.com/sponsors/odion-cloud](https://github.com/sponsors/odion-cloud)

### ₿ Cryptocurrency Support

- **Bitcoin (BTC)**: `bc1q2k0ftm2fgst22kzj683e8gpau3spfa23ttkg26`
- **USDT (Ethereum)**: `0xd6f4d8733c8C23e7bEC8Aeba37F4b3D2e93172d1`
- **USDT (BNB Chain)**: `0xd6f4d8733c8C23e7bEC8Aeba37F4b3D2e93172d1`
- **USDT (TRON/TRC20)**: `TXVy781mQ2tCuQ1BrattXWueUHp1wB5fwt`
- **USDT (Solana)**: `GZ8jmSUUzc4dQF7Cthj2atomvpBZWqccR81N9DL4o1Be`
- **USDT (TON)**: `UQAthXSNIlauj3SrzpDAU4VYxgEVV3niOSmeTPCtMBKGfEAE`

### 🤝 Other Ways to Help

- ⭐ **Star the project** on GitHub
- 🐛 **Report issues** and suggest features
- 📖 **Improve documentation**
- 💬 **Share with other developers**

## 📄 License

MIT License - feel free to use in your projects!

---

**Need help?** Check the examples above or create an issue on GitHub.
