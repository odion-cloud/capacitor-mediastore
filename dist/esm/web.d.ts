import { WebPlugin } from '@capacitor/core';
import type { CapacitorMediaStorePlugin, MediaQueryOptions, MediaResponse, MediaTypeOptions, AlbumResponse, SaveMediaOptions, SaveMediaResponse, MediaMetadataOptions, MediaMetadataResponse, PermissionStatus, RequestPermissionsOptions } from './definitions';
export declare class CapacitorMediaStoreWeb extends WebPlugin implements CapacitorMediaStorePlugin {
    getMedias(options?: MediaQueryOptions): Promise<MediaResponse>;
    getMediasByType(options: MediaTypeOptions): Promise<MediaResponse>;
    getAlbums(): Promise<AlbumResponse>;
    saveMedia(options: SaveMediaOptions): Promise<SaveMediaResponse>;
    getMediaMetadata(options: MediaMetadataOptions): Promise<MediaMetadataResponse>;
    checkPermissions(): Promise<PermissionStatus>;
    requestPermissions(options?: RequestPermissionsOptions): Promise<PermissionStatus>;
}
