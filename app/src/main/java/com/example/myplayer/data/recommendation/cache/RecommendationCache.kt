package com.example.myplayer.data.recommendation.cache

import android.util.LruCache
import com.example.myplayer.data.local.dao.CachedRecommendationDao
import com.example.myplayer.data.local.entity.CachedRecommendationEntity
import com.example.myplayer.data.recommendation.logging.RecommendationLogger
import com.example.myplayer.data.recommendation.model.RecommendationSong
import com.example.myplayer.data.recommendation.utils.RecommendationConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecommendationCache @Inject constructor(
    private val logger: RecommendationLogger,
    private val dao: CachedRecommendationDao? = null
) {
    private data class CacheEntry(
        val timestamp: Long,
        val songs: List<RecommendationSong>
    )

    // Thread-safe LRU Cache (in-memory, fast path)
    private val cache = object : LruCache<String, CacheEntry>(RecommendationConstants.MAX_CACHE_SIZE) {}

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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
        // Write-through to Room so data survives app restarts
        if (dao != null && songs.isNotEmpty()) {
            scope.launch {
                try {
                    val entities = songs.map { song ->
                        CachedRecommendationEntity(
                            videoId = song.videoId,
                            title = song.title,
                            artist = song.artist,
                            thumbnailUrl = song.thumbnailUrl,
                            durationMs = song.durationMs,
                            source = song.source,
                            seedId = videoId
                        )
                    }
                    dao.insertAll(entities)
                    // Prune very old entries (older than 7 days)
                    val cutoff = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
                    dao.deleteExpired(cutoff)
                } catch (e: Exception) {
                    logger.log("DB write-through failed for $videoId: ${e.message}")
                }
            }
        }
    }

    @Synchronized
    fun clear() {
        logger.log("Cache CLEAR triggered")
        cache.evictAll()
        scope.launch {
            try { dao?.clearAll() } catch (e: Exception) { /* ignore */ }
        }
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

    /**
     * Returns persisted recommendations from the DB.
     * Used as a stale-while-revalidate fallback when offline or on cold start.
     * Returns up to [limit] songs ordered by most recently cached.
     */
    suspend fun getPersistedFallback(limit: Int = 30): List<RecommendationSong> {
        val currentDao = dao ?: return emptyList()
        return try {
            currentDao.getRecent(limit).map { it.toRecommendationSong() }
        } catch (e: Exception) {
            logger.log("DB fallback read failed: ${e.message}")
            emptyList()
        }
    }

    private fun CachedRecommendationEntity.toRecommendationSong() = RecommendationSong(
        videoId = videoId,
        title = title,
        artist = artist,
        thumbnailUrl = thumbnailUrl,
        durationMs = durationMs,
        source = source
    )
}
