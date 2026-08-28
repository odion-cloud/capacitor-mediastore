import type { PluginListenerHandle } from '@capacitor/core';

export interface CapacitorMediaStorePlugin {
  /**
   * Get all media files from device storage
   */
  getMedias(options?: MediaQueryOptions): Promise<MediaResponse>;

  /**
   * Get media files by specific type
   */
  getMediasByType(options: MediaTypeOptions): Promise<MediaResponse>;

  /**
   * Get all albums from device
   */
  getAlbums(): Promise<AlbumResponse>;

  /**
   * Save media file to device storage
   */
  saveMedia(options: SaveMediaOptions): Promise<SaveMediaResponse>;

  /**
   * Get detailed metadata for a specific media file
   */
  getMediaMetadata(options: MediaMetadataOptions): Promise<MediaMetadataResponse>;

  /**
   * Check and request necessary permissions
   */
  checkPermissions(): Promise<PermissionStatus>;

  /**
   * Request permissions for media access
   */
  requestPermissions(options?: RequestPermissionsOptions): Promise<PermissionStatus>;

  // ──────────────────────────────────────────────
  // File Change Watcher
  // ──────────────────────────────────────────────

  /**
   * Start watching for file changes in the device media store.
   * Emits 'mediaChanged' events when files are added, deleted, or modified.
   * Register a listener with addListener('mediaChanged', callback) first.
   */
  startWatching(options?: WatchOptions): Promise<void>;

  /**
   * Stop watching for file changes.
   */
  stopWatching(): Promise<void>;

  // ──────────────────────────────────────────────
  // Native Media Session
  // ──────────────────────────────────────────────

  /**
   * Create or update a native media session notification.
   * On Android, this starts a foreground service with media controls.
   * On iOS, this updates MPNowPlayingInfoCenter.
   */
  setMediaSession(options: MediaSessionOptions): Promise<void>;

  /**
   * Update the playback state (playing, paused, buffering, stopped).
   */
  updatePlaybackState(options: PlaybackStateOptions): Promise<void>;

  /**
   * Update the position/progress bar in the media notification.
   */
  updatePositionState(options: PositionStateOptions): Promise<void>;

  /**
   * Update the custom buttons shown in the media notification.
   * Use this to dynamically change button icons (e.g., toggle like icon).
   */
  updateMediaSessionButtons(options: MediaSessionButtonOptions): Promise<void>;

  /**
   * Destroy the media session and dismiss the notification.
   */

  // ----------------------------------------------------------------
  // File Picker � Save As & Open
  // ----------------------------------------------------------------

  /**
   * Opens the native "Save As" file picker (Android SAF CreateDocument / iOS UIDocumentPicker).
   * User selects where to save the file. Data is provided as a base64 string.
   */
  saveFileAs(options: SaveFileAsOptions): Promise<SaveFileAsResult>;

  /**
   * Opens the native "Open" file picker (Android SAF OpenDocument / iOS UIDocumentPicker).
   * User selects a file to open. Returns file data as a base64 string.
   */
  openFile(options?: OpenFileOptions): Promise<OpenFileResult>;

  destroyMediaSession(): Promise<void>;

}

// ──────────────────────────────────────────────
// Existing interfaces (unchanged)
// ──────────────────────────────────────────────

export interface MediaQueryOptions {
  /**
   * Limit the number of results
   */
  limit?: number;

  /**
   * Offset for pagination
   */
  offset?: number;

  /**
   * Sort order (ASC or DESC)
   */
  sortOrder?: 'ASC' | 'DESC';

  /**
   * Sort by field
   */
  sortBy?: 'DATE_ADDED' | 'DATE_MODIFIED' | 'DISPLAY_NAME' | 'SIZE' | 'TITLE';

  /**
   * Filter by album name
   */
  albumName?: string;

  /**
   * Filter by artist name
   */
  artistName?: string;

  /**
   * Include external storage (SD card)
   */
  includeExternal?: boolean;
}

export interface MediaTypeOptions extends MediaQueryOptions {
  /**
   * Type of media to retrieve
   */
  mediaType: MediaType;
}

export interface MediaMetadataOptions {
  /**
   * File path or URI of the media file
   */
  filePath: string;
}

export interface SaveMediaOptions {
  /**
   * Base64 encoded data or file URI
   */
  data: string;

  /**
   * File name
   */
  fileName: string;

  /**
   * Media type
   */
  mediaType: MediaType;

  /**
   * Album name (optional)
   */
  albumName?: string;

  /**
   * Relative path within the media type directory
   */
  relativePath?: string;
}

export interface RequestPermissionsOptions {
  /**
   * Specific permission types to request. If not provided, all permissions will be requested.
   * Available types: 'images', 'audio', 'video'
   */
  types?: string[];
}

export interface MediaFile {
  /**
   * Unique identifier
   */
  id: string;

  /**
   * File path/URI
   */
  uri: string;

  /**
   * Display name
   */
  displayName: string;

  /**
   * File size in bytes
   */
  size: number;

  /**
   * MIME type
   */
  mimeType: string;

  /**
   * Date added (timestamp)
   */
  dateAdded: number;

  /**
   * Date modified (timestamp)
   */
  dateModified: number;

  /**
   * Media type
   */
  mediaType: MediaType;

  /**
   * Width (for images/videos)
   */
  width?: number;

  /**
   * Height (for images/videos)
   */
  height?: number;

  /**
   * Duration in milliseconds (for audio/video)
   */
  duration?: number;

  /**
   * Title (for audio files)
   */
  title?: string;

  /**
   * Artist (for audio files)
   */
  artist?: string;

  /**
   * Album (for audio files)
   */
  album?: string;

  /**
   * Album artist (for audio files)
   */
  albumArtist?: string;

  /**
   * Composer (for audio files)
   */
  composer?: string;

  /**
   * Genre (for audio files)
   */
  genre?: string;

  /**
   * Track number (for audio files)
   */
  track?: number;

  /**
   * Year (for audio files)
   */
  year?: number;

  /**
   * Bitrate (for audio files)
   */
  bitrate?: number;

  /**
   * Sample rate (for audio files)
   */
  sampleRate?: number;

  /**
   * Number of channels (for audio files)
   */
  channels?: number;

  /**
   * Album art URI (for audio files)
   */
  albumArtUri?: string;

  /**
   * Is on external storage
   */
  isExternal?: boolean;
}

export interface Album {
  /**
   * Album ID
   */
  id: string;

  /**
   * Album name
   */
  name: string;

  /**
   * Number of tracks
   */
  trackCount: number;

  /**
   * First track ID
   */
  firstTrackId?: string;

  /**
   * Album art URI
   */
  albumArtUri?: string;

  /**
   * Artist name
   */
  artist?: string;
}

export interface MediaResponse {
  /**
   * Array of media files
   */
  media: MediaFile[];

  /**
   * Total count (useful for pagination)
   */
  totalCount: number;

  /**
   * Whether there are more results
   */
  hasMore: boolean;
}

export interface AlbumResponse {
  /**
   * Array of albums
   */
  albums: Album[];

  /**
   * Total count
   */
  totalCount: number;
}

export interface MediaMetadataResponse {
  /**
   * Media file with full metadata
   */
  media: MediaFile;
}

export interface SaveMediaResponse {
  /**
   * Success status
   */
  success: boolean;

  /**
   * URI of saved file
   */
  uri?: string;

  /**
   * Error message if failed
   */
  error?: string;
}

export interface PermissionStatus {
  /**
   * Read external storage permission (Android 6-12)
   */
  readExternalStorage?: PermissionState;

  /**
   * Read media images permission (Android 13+)
   */
  readMediaImages?: PermissionState;

  /**
   * Read media audio permission (Android 13+)
   */
  readMediaAudio?: PermissionState;

  /**
   * Read media video permission (Android 13+)
   */
  readMediaVideo?: PermissionState;

  /**
   * Read visual media user selected permission (Android 14+)
   */
  readMediaVisualUserSelected?: PermissionState;

  /**
   * Write external storage permission (Android 6-9)
   */
  writeExternalStorage?: PermissionState;
}

// ──────────────────────────────────────────────
// File Change Watcher types
// ──────────────────────────────────────────────

export interface WatchOptions {
  /**
   * Filter which media types to watch.
   * Default: watches all types.
   */
  mediaTypes?: MediaType[];
}

export interface MediaChangeEvent {
  /**
   * The type of change detected
   */
  type: 'added' | 'deleted' | 'modified';

  /**
   * The file that was changed.
   * For 'deleted' events, only the 'id' field may be available.
   */
  file: MediaFile;
}

// ──────────────────────────────────────────────
// Native Media Session types
// ──────────────────────────────────────────────

export interface MediaSessionOptions {
  /**
   * Song title
   */
  title: string;

  /**
   * Artist name
   */
  artist: string;

  /**
   * Album name
   */
  album?: string;

  /**
   * Artwork URL (http/https) or base64 data URI
   */
  artworkUrl?: string;

  /**
   * Duration in seconds
   */
  duration?: number;

  /**
   * Android only: drawable resource name for the small notification icon (your app logo).
   *
   * **How to set up:**
   * 1. Create a monochrome white-on-transparent icon
   * 2. Format: Vector XML (24×24dp) or PNG (48×48px mdpi / 72×72 hdpi / 96×96 xhdpi)
   * 3. Place it in: `android/app/src/main/res/drawable/ic_notification.xml`
   *    (or `res/drawable-mdpi/ic_notification.png`, `res/drawable-xhdpi/ic_notification.png`, etc.)
   * 4. Pass just the name without extension: `smallIconName: 'ic_notification'`
   *
   * If not provided or not found, defaults to a generic Play icon.
   */
  smallIconName?: string;

  /**
   * Custom buttons to show in the notification.
   * Standard transport buttons (prev, play/pause, next) are always included.
   */
  buttons?: MediaSessionButton[];
}

export interface PlaybackStateOptions {
  /**
   * Current playback state
   */
  state: 'playing' | 'paused' | 'buffering' | 'stopped' | 'none';
}

export interface PositionStateOptions {
  /**
   * Current playback position in seconds
   */
  position: number;

  /**
   * Total duration in seconds
   */
  duration: number;

  /**
   * Playback speed. Default: 1.0
   */
  playbackRate?: number;
}

export interface MediaSessionButton {
  /**
   * Action identifier. Built-in actions: 'like', 'shuffle', 'repeat', 'radio'.
   * You can also use custom action names.
   */
  action: string;

  /**
   * Display name shown as accessibility label
   */
  displayName: string;

  /**
   * The drawable resource name for the button icon (WITHOUT file extension).
   *
   * **Android:** Place icon files in `android/app/src/main/res/drawable/`.
   * - Format: Vector XML (24dp) or PNG (48×48px for mdpi, 96×96px for xhdpi)
   * - Naming: Use lowercase with underscores, e.g. `ic_shuffle.xml`
   * - The plugin resolves by trying: exact name → `ic_<name>` → `ms_<name>`
   * - Example: `iconName: 'ic_shuffle'` → looks for `res/drawable/ic_shuffle.xml`
   * - Example: `iconName: 'shuffle'` → looks for `res/drawable/shuffle.xml`,
   *   then `res/drawable/ic_shuffle.xml`, then `res/drawable/ms_shuffle.xml`
   *
   * **iOS:** SF Symbol name (e.g., 'heart', 'shuffle').
   *
   * **Tip:** You can use Android Studio's Vector Asset tool to import Material
   * Icons directly as XML drawables.
   */
  iconName: string;

  /**
   * Whether the button is enabled/visible. Default: true
   */
  enabled?: boolean;

  /**
   * Position of the button relative to transport controls.
   * - `'left'`: Displayed BEFORE the Previous button
   * - `'right'`: Displayed AFTER the Next button
   * Default: `'right'`
   */
  position?: 'left' | 'right';
}

export interface MediaSessionButtonOptions {
  /**
   * Updated list of custom buttons
   */
  buttons: MediaSessionButton[];
}

export interface MediaSessionActionEvent {
  /**
   * The action that was triggered.
   * Values: 'play', 'pause', 'nexttrack', 'previoustrack', 'stop', 'like', 'shuffle', 'repeat',
   *         'seekto', 'seekforward', 'seekbackward', or any custom action name.
   */
  action: string;

  /**
   * Extra data for the action (e.g., { seekTime: 30.5 } for seekto)
   */
  extras?: Record<string, any>;
}


export enum MediaType {
  IMAGE = 'image',
  AUDIO = 'audio',
  VIDEO = 'video',
  DOCUMENT = 'document',
  ALL = 'all'
}

export enum PermissionState {
  GRANTED = 'granted',
  DENIED = 'denied',
  PROMPT = 'prompt',
  PROMPT_WITH_RATIONALE = 'prompt-with-rationale'
}

// ----------------------------------------------------------------
// File Picker � Save As & Open types
// ----------------------------------------------------------------

export interface SaveFileAsOptions {
  /**
   * Suggested file name for the save dialog
   */
  fileName: string;

  /**
   * MIME type for the file (default: 'application/octet-stream')
   */
  mimeType?: string;

  /**
   * File content as a base64-encoded string
   */
  data: string;
}

export interface SaveFileAsResult {
  /**
   * Whether the save was successful
   */
  success?: boolean;

  /**
   * Whether the user cancelled the picker
   */
  cancelled?: boolean;

  /**
   * URI of the saved file (platform-specific)
   */
  uri?: string;
}

export interface OpenFileOptions {
  /**
   * Array of MIME types to filter by (default: all types)
   */
  mimeTypes?: string[];
}

export interface OpenFileResult {
  /**
   * File content as a base64-encoded string
   */
  data?: string;

  /**
   * File name
   */
  name?: string;

  /**
   * MIME type of the file
   */
  mimeType?: string;

  /**
   * File size in bytes
   */
  size?: number;

  /**
   * Whether the user cancelled the picker
   */
  cancelled?: boolean;
}
