# JavaScript Usage Examples

## Installation for JavaScript Projects

```bash
npm install @capacitor/mediastore
npx cap sync android
```

## Basic JavaScript Usage

```javascript
// Import the plugin
import { CapacitorMediaStore } from '@capacitor/mediastore';

// Request permissions first
async function setupPermissions() {
  try {
    const permissions = await CapacitorMediaStore.requestPermissions();
    console.log('Permissions granted:', permissions);
    return permissions;
  } catch (error) {
    console.error('Permission error:', error);
    return null;
  }
}

// Get all audio files including from SD card
async function loadAllSongs() {
  try {
    const result = await CapacitorMediaStore.getMediasByType({
      mediaType: 'audio',
      sortBy: 'TITLE',
      sortOrder: 'ASC',
      includeExternal: true, // Critical for SD card access
      limit: 1000
    });

    console.log('Total songs found:', result.totalCount);
    console.log('Songs loaded:', result.media.length);
    
    // Filter SD card songs
    const sdCardSongs = result.media.filter(song => song.isExternal);
    console.log('SD card songs:', sdCardSongs.length);
    
    return result.media;
  } catch (error) {
    console.error('Error loading songs:', error);
    return [];
  }
}

// Get all music albums
async function loadAlbums() {
  try {
    const result = await CapacitorMediaStore.getAlbums();
    console.log('Albums found:', result.albums.length);
    return result.albums;
  } catch (error) {
    console.error('Error loading albums:', error);
    return [];
  }
}

// Get detailed metadata for a specific song
async function getSongMetadata(songUri) {
  try {
    const result = await CapacitorMediaStore.getMediaMetadata({
      filePath: songUri
    });
    
    const metadata = result.media;
    console.log('Song metadata:', {
      title: metadata.title,
      artist: metadata.artist,
      album: metadata.album,
      duration: metadata.duration,
      bitrate: metadata.bitrate,
      genre: metadata.genre
    });
    
    return metadata;
  } catch (error) {
    console.error('Error getting metadata:', error);
    return null;
  }
}

// Search songs by artist
async function searchByArtist(artistName) {
  try {
    const result = await CapacitorMediaStore.getMediasByType({
      mediaType: 'audio',
      artistName: artistName,
      includeExternal: true
    });
    
    return result.media;
  } catch (error) {
    console.error('Error searching by artist:', error);
    return [];
  }
}

// Get songs from specific album
async function getSongsFromAlbum(albumName) {
  try {
    const result = await CapacitorMediaStore.getMediasByType({
      mediaType: 'audio',
      albumName: albumName,
      includeExternal: true
    });
    
    return result.media;
  } catch (error) {
    console.error('Error getting album songs:', error);
    return [];
  }
}
```

## Vanilla JavaScript Example (No Framework)

```html
<!DOCTYPE html>
<html>
<head>
    <title>Music Player</title>
    <script type="module">
        import { CapacitorMediaStore } from '@capacitor/mediastore';
        
        let allSongs = [];
        let currentPage = 0;
        const songsPerPage = 50;
        
        // Initialize the app
        async function initApp() {
            const permissionsBtn = document.getElementById('permissions-btn');
            const loadSongsBtn = document.getElementById('load-songs-btn');
            const songsList = document.getElementById('songs-list');
            const statsDiv = document.getElementById('stats');
            
            permissionsBtn.addEventListener('click', async () => {
                const permissions = await CapacitorMediaStore.requestPermissions();
                console.log('Permissions:', permissions);
                
                if (permissions.readExternalStorage === 'granted' || 
                    permissions.readMediaAudio === 'granted') {
                    permissionsBtn.textContent = 'Permissions Granted ✓';
                    permissionsBtn.disabled = true;
                    loadSongsBtn.disabled = false;
                }
            });
            
            loadSongsBtn.addEventListener('click', async () => {
                await loadSongs();
            });
            
            // Check permissions on load
            const currentPermissions = await CapacitorMediaStore.checkPermissions();
            if (currentPermissions.readExternalStorage === 'granted' || 
                currentPermissions.readMediaAudio === 'granted') {
                permissionsBtn.textContent = 'Permissions Granted ✓';
                permissionsBtn.disabled = true;
                loadSongsBtn.disabled = false;
            }
        }
        
        async function loadSongs() {
            try {
                const result = await CapacitorMediaStore.getMediasByType({
                    mediaType: 'audio',
                    sortBy: 'TITLE',
                    sortOrder: 'ASC',
                    includeExternal: true,
                    offset: currentPage * songsPerPage,
                    limit: songsPerPage
                });
                
                allSongs = allSongs.concat(result.media);
                displaySongs(result.media);
                updateStats(result);
                
                if (result.hasMore) {
                    showLoadMoreButton();
                }
                
            } catch (error) {
                console.error('Error loading songs:', error);
                alert('Error loading songs: ' + error.message);
            }
        }
        
        function displaySongs(songs) {
            const songsList = document.getElementById('songs-list');
            
            songs.forEach(song => {
                const songElement = document.createElement('div');
                songElement.className = 'song-item';
                songElement.innerHTML = `
                    <div class="song-info">
                        <h3>${song.title || song.displayName}</h3>
                        <p>${song.artist || 'Unknown Artist'} - ${song.album || 'Unknown Album'}</p>
                        <small>
                            Duration: ${formatDuration(song.duration)}
                            ${song.isExternal ? '(SD Card)' : '(Internal)'}
                        </small>
                    </div>
                    <button onclick="showMetadata('${song.uri}')">Details</button>
                `;
                songsList.appendChild(songElement);
            });
        }
        
        function updateStats(result) {
            const statsDiv = document.getElementById('stats');
            const sdCardCount = allSongs.filter(song => song.isExternal).length;
            const internalCount = allSongs.length - sdCardCount;
            
            statsDiv.innerHTML = `
                <p>Total Songs: ${result.totalCount}</p>
                <p>Loaded: ${allSongs.length}</p>
                <p>Internal Storage: ${internalCount}</p>
                <p>SD Card: ${sdCardCount}</p>
            `;
        }
        
        function formatDuration(duration) {
            if (!duration) return 'Unknown';
            const minutes = Math.floor(duration / 60000);
            const seconds = Math.floor((duration % 60000) / 1000);
            return `${minutes}:${seconds.toString().padStart(2, '0')}`;
        }
        
        window.showMetadata = async function(uri) {
            try {
                const result = await CapacitorMediaStore.getMediaMetadata({
                    filePath: uri
                });
                
                const metadata = result.media;
                alert(`
Title: ${metadata.title || 'Unknown'}
Artist: ${metadata.artist || 'Unknown'}
Album: ${metadata.album || 'Unknown'}
Duration: ${formatDuration(metadata.duration)}
Bitrate: ${metadata.bitrate || 'Unknown'} kbps
Genre: ${metadata.genre || 'Unknown'}
Year: ${metadata.year || 'Unknown'}
                `);
            } catch (error) {
                alert('Error getting metadata: ' + error.message);
            }
        };
        
        // Initialize when page loads
        document.addEventListener('DOMContentLoaded', initApp);
    </script>
    
    <style>
        body { font-family: Arial, sans-serif; padding: 20px; }
        .controls { margin-bottom: 20px; }
        .controls button { margin-right: 10px; padding: 10px 15px; }
        #stats { background: #f5f5f5; padding: 15px; margin-bottom: 20px; }
        .song-item { 
            display: flex; 
            justify-content: space-between; 
            align-items: center; 
            padding: 10px; 
            border-bottom: 1px solid #eee; 
        }
        .song-info h3 { margin: 0 0 5px 0; }
        .song-info p { margin: 0 0 5px 0; color: #666; }
        .song-info small { color: #999; }
    </style>
</head>
<body>
    <h1>Music Player - JavaScript Example</h1>
    
    <div class="controls">
        <button id="permissions-btn">Grant Permissions</button>
        <button id="load-songs-btn" disabled>Load Songs</button>
    </div>
    
    <div id="stats"></div>
    <div id="songs-list"></div>
</body>
</html>
```

## React Example

```jsx
import React, { useState, useEffect } from 'react';
import { CapacitorMediaStore } from '@capacitor/mediastore';

function MusicPlayer() {
  const [songs, setSongs] = useState([]);
  const [albums, setAlbums] = useState([]);
  const [hasPermissions, setHasPermissions] = useState(false);
  const [loading, setLoading] = useState(false);
  const [stats, setStats] = useState({ total: 0, sdCard: 0, internal: 0 });

  useEffect(() => {
    checkPermissions();
  }, []);

  const checkPermissions = async () => {
    try {
      const permissions = await CapacitorMediaStore.checkPermissions();
      const granted = permissions.readExternalStorage === 'granted' || 
                     permissions.readMediaAudio === 'granted';
      setHasPermissions(granted);
    } catch (error) {
      console.error('Error checking permissions:', error);
    }
  };

  const requestPermissions = async () => {
    try {
      const permissions = await CapacitorMediaStore.requestPermissions();
      const granted = permissions.readExternalStorage === 'granted' || 
                     permissions.readMediaAudio === 'granted';
      setHasPermissions(granted);
      return granted;
    } catch (error) {
      console.error('Error requesting permissions:', error);
      return false;
    }
  };

  const loadSongs = async () => {
    if (!hasPermissions) {
      const granted = await requestPermissions();
      if (!granted) return;
    }

    setLoading(true);
    try {
      const result = await CapacitorMediaStore.getMediasByType({
        mediaType: 'audio',
        sortBy: 'TITLE',
        sortOrder: 'ASC',
        includeExternal: true,
        limit: 1000
      });

      setSongs(result.media);
      
      const sdCardCount = result.media.filter(song => song.isExternal).length;
      setStats({
        total: result.totalCount,
        sdCard: sdCardCount,
        internal: result.media.length - sdCardCount
      });
    } catch (error) {
      console.error('Error loading songs:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadAlbums = async () => {
    if (!hasPermissions) return;

    try {
      const result = await CapacitorMediaStore.getAlbums();
      setAlbums(result.albums);
    } catch (error) {
      console.error('Error loading albums:', error);
    }
  };

  const formatDuration = (duration) => {
    if (!duration) return 'Unknown';
    const minutes = Math.floor(duration / 60000);
    const seconds = Math.floor((duration % 60000) / 1000);
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  };

  return (
    <div style={{ padding: '20px', maxWidth: '800px', margin: '0 auto' }}>
      <h1>Music Player - React Example</h1>
      
      <div style={{ marginBottom: '20px' }}>
        {!hasPermissions ? (
          <button onClick={requestPermissions}>Grant Permissions</button>
        ) : (
          <span>✓ Permissions Granted</span>
        )}
        <button onClick={loadSongs} disabled={!hasPermissions || loading}>
          {loading ? 'Loading...' : 'Load Songs'}
        </button>
        <button onClick={loadAlbums} disabled={!hasPermissions}>
          Load Albums
        </button>
      </div>

      <div style={{ background: '#f5f5f5', padding: '15px', marginBottom: '20px' }}>
        <p>Total Songs: {stats.total}</p>
        <p>Internal Storage: {stats.internal}</p>
        <p>SD Card: {stats.sdCard}</p>
      </div>

      <div>
        <h2>Songs ({songs.length})</h2>
        {songs.map(song => (
          <div key={song.id} style={{ 
            display: 'flex', 
            justifyContent: 'space-between', 
            alignItems: 'center',
            padding: '10px', 
            borderBottom: '1px solid #eee' 
          }}>
            <div>
              <h3 style={{ margin: '0 0 5px 0' }}>
                {song.title || song.displayName}
              </h3>
              <p style={{ margin: '0 0 5px 0', color: '#666' }}>
                {song.artist || 'Unknown Artist'} - {song.album || 'Unknown Album'}
              </p>
              <small style={{ color: '#999' }}>
                Duration: {formatDuration(song.duration)}
                {song.isExternal ? ' (SD Card)' : ' (Internal)'}
              </small>
            </div>
            <button onClick={() => {
              // Handle play or show details
              console.log('Song selected:', song);
            }}>
              Play
            </button>
          </div>
        ))}
      </div>

      {albums.length > 0 && (
        <div>
          <h2>Albums ({albums.length})</h2>
          {albums.map(album => (
            <div key={album.id} style={{ 
              padding: '10px', 
              borderBottom: '1px solid #eee' 
            }}>
              <h3>{album.name}</h3>
              <p>{album.artist} ({album.trackCount} tracks)</p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default MusicPlayer;
```

## Node.js Build Script Example

```javascript
// build-music-library.js
const { CapacitorMediaStore } = require('@capacitor/mediastore');

async function buildMusicLibrary() {
  try {
    console.log('Building music library...');
    
    // Request permissions
    await CapacitorMediaStore.requestPermissions();
    
    // Get all audio files
    const songs = await CapacitorMediaStore.getMediasByType({
      mediaType: 'audio',
      includeExternal: true,
      sortBy: 'ARTIST'
    });
    
    // Get all albums
    const albums = await CapacitorMediaStore.getAlbums();
    
    // Process and save to JSON
    const musicLibrary = {
      songs: songs.media.map(song => ({
        id: song.id,
        title: song.title || song.displayName,
        artist: song.artist || 'Unknown Artist',
        album: song.album || 'Unknown Album',
        duration: song.duration,
        uri: song.uri,
        isExternal: song.isExternal
      })),
      albums: albums.albums,
      stats: {
        totalSongs: songs.totalCount,
        totalAlbums: albums.totalCount,
        sdCardSongs: songs.media.filter(s => s.isExternal).length
      }
    };
    
    console.log('Music library built:', musicLibrary.stats);
    return musicLibrary;
    
  } catch (error) {
    console.error('Error building music library:', error);
  }
}

module.exports = { buildMusicLibrary };
```