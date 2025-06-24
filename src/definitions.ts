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

  /**
   * Create document picker intent for Android 13+ using Storage Access Framework (SAF)
   * This method provides guidance for using SAF to access documents on Android 13+
   */
  createDocumentPickerIntent(): Promise<SAFIntentResponse>;

  /**
   * Get document metadata from Storage Access Framework URI (Android 13+)
   */
  getDocumentMetadataFromSAF(options: SAFUriOptions): Promise<DocumentMetadataResponse>;

  /**
   * Check if the device should use Storage Access Framework for documents
   * Returns true for Android 13+ devices
   */
  shouldUseSAFForDocuments(): Promise<SAFAvailabilityResponse>;
}

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

export interface SAFUriOptions {
  /**
   * URI string from Storage Access Framework document picker
   */
  uri: string;
}

export interface SAFIntentResponse {
  /**
   * Success status
   */
  success: boolean;

  /**
   * Guidance message for implementing SAF
   */
  message: string;

  /**
   * Intent action (ACTION_OPEN_DOCUMENT)
   */
  intentAction?: string;

  /**
   * Intent type
   */
  intentType?: string;

  /**
   * Implementation guidance
   */
  guidance: string;

  /**
   * Error message if failed
   */
  error?: string;
}

export interface DocumentMetadataResponse {
  /**
   * Document display name
   */
  displayName?: string;

  /**
   * File size in bytes
   */
  size?: number;

  /**
   * Document URI
   */
  uri: string;

  /**
   * Media type (always "document")
   */
  mediaType: string;

  /**
   * MIME type
   */
  mimeType?: string;

  /**
   * Error message if failed
   */
  error?: string;
}

export interface SAFAvailabilityResponse {
  /**
   * Whether Storage Access Framework should be used for documents
   */
  shouldUseSAF: boolean;

  /**
   * Android version (API level)
   */
  androidVersion: number;

  /**
   * Descriptive message
   */
  message: string;
}
