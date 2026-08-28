import { WebPlugin } from '@capacitor/core';
import type { SaveFileAsOptions, SaveFileAsResult, OpenFileOptions, OpenFileResult, CapacitorMediaStorePlugin, MediaQueryOptions, MediaResponse, MediaTypeOptions, AlbumResponse, SaveMediaOptions, SaveMediaResponse, MediaMetadataOptions, MediaMetadataResponse, PermissionStatus, RequestPermissionsOptions, WatchOptions, MediaSessionOptions, PlaybackStateOptions, PositionStateOptions, MediaSessionButtonOptions } from './definitions';
export declare class CapacitorMediaStoreWeb extends WebPlugin implements CapacitorMediaStorePlugin {
    getMedias(options?: MediaQueryOptions): Promise<MediaResponse>;
    getMediasByType(options: MediaTypeOptions): Promise<MediaResponse>;
    getAlbums(): Promise<AlbumResponse>;
    saveMedia(options: SaveMediaOptions): Promise<SaveMediaResponse>;
    getMediaMetadata(options: MediaMetadataOptions): Promise<MediaMetadataResponse>;
    checkPermissions(): Promise<PermissionStatus>;
    requestPermissions(options?: RequestPermissionsOptions): Promise<PermissionStatus>;
    startWatching(options?: WatchOptions): Promise<void>;
    stopWatching(): Promise<void>;
    setMediaSession(options: MediaSessionOptions): Promise<void>;
    updatePlaybackState(options: PlaybackStateOptions): Promise<void>;
    updatePositionState(options: PositionStateOptions): Promise<void>;
    updateMediaSessionButtons(options: MediaSessionButtonOptions): Promise<void>;
    destroyMediaSession(): Promise<void>;
    saveFileAs(options: SaveFileAsOptions): Promise<SaveFileAsResult>;
    openFile(options?: OpenFileOptions): Promise<OpenFileResult>;
}
