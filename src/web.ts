import { WebPlugin } from '@capacitor/core';

import type { 
  CapacitorMediaStorePlugin, 
  MediaQueryOptions, 
  MediaResponse, 
  MediaTypeOptions, 
  AlbumResponse, 
  SaveMediaOptions, 
  SaveMediaResponse, 
  MediaMetadataOptions, 
  MediaMetadataResponse, 
  PermissionStatus
} from './definitions';

import { PermissionState } from './definitions';

export class CapacitorMediaStoreWeb extends WebPlugin implements CapacitorMediaStorePlugin {
  async getMedias(options?: MediaQueryOptions): Promise<MediaResponse> {
    console.warn('CapacitorMediaStore.getMedias() is not supported on web platform');
    throw this.unimplemented('Not implemented on web.');
  }

  async getMediasByType(options: MediaTypeOptions): Promise<MediaResponse> {
    console.warn('CapacitorMediaStore.getMediasByType() is not supported on web platform');
    throw this.unimplemented('Not implemented on web.');
  }

  async getAlbums(): Promise<AlbumResponse> {
    console.warn('CapacitorMediaStore.getAlbums() is not supported on web platform');
    throw this.unimplemented('Not implemented on web.');
  }

  async saveMedia(options: SaveMediaOptions): Promise<SaveMediaResponse> {
    console.warn('CapacitorMediaStore.saveMedia() is not supported on web platform');
    throw this.unimplemented('Not implemented on web.');
  }

  async getMediaMetadata(options: MediaMetadataOptions): Promise<MediaMetadataResponse> {
    console.warn('CapacitorMediaStore.getMediaMetadata() is not supported on web platform');
    throw this.unimplemented('Not implemented on web.');
  }

  async checkPermissions(): Promise<PermissionStatus> {
    console.warn('CapacitorMediaStore.checkPermissions() is not supported on web platform');
    return {
      readExternalStorage: PermissionState.GRANTED,
      readMediaImages: PermissionState.GRANTED,
      readMediaAudio: PermissionState.GRANTED,
      readMediaVideo: PermissionState.GRANTED,
      writeExternalStorage: PermissionState.GRANTED
    };
  }

  async requestPermissions(): Promise<PermissionStatus> {
    console.warn('CapacitorMediaStore.requestPermissions() is not supported on web platform');
    return {
      readExternalStorage: PermissionState.GRANTED,
      readMediaImages: PermissionState.GRANTED,
      readMediaAudio: PermissionState.GRANTED,
      readMediaVideo: PermissionState.GRANTED,
      writeExternalStorage: PermissionState.GRANTED
    };
  }
}
