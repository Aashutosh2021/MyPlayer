package com.example.myplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myplayer.data.local.entity.FavoriteEntity
import com.example.myplayer.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("""
        SELECT songs.* FROM songs 
        INNER JOIN favorites ON songs.id = favorites.songId 
        ORDER BY favorites.addedAt DESC
    """)
    fun getFavoriteSongs(): Flow<List<SongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addFavorite(favorite: FavoriteEntity): Unit

    @Query("DELETE FROM favorites WHERE songId = :songId")
    fun removeFavorite(songId: String): Unit

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE songId = :songId)")
    fun isFavorite(songId: String): Flow<Boolean>

    @Query("SELECT * FROM favorites")
    fun getAllFavoritesSync(): List<FavoriteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFavorites(favorites: List<FavoriteEntity>)
}
