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
  PermissionStatus,
  RequestPermissionsOptions,
  SAFIntentResponse,
  SAFUriOptions,
  DocumentMetadataResponse,
  SAFAvailabilityResponse
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

  async requestPermissions(options?: RequestPermissionsOptions): Promise<PermissionStatus> {
    console.warn('CapacitorMediaStore.requestPermissions() is not supported on web platform');
    return {
      readExternalStorage: PermissionState.GRANTED,
      readMediaImages: PermissionState.GRANTED,
      readMediaAudio: PermissionState.GRANTED,
      readMediaVideo: PermissionState.GRANTED,
      writeExternalStorage: PermissionState.GRANTED
    };
  }

  async createDocumentPickerIntent(): Promise<SAFIntentResponse> {
    console.warn('CapacitorMediaStore.createDocumentPickerIntent() is not supported on web platform');
    throw this.unimplemented('Not implemented on web. Use HTML file input for document selection.');
  }

  async getDocumentMetadataFromSAF(options: SAFUriOptions): Promise<DocumentMetadataResponse> {
    console.warn('CapacitorMediaStore.getDocumentMetadataFromSAF() is not supported on web platform');
    throw this.unimplemented('Not implemented on web.');
  }

  async shouldUseSAFForDocuments(): Promise<SAFAvailabilityResponse> {
    console.warn('CapacitorMediaStore.shouldUseSAFForDocuments() is not supported on web platform');
    return {
      shouldUseSAF: false,
      androidVersion: 0,
      message: 'Storage Access Framework is not available on web platform. Use HTML file input for document selection.'
    };
  }
}
