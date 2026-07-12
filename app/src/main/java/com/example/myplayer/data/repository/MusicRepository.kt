package com.example.myplayer.data.repository

import com.example.myplayer.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Facade Repository adapting local storage access to domain-specific repositories.
 * Preserves the exact public signature for UI, ViewModels, and MusicController
 * to avoid refactoring code churn.
 *
 * Implements the Strangler Pattern during Phase R8 consolidation.
 */
@Singleton
class MusicRepository @Inject constructor(
    private val songRepository: SongRepository,
    private val playlistRepository: PlaylistRepository,
    private val favoriteRepository: FavoriteRepository,
    private val recentHistoryRepository: RecentHistoryRepository
) {
    // Songs & Scanning
    fun getAllSongs(): Flow<List<SongEntity>> = songRepository.getAllSongs()
    fun getTrendingSongs(): Flow<List<SongEntity>> = songRepository.getTrendingSongs()
    fun getRecentlyAddedSongs(): Flow<List<SongEntity>> = songRepository.getRecentlyAddedSongs()
    fun getMostPlayedSongs(): Flow<List<SongEntity>> = songRepository.getMostPlayedSongs()
    fun searchSongs(query: String): Flow<List<SongEntity>> = songRepository.searchSongs(query)

    suspend fun incrementPlayCount(songId: String) {
        songRepository.incrementPlayCount(songId)
    }

    // Folders & Scanning
    fun getAllFolders(): Flow<List<FolderEntity>> = songRepository.getAllFolders()
    
    suspend fun addFolder(uri: String, name: String) {
        songRepository.addFolder(uri, name)
    }

    suspend fun removeFolder(folder: FolderEntity) {
        songRepository.removeFolder(folder)
    }

    suspend fun rescanAllFolders() {
        songRepository.rescanAllFolders()
    }

    // Playlists
    fun getAllPlaylists(): Flow<List<PlaylistEntity>> = playlistRepository.getAllPlaylists()
    fun getSongsInPlaylist(playlistId: Long): Flow<List<SongEntity>> = playlistRepository.getSongsInPlaylist(playlistId)
    fun getSongsInPlaylistAsPlayable(playlistId: Long): Flow<List<PlayableSong>> = playlistRepository.getSongsInPlaylistAsPlayable(playlistId)
    
    suspend fun createPlaylist(name: String): Long = playlistRepository.createPlaylist(name)
    
    suspend fun renamePlaylist(playlistId: Long, newName: String) {
        playlistRepository.renamePlaylist(playlistId, newName)
    }

    suspend fun deletePlaylist(playlist: PlaylistEntity) {
        playlistRepository.deletePlaylist(playlist)
    }

    suspend fun addSongToPlaylist(playlistId: Long, song: PlayableSong, position: Int) {
        playlistRepository.addSongToPlaylist(playlistId, song, position)
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: String) {
        playlistRepository.removeSongFromPlaylist(playlistId, songId)
    }

    suspend fun reorderSongsInPlaylist(playlistId: Long, orderedSongIds: List<String>) {
        playlistRepository.reorderSongsInPlaylist(playlistId, orderedSongIds)
    }

    // Favorites
    fun getFavoriteSongs(): Flow<List<SongEntity>> = favoriteRepository.getFavoriteSongs()
    fun isFavorite(songId: String): Flow<Boolean> = favoriteRepository.isFavorite(songId)

    suspend fun toggleFavorite(song: PlayableSong, isCurrentlyFavorite: Boolean) {
        favoriteRepository.toggleFavorite(song, isCurrentlyFavorite)
    }

    // Recent History
    fun getRecentHistory(): Flow<List<SongEntity>> = recentHistoryRepository.getRecentHistory()

    suspend fun addRecentHistory(songId: String) {
        recentHistoryRepository.addRecentHistory(songId)
    }
}
