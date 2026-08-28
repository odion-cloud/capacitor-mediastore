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
- ? **Native file picker** � Save As & Open document browser (SAF on Android, UIDocumentPicker on iOS)
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


---

## File Picker � Native Save As & Open

Open native file browsers on Android (SAF � Storage Access Framework) and iOS (UIDocumentPicker) to let users choose where to save files or pick files to open. No storage permissions required � the system grants URI-based access.

> **Note:** These methods are designed for native Android/iOS only. On web, use `window.showSaveFilePicker()` and `<input type="file">` instead.

### Save a File (Native "Save As" Dialog)

Opens the system document browser where the user picks a destination folder and filename.

```typescript
// Generate your data as a base64 string
const base64Data = btoa("Hello, world!"); // or from a Blob/ArrayBuffer

const result = await CapacitorMediaStore.saveFileAs({
  fileName: "MyBackup_20260827.backup",
  mimeType: "application/octet-stream", // optional, default: application/octet-stream
  data: base64Data,
});

if (result.cancelled) {
  console.log("User cancelled the save picker");
} else if (result.success) {
  console.log("File saved to:", result.uri);
}
```

### Open a File (Native File Browser)

Opens the system file browser where the user selects a file. Returns the file content as base64.

```typescript
const result = await CapacitorMediaStore.openFile({
  mimeTypes: ["application/octet-stream", "application/zip"],
});

if (result.cancelled) {
  console.log("User cancelled the file picker");
} else {
  console.log("File name:", result.name);
  console.log("MIME type:", result.mimeType);
  console.log("Size:", result.size, "bytes");
  // result.data contains the file as a base64 string
  const rawBytes = atob(result.data);
}
```

### Example: Backup & Restore Workflow

```typescript
import { CapacitorMediaStore } from "@odion-cloud/capacitor-mediastore";
import JSZip from "jszip";

// -- Create & Save a Backup --
async function createBackup() {
  const zip = new JSZip();
  zip.file("data.json", JSON.stringify({ playlists: [] }));

  const zipBase64 = await zip.generateAsync({
    type: "base64",
    compression: "DEFLATE",
  });

  const result = await CapacitorMediaStore.saveFileAs({
    fileName: "MyApp_Backup.zip",
    mimeType: "application/zip",
    data: zipBase64,
  });

  return result.success;
}

// -- Open & Restore a Backup --
async function restoreBackup() {
  const result = await CapacitorMediaStore.openFile({
    mimeTypes: ["application/zip", "application/octet-stream"],
  });

  if (result.cancelled) return;

  const zip = await JSZip.loadAsync(result.data, { base64: true });
  const dataJson = await zip.file("data.json")?.async("text");
  const restored = JSON.parse(dataJson);
  console.log("Restored playlists:", restored.playlists.length);
}
```

### SaveFileAsOptions

```typescript
interface SaveFileAsOptions {
  fileName: string;                // Suggested file name for the save dialog
  mimeType?: string;               // MIME type (default: 'application/octet-stream')
  data: string;                    // File content as base64-encoded string
}
```

### SaveFileAsResult

```typescript
interface SaveFileAsResult {
  success?: boolean;               // Whether the save was successful
  cancelled?: boolean;             // Whether the user cancelled the picker
  uri?: string;                    // URI of the saved file (platform-specific)
}
```

### OpenFileOptions

```typescript
interface OpenFileOptions {
  mimeTypes?: string[];            // Array of MIME types to filter (default: all types)
}
```

### OpenFileResult

```typescript
interface OpenFileResult {
  data?: string;                   // File content as base64 string
  name?: string;                   // File name
  mimeType?: string;               // MIME type of the file
  size?: number;                   // File size in bytes
  cancelled?: boolean;             // Whether the user cancelled the picker
}
```

### Platform Behavior

| Platform    | Save As                                              | Open File                                            |
| ----------- | ---------------------------------------------------- | ---------------------------------------------------- |
| **Android** | SAF `ACTION_CREATE_DOCUMENT` (full folder browser)     | SAF `ACTION_OPEN_DOCUMENT` (full file browser)         |
| **iOS**     | `UIDocumentPicker(forExporting:)`                      | `UIDocumentPicker(forOpeningContentTypes:)`             |
| **Web**     | N/A � use `showSaveFilePicker()`                       | N/A � use `<input type="file">`                        |

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


### File Picker Methods (Save As & Open)

| Method                    | Description                                                             |
| ------------------------- | ----------------------------------------------------------------------- |
| `saveFileAs(options)`     | Open native "Save As" picker to save a file to user-chosen location     |
| `openFile(options?)`      | Open native file browser to select and read a file                      |
### Media Session Methods (JS API)

| Method                               | Description                             |
| ------------------------------------ | --------------------------------------- |
| `setMediaSession(options)`           | Create/update native media notification |
| `updatePlaybackState(options)`       | Update playing/paused/buffering state   |
| `updatePositionState(options)`       | Update progress bar position            |
| `updateMediaSessionButtons(options)` | Dynamically update button icons         |
| `destroyMediaSession()`              | Dismiss the media notification          |

### Media Session Methods (Native Plugin API)

These methods allow other native Capacitor plugins (e.g. a media player plugin) to control the media session **directly from Kotlin/Swift** — no JS bridge needed. This is critical for background/Doze mode resilience.

| Method (Android/Kotlin)                                               | Description                                           |
| --------------------------------------------------------------------- | ----------------------------------------------------- |
| `setNativeActionListener(listener)`                                    | Register a callback for notification button taps       |
| `setMediaSessionNative(title, artist, album?, artworkUrl?, duration?, smallIconName?, buttons?)` | Create/update the notification |
| `updatePlaybackStateNative(state)`                                     | Update playing/paused state                           |
| `updatePositionStateNative(position, duration, playbackRate)`          | Update progress bar                                   |
| `updateMediaSessionButtonsNative(buttons)`                             | Update button icons                                   |
| `destroyMediaSessionNative()`                                          | Dismiss the notification                              |

**Usage from another Capacitor plugin (Kotlin):**

```kotlin
// Get the MediaStore plugin instance (both plugins loaded on the same bridge)
val mediaStore = bridge.getPlugin("CapacitorMediaStore").instance as MediaStorePlugin

// Register native action listener (works even in Doze mode)
mediaStore.setNativeActionListener { action, extras ->
    when (action) {
        "play" -> exoPlayer.play()
        "pause" -> exoPlayer.pause()
        "nexttrack" -> exoPlayer.nextTrack()
        "like" -> toggleFavourite()
        "repeat" -> cycleRepeatMode()
    }
}

// Set metadata with buttons
mediaStore.setMediaSessionNative(
    title = "Bohemian Rhapsody",
    artist = "Queen",
    artworkUrl = "https://example.com/art.jpg",
    duration = 354L,
    smallIconName = "ic_notification",
    buttons = listOf(
        MediaSessionService.CustomButton("like", "Like", "ic_favorite_border", true, "left"),
        MediaSessionService.CustomButton("repeat", "Repeat", "ic_repeat_all", true, "right")
    )
)

// Sync playback state
mediaStore.updatePlaybackStateNative("playing")

// Update progress bar (from a heartbeat/timer)
mediaStore.updatePositionStateNative(position = 45.0, duration = 354.0, playbackRate = 1.0f)
```

**Usage from another Capacitor plugin (Swift):**

```swift
// Get the MediaStore implementation instance
let mediaStore = bridge.plugin(withName: "CapacitorMediaStore") as? CapacitorMediaStorePlugin

// Register native action listener
mediaStore?.implementation.setNativeActionListener { action, extras in
    switch action {
    case "play": player.play()
    case "pause": player.pause()
    default: break
    }
}

// Set metadata
mediaStore?.implementation.setMediaSessionNative(
    title: "Bohemian Rhapsody",
    artist: "Queen",
    artworkUrl: "https://example.com/art.jpg",
    duration: 354.0
)
```

### Artwork Sources

The media session supports artwork from multiple sources:

| Source | Format | Example |
|---|---|---|
| HTTP/HTTPS URL | Remote image | `https://example.com/art.jpg` |
| Base64 data URI | Inline image | `data:image/png;base64,...` |
| Local file path | Device storage | `/storage/emulated/0/Music/art.jpg` |
| `file://` URI | Device storage | `file:///storage/emulated/0/Music/art.jpg` |
| Audio file path | Embedded album art | `/storage/emulated/0/Music/song.mp3` |
| `content://` URI | Content provider | `content://media/external/audio/albumart/123` |

> **Offline artwork**: When you pass an audio file path (e.g. an MP3), the plugin automatically extracts embedded album art using `MediaMetadataRetriever` (Android) or `AVAsset` (iOS).

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
