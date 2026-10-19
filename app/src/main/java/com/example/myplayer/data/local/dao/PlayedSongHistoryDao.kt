package com.example.myplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myplayer.data.local.entity.PlayedSongHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayedSongHistoryDao {

    @Query("SELECT * FROM played_songs_history WHERE playedAt >= :cutoffTime ORDER BY playedAt DESC LIMIT 100")
    fun getPlayedHistory(cutoffTime: Long): Flow<List<PlayedSongHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(song: PlayedSongHistoryEntity)

    @Query("DELETE FROM played_songs_history WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM played_songs_history")
    suspend fun clearHistory()

    @Query("DELETE FROM played_songs_history WHERE playedAt < :cutoffTime")
    suspend fun pruneOldHistory(cutoffTime: Long)
}
