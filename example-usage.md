# Example Usage in Vue.js Music App

## Installation

```bash
npm install @capacitor/mediastore
npx cap sync android
```

## Vue Component Example

```vue
<template>
  <div class="music-app">
    <div class="header">
      <h1>My Music Player</h1>
      <button @click="requestPermissions" class="permission-btn">
        Grant Permissions
      </button>
    </div>

    <div class="stats">
      <p>Total Songs: {{ totalSongs }}</p>
      <p>SD Card Songs: {{ sdCardSongs }}</p>
    </div>

    <div class="controls">
      <button @click="loadAllSongs">Load All Songs</button>
      <button @click="loadAlbums">Load Albums</button>
    </div>

    <div class="song-list" v-if="songs.length > 0">
      <h2>Songs</h2>
      <div v-for="song in songs" :key="song.id" class="song-item">
        <div class="song-info">
          <h3>{{ song.title || song.displayName }}</h3>
          <p>{{ song.artist || 'Unknown Artist' }} - {{ song.album || 'Unknown Album' }}</p>
          <small>
            Duration: {{ formatDuration(song.duration) }}
            {{ song.isExternal ? '(SD Card)' : '(Internal)' }}
          </small>
        </div>
        <button @click="getMetadata(song.uri)">Get Details</button>
      </div>
    </div>

    <div class="albums" v-if="albums.length > 0">
      <h2>Albums</h2>
      <div v-for="album in albums" :key="album.id" class="album-item">
        <h3>{{ album.name }}</h3>
        <p>{{ album.artist }} ({{ album.trackCount }} tracks)</p>
      </div>
    </div>

    <div class="metadata" v-if="selectedMetadata">
      <h2>Detailed Metadata</h2>
      <pre>{{ JSON.stringify(selectedMetadata, null, 2) }}</pre>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { CapacitorMediaStore } from '@capacitor/mediastore';

const songs = ref([]);
const albums = ref([]);
const totalSongs = ref(0);
const sdCardSongs = ref(0);
const selectedMetadata = ref(null);

const requestPermissions = async () => {
  try {
    const result = await CapacitorMediaStore.requestPermissions();
    console.log('Permissions granted:', result);
  } catch (error) {
    console.error('Permission error:', error);
  }
};

const loadAllSongs = async () => {
  try {
    // Load all audio files including from SD card
    const result = await CapacitorMediaStore.getMediasByType({
      mediaType: 'audio',
      sortBy: 'TITLE',
      sortOrder: 'ASC',
      includeExternal: true, // Critical for SD card access
      limit: 1000 // Adjust based on your needs
    });

    songs.value = result.media;
    totalSongs.value = result.totalCount;
    
    // Count SD card songs
    sdCardSongs.value = result.media.filter(song => song.isExternal).length;
    
    console.log(`Loaded ${result.media.length} songs`);
    console.log(`${sdCardSongs.value} songs from SD card`);
  } catch (error) {
    console.error('Error loading songs:', error);
  }
};

const loadAlbums = async () => {
  try {
    const result = await CapacitorMediaStore.getAlbums();
    albums.value = result.albums;
    console.log(`Loaded ${result.albums.length} albums`);
  } catch (error) {
    console.error('Error loading albums:', error);
  }
};

const getMetadata = async (uri: string) => {
  try {
    const result = await CapacitorMediaStore.getMediaMetadata({
      filePath: uri
    });
    selectedMetadata.value = result.media;
    console.log('Detailed metadata:', result.media);
  } catch (error) {
    console.error('Error getting metadata:', error);
  }
};

const formatDuration = (duration: number | undefined) => {
  if (!duration) return 'Unknown';
  const minutes = Math.floor(duration / 60000);
  const seconds = Math.floor((duration % 60000) / 1000);
  return `${minutes}:${seconds.toString().padStart(2, '0')}`;
};

onMounted(() => {
  // Check permissions on app start
  CapacitorMediaStore.checkPermissions().then(permissions => {
    console.log('Current permissions:', permissions);
  });
});
</script>

<style scoped>
.music-app {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.permission-btn {
  background: #007cba;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 5px;
  cursor: pointer;
}

.stats {
  background: #f5f5f5;
  padding: 15px;
  border-radius: 5px;
  margin-bottom: 20px;
}

.controls {
  margin-bottom: 20px;
}

.controls button {
  margin-right: 10px;
  padding: 10px 15px;
  background: #28a745;
  color: white;
  border: none;
  border-radius: 5px;
  cursor: pointer;
}

.song-item, .album-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  border-bottom: 1px solid #eee;
}

.song-info h3 {
  margin: 0 0 5px 0;
}

.song-info p {
  margin: 0 0 5px 0;
  color: #666;
}

.song-info small {
  color: #999;
}

.metadata {
  margin-top: 20px;
  background: #f8f9fa;
  padding: 15px;
  border-radius: 5px;
}

.metadata pre {
  background: white;
  padding: 10px;
  border-radius: 3px;
  overflow-x: auto;
}
</style>
```

## Composable for Music Service

```typescript
// composables/useMusicService.ts
import { ref } from 'vue';
import { CapacitorMediaStore, MediaFile, Album } from '@capacitor/mediastore';

export const useMusicService = () => {
  const songs = ref<MediaFile[]>([]);
  const albums = ref<Album[]>([]);
  const isLoading = ref(false);
  const hasPermissions = ref(false);

  const checkPermissions = async () => {
    try {
      const result = await CapacitorMediaStore.checkPermissions();
      hasPermissions.value = result.readMediaAudio === 'granted' || 
                           result.readExternalStorage === 'granted';
      return hasPermissions.value;
    } catch (error) {
      console.error('Error checking permissions:', error);
      return false;
    }
  };

  const requestPermissions = async () => {
    try {
      const result = await CapacitorMediaStore.requestPermissions();
      hasPermissions.value = result.readMediaAudio === 'granted' || 
                           result.readExternalStorage === 'granted';
      return hasPermissions.value;
    } catch (error) {
      console.error('Error requesting permissions:', error);
      return false;
    }
  };

  const loadSongs = async (options = {}) => {
    if (!hasPermissions.value) {
      throw new Error('No media permissions granted');
    }

    isLoading.value = true;
    try {
      const result = await CapacitorMediaStore.getMediasByType({
        mediaType: 'audio',
        sortBy: 'TITLE',
        sortOrder: 'ASC',
        includeExternal: true,
        ...options
      });

      songs.value = result.media;
      return result;
    } catch (error) {
      console.error('Error loading songs:', error);
      throw error;
    } finally {
      isLoading.value = false;
    }
  };

  const loadAlbums = async () => {
    if (!hasPermissions.value) {
      throw new Error('No media permissions granted');
    }

    try {
      const result = await CapacitorMediaStore.getAlbums();
      albums.value = result.albums;
      return result;
    } catch (error) {
      console.error('Error loading albums:', error);
      throw error;
    }
  };

  const searchSongs = async (query: string) => {
    if (!hasPermissions.value) return [];

    try {
      const result = await CapacitorMediaStore.getMediasByType({
        mediaType: 'audio',
        includeExternal: true
      });

      return result.media.filter(song => 
        song.title?.toLowerCase().includes(query.toLowerCase()) ||
        song.artist?.toLowerCase().includes(query.toLowerCase()) ||
        song.album?.toLowerCase().includes(query.toLowerCase())
      );
    } catch (error) {
      console.error('Error searching songs:', error);
      return [];
    }
  };

  const getSongsByAlbum = async (albumName: string) => {
    if (!hasPermissions.value) return [];

    try {
      const result = await CapacitorMediaStore.getMediasByType({
        mediaType: 'audio',
        albumName,
        includeExternal: true
      });

      return result.media;
    } catch (error) {
      console.error('Error getting songs by album:', error);
      return [];
    }
  };

  return {
    songs,
    albums,
    isLoading,
    hasPermissions,
    checkPermissions,
    requestPermissions,
    loadSongs,
    loadAlbums,
    searchSongs,
    getSongsByAlbum
  };
};
```

## Key Benefits for Your Music App

1. **SD Card Access**: Unlike Capacitor Filesystem, this plugin can access songs stored on external SD cards
2. **Rich Metadata**: Get complete song information including artist, album, duration, bitrate without additional processing
3. **Performance**: Uses Android's indexed MediaStore database for fast queries
4. **Proper Permissions**: Handles Android 13+ granular media permissions automatically
5. **Type Safety**: Full TypeScript support with detailed interface definitions

## Common Use Cases

```typescript
// Get all songs with pagination
const getPagedSongs = async (page: number, limit = 50) => {
  return await CapacitorMediaStore.getMediasByType({
    mediaType: 'audio',
    offset: page * limit,
    limit,
    includeExternal: true
  });
};

// Get songs by artist
const getSongsByArtist = async (artistName: string) => {
  return await CapacitorMediaStore.getMediasByType({
    mediaType: 'audio',
    artistName,
    includeExternal: true
  });
};

// Get recently added songs
const getRecentSongs = async () => {
  return await CapacitorMediaStore.getMediasByType({
    mediaType: 'audio',
    sortBy: 'DATE_ADDED',
    sortOrder: 'DESC',
    limit: 50,
    includeExternal: true
  });
};
```