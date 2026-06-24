package com.example.myplayer.data.local.dao

import androidx.room.*
import com.example.myplayer.data.local.entity.DownloadedSongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedSongDao {

    @Query("SELECT * FROM downloaded_songs ORDER BY downloadedAt DESC")
    fun getAllDownloads(): Flow<List<DownloadedSongEntity>>

    @Query("SELECT * FROM downloaded_songs WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DownloadedSongEntity?

    @Query("SELECT COUNT(*) > 0 FROM downloaded_songs WHERE id = :id")
    suspend fun existsById(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: DownloadedSongEntity)

    @Query("DELETE FROM downloaded_songs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM downloaded_songs WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%'")
    fun searchDownloads(query: String): Flow<List<DownloadedSongEntity>>
}
