package com.example.myplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myplayer.data.local.entity.CachedRecommendationEntity

@Dao
interface CachedRecommendationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<CachedRecommendationEntity>)

    @Query("SELECT * FROM cached_recommendations ORDER BY cachedAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 50): List<CachedRecommendationEntity>

    @Query("SELECT * FROM cached_recommendations WHERE seedId = :seedId ORDER BY cachedAt DESC LIMIT :limit")
    suspend fun getBySeed(seedId: String, limit: Int = 30): List<CachedRecommendationEntity>

    @Query("DELETE FROM cached_recommendations WHERE cachedAt < :expireBeforeMs")
    suspend fun deleteExpired(expireBeforeMs: Long)

    @Query("DELETE FROM cached_recommendations")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM cached_recommendations")
    suspend fun count(): Int
}
