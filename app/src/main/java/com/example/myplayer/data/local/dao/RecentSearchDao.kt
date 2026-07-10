package com.example.myplayer.data.local.dao

import androidx.room.*
import com.example.myplayer.data.local.entity.RecentSearchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentSearchDao {

    @Query("SELECT * FROM recent_searches ORDER BY timestamp DESC LIMIT 20")
    fun getRecentSearches(): Flow<List<RecentSearchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RecentSearchEntity)

    @Query("DELETE FROM recent_searches WHERE query = :query")
    suspend fun delete(query: String)

    @Query("DELETE FROM recent_searches WHERE query NOT IN (SELECT query FROM recent_searches ORDER BY timestamp DESC LIMIT 20)")
    suspend fun trimOldEntries()

    @Query("DELETE FROM recent_searches")
    suspend fun clearAll()

    @Query("SELECT * FROM recent_searches")
    fun getAllRecentSearchesSync(): List<RecentSearchEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRecentSearches(searches: List<RecentSearchEntity>)
}
