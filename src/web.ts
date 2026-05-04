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
  WatchOptions,
  MediaSessionOptions,
  PlaybackStateOptions,
  PositionStateOptions,
  MediaSessionButtonOptions
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

  // ──────────────────────────────────────────────
  // File Change Watcher (web stubs)
  // ──────────────────────────────────────────────

  async startWatching(options?: WatchOptions): Promise<void> {
    console.warn('CapacitorMediaStore.startWatching() is not supported on web platform');
    throw this.unimplemented('Not implemented on web.');
  }

  async stopWatching(): Promise<void> {
    console.warn('CapacitorMediaStore.stopWatching() is not supported on web platform');
    throw this.unimplemented('Not implemented on web.');
  }

  // ──────────────────────────────────────────────
  // Native Media Session (web fallback using Web MediaSession API)
  // ──────────────────────────────────────────────

  async setMediaSession(options: MediaSessionOptions): Promise<void> {
    if ('mediaSession' in navigator) {
      const artwork: MediaImage[] = [];
      if (options.artworkUrl) {
        artwork.push({ src: options.artworkUrl, sizes: '512x512', type: 'image/png' });
      }
      navigator.mediaSession.metadata = new MediaMetadata({
        title: options.title,
        artist: options.artist,
        album: options.album || '',
        artwork: artwork,
      });
    } else {
      console.warn('CapacitorMediaStore.setMediaSession() - Web MediaSession API not available');
    }
  }

  async updatePlaybackState(options: PlaybackStateOptions): Promise<void> {
    if ('mediaSession' in navigator) {
      navigator.mediaSession.playbackState = options.state === 'playing' ? 'playing' : options.state === 'paused' ? 'paused' : 'none';
    }
  }

  async updatePositionState(options: PositionStateOptions): Promise<void> {
    if ('mediaSession' in navigator && navigator.mediaSession.setPositionState) {
      try {
        navigator.mediaSession.setPositionState({
          duration: options.duration,
          playbackRate: options.playbackRate || 1.0,
          position: options.position,
        });
      } catch (e) {
        console.warn('CapacitorMediaStore.updatePositionState() error:', e);
      }
    }
  }

  async updateMediaSessionButtons(options: MediaSessionButtonOptions): Promise<void> {
    console.warn('CapacitorMediaStore.updateMediaSessionButtons() - Custom buttons not supported on web');
  }

  async destroyMediaSession(): Promise<void> {
    if ('mediaSession' in navigator) {
      navigator.mediaSession.metadata = null;
      navigator.mediaSession.playbackState = 'none';
    }
  }
}
