package com.example.myplayer.data.repository

import com.example.myplayer.data.local.dao.FavoriteDao
import com.example.myplayer.data.local.dao.SongDao
import com.example.myplayer.data.local.entity.FavoriteEntity
import com.example.myplayer.data.local.entity.SongEntity
import com.example.myplayer.data.model.toSongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository owning User Favorites addition, deletion, and checks.
 * Standardizes Dispatchers.IO for SQLite transactions and structured error handling.
 * Consolidates mapping functions by invoking mapped extension helpers.
 *
 * Introduced in Phase R8 — Data Layer Consolidation.
 */
@Singleton
class FavoriteRepository @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val songDao: SongDao
) {
    fun getFavoriteSongs(): Flow<List<SongEntity>> = favoriteDao.getFavoriteSongs()
    
    fun isFavorite(songId: String): Flow<Boolean> = favoriteDao.isFavorite(songId)

    suspend fun toggleFavorite(song: PlayableSong, isCurrentlyFavorite: Boolean) = withContext(Dispatchers.IO) {
        try {
            val songId = song.id
            if (isCurrentlyFavorite) {
                favoriteDao.removeFavorite(songId)
            } else {
                when (song) {
                    is PlayableSong.Online -> {
                        val entity = song.toSongEntity()
                        songDao.insertSongs(listOf(entity))
                    }
                    is PlayableSong.Downloaded -> {
                        val entity = song.toSongEntity()
                        songDao.insertSongs(listOf(entity))
                    }
                    is PlayableSong.Local -> {
                        // Local songs are already in the DB, no insertion needed.
                    }
                }
                favoriteDao.addFavorite(FavoriteEntity(songId))
            }
        } catch (e: Exception) {
            android.util.Log.e("FavoriteRepository", "Failed to toggle favorite for ${song.id}", e)
        }
    }
}
