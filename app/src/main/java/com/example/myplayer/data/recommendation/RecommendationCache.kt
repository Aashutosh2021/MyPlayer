package com.example.myplayer.data.recommendation

import com.example.myplayer.data.recommendation.model.RecommendationSong
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thread-safe, in-memory cache for recommendations.
 * Used to avoid duplicate requests and store recent recommendations.
 */
@Singleton
class RecommendationCache @Inject constructor() {
    
    private val cache = ConcurrentHashMap<String, CacheEntry>()
    // Configurable expiration (e.g., 15 minutes)
    private val expirationMs: Long = 15 * 60 * 1000

    private data class CacheEntry(
        val timestamp: Long,
        val songs: List<RecommendationSong>
    )

    fun get(videoId: String): List<RecommendationSong>? {
        val entry = cache[videoId] ?: return null
        if (System.currentTimeMillis() - entry.timestamp > expirationMs) {
            cache.remove(videoId)
            return null
        }
        return entry.songs
    }

    fun put(videoId: String, songs: List<RecommendationSong>) {
        cache[videoId] = CacheEntry(
            timestamp = System.currentTimeMillis(),
            songs = songs
        )
    }

    fun clear() {
        cache.clear()
    }
}
