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
    requestPermissions(): Promise<PermissionStatus>;
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
     * Read external storage permission
     */
    readExternalStorage: PermissionState;
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
     * Write external storage permission
     */
    writeExternalStorage?: PermissionState;
}
export declare enum MediaType {
    IMAGE = "image",
    AUDIO = "audio",
    VIDEO = "video",
    DOCUMENT = "document",
    ALL = "all"
}
export declare enum PermissionState {
    GRANTED = "granted",
    DENIED = "denied",
    PROMPT = "prompt",
    PROMPT_WITH_RATIONALE = "prompt-with-rationale"
}
