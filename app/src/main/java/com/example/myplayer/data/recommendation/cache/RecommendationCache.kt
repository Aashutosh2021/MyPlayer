package com.example.myplayer.data.recommendation.cache

import android.util.LruCache
import com.example.myplayer.data.recommendation.logging.RecommendationLogger
import com.example.myplayer.data.recommendation.model.RecommendationSong
import com.example.myplayer.data.recommendation.utils.RecommendationConstants
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecommendationCache @Inject constructor(
    private val logger: RecommendationLogger
) {
    private data class CacheEntry(
        val timestamp: Long,
        val songs: List<RecommendationSong>
    )

    // Thread-safe LRU Cache
    private val cache = object : LruCache<String, CacheEntry>(RecommendationConstants.MAX_CACHE_SIZE) {}

    @Synchronized
    fun get(videoId: String): List<RecommendationSong>? {
        val entry = cache.get(videoId)
        if (entry == null) {
            logger.log("Cache MISS for videoId: $videoId (Not found)")
            return null
        }
        
        val age = System.currentTimeMillis() - entry.timestamp
        if (age > RecommendationConstants.CACHE_EXPIRATION_MS) {
            logger.log("Cache MISS for videoId: $videoId (Expired, age: ${age}ms)")
            cache.remove(videoId)
            return null
        }
        
        logger.log("Cache HIT for videoId: $videoId (Count: ${entry.songs.size}, age: ${age}ms)")
        return entry.songs
    }

    @Synchronized
    fun put(videoId: String, songs: List<RecommendationSong>) {
        logger.log("Cache PUT for videoId: $videoId (Count: ${songs.size})")
        cache.put(videoId, CacheEntry(
            timestamp = System.currentTimeMillis(),
            songs = songs
        ))
    }

    @Synchronized
    fun clear() {
        logger.log("Cache CLEAR triggered")
        cache.evictAll()
    }
    
    @Synchronized
    fun isCached(videoId: String): Boolean {
        val entry = cache.get(videoId) ?: return false
        val age = System.currentTimeMillis() - entry.timestamp
        return age <= RecommendationConstants.CACHE_EXPIRATION_MS
    }
    
    @Synchronized
    fun invalidate(videoId: String) {
        logger.log("Cache INVALIDATE for videoId: $videoId")
        cache.remove(videoId)
    }
}
