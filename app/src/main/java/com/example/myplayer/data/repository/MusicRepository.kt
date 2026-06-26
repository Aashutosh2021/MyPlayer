package com.example.myplayer.data.repository

import com.example.myplayer.data.local.dao.*
import com.example.myplayer.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao,
    private val favoriteDao: FavoriteDao,
    private val folderDao: FolderDao,
    private val recentHistoryDao: RecentHistoryDao,
    private val mediaScanner: MediaScanner
) {
    // Songs
    fun getAllSongs(): Flow<List<SongEntity>> = songDao.getAllSongs()
    fun getTrendingSongs(): Flow<List<SongEntity>> = songDao.getTrendingSongs()
    fun getRecentlyAddedSongs(): Flow<List<SongEntity>> = songDao.getRecentlyAddedSongs()
    fun getMostPlayedSongs(): Flow<List<SongEntity>> = songDao.getMostPlayedSongs()
    fun searchSongs(query: String): Flow<List<SongEntity>> = songDao.searchSongs(query)

    suspend fun incrementPlayCount(songId: String) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        songDao.incrementPlayCount(songId)
    }

    // Folders & Scanning
    fun getAllFolders(): Flow<List<FolderEntity>> = folderDao.getAllFolders()
    
    suspend fun addFolder(uri: String, name: String) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val folder = FolderEntity(uri, name)
        folderDao.addFolder(folder)
        mediaScanner.scanFolder(folder)
    }

    suspend fun removeFolder(folder: FolderEntity) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        folderDao.deleteFolder(folder)
        songDao.deleteSongsByFolder(folder.uri)
    }

    suspend fun rescanAllFolders() {
        mediaScanner.scanAllFolders()
    }

    // Playlists
    fun getAllPlaylists(): Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()
    fun getSongsInPlaylist(playlistId: Long): Flow<List<SongEntity>> = playlistDao.getSongsInPlaylist(playlistId)
    
    suspend fun createPlaylist(name: String): Long = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        playlistDao.insertPlaylist(PlaylistEntity(name = name))
    }
    
    suspend fun renamePlaylist(playlistId: Long, newName: String) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        playlistDao.renamePlaylist(playlistId, newName)
    }

    suspend fun deletePlaylist(playlist: PlaylistEntity) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        playlistDao.deletePlaylist(playlist)
    }

    suspend fun addSongToPlaylist(playlistId: Long, song: PlayableSong, position: Int) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (song is PlayableSong.Online) {
            val entity = SongEntity(
                id = song.id,
                title = song.title,
                artist = song.artist,
                album = "YouTube Music",
                duration = song.durationMs,
                path = "online://${song.id}",
                albumArt = song.thumbnailUrl,
                dateAdded = System.currentTimeMillis()
            )
            songDao.insertSongs(listOf(entity))
        } else if (song is PlayableSong.Downloaded) {
            val entity = SongEntity(
                id = song.id,
                title = song.title,
                artist = song.artist,
                album = "Downloads",
                duration = song.durationMs,
                path = song.entity.localPath,
                albumArt = song.thumbnailUrl,
                dateAdded = System.currentTimeMillis()
            )
            songDao.insertSongs(listOf(entity))
        }
        playlistDao.insertSongToPlaylist(PlaylistSongCrossReference(playlistId, song.id, position))
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: String) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
    }

    suspend fun reorderSongsInPlaylist(playlistId: Long, orderedSongIds: List<String>) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        orderedSongIds.forEachIndexed { index, songId ->
            playlistDao.updateSongPosition(playlistId, songId, index)
        }
    }

    // Favorites
    fun getFavoriteSongs(): Flow<List<SongEntity>> = favoriteDao.getFavoriteSongs()
    fun isFavorite(songId: String): Flow<Boolean> = favoriteDao.isFavorite(songId)

    suspend fun toggleFavorite(song: PlayableSong, isCurrentlyFavorite: Boolean) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val songId = song.id
        if (isCurrentlyFavorite) {
            favoriteDao.removeFavorite(songId)
        } else {
            if (song is PlayableSong.Online) {
                val entity = SongEntity(
                    id = song.id,
                    title = song.title,
                    artist = song.artist,
                    album = "YouTube Music",
                    duration = song.durationMs,
                    path = "online://${song.id}",
                    albumArt = song.thumbnailUrl,
                    dateAdded = System.currentTimeMillis()
                )
                songDao.insertSongs(listOf(entity))
            } else if (song is PlayableSong.Downloaded) {
                val entity = SongEntity(
                    id = song.id,
                    title = song.title,
                    artist = song.artist,
                    album = "Downloads",
                    duration = song.durationMs,
                    path = song.entity.localPath,
                    albumArt = song.thumbnailUrl,
                    dateAdded = System.currentTimeMillis()
                )
                songDao.insertSongs(listOf(entity))
            }
            favoriteDao.addFavorite(FavoriteEntity(songId))
        }
    }

    // Recent History
    fun getRecentHistory(): Flow<List<SongEntity>> = recentHistoryDao.getRecentHistory()

    suspend fun addRecentHistory(songId: String) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        recentHistoryDao.addRecent(RecentHistoryEntity(songId))
    }
}
