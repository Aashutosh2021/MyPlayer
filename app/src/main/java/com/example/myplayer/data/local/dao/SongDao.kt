package com.example.myplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myplayer.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY id ASC LIMIT 15")
    fun getTrendingSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :id")
    fun getSongById(id: String): SongEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertSongs(songs: List<SongEntity>): Unit

    @Query("DELETE FROM songs WHERE path LIKE :folderUri || '%'")
    fun deleteSongsByFolder(folderUri: String): Unit

    @Query("SELECT * FROM songs ORDER BY dateAdded DESC LIMIT 50")
    fun getRecentlyAddedSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY playCount DESC LIMIT 50")
    fun getMostPlayedSongs(): Flow<List<SongEntity>>

    @Query("UPDATE songs SET playCount = playCount + 1 WHERE id = :id")
    fun incrementPlayCount(id: String): Unit

    @Query("UPDATE songs SET path = :path WHERE id = :id")
    fun updateSongPath(id: String, path: String): Unit
    
    @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%'")
    fun searchSongs(query: String): Flow<List<SongEntity>>
}
