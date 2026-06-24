package com.example.myplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myplayer.data.local.entity.RecentHistoryEntity
import com.example.myplayer.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentHistoryDao {
    @Query("""
        SELECT songs.* FROM songs 
        INNER JOIN recent_history ON songs.id = recent_history.songId 
        ORDER BY recent_history.playedAt DESC LIMIT 50
    """)
    fun getRecentHistory(): Flow<List<SongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addRecent(history: RecentHistoryEntity): Unit
}
