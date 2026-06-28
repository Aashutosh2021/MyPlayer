package com.example.myplayer.data.recommendation

import com.example.myplayer.data.online.model.OnlineSong
import com.example.myplayer.data.recommendation.model.RecommendationSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository layer for the Recommendation Engine.
 * Exposes methods to fetch, preload, and clear recommendation data.
 */
@Singleton
class RecommendationRepository @Inject constructor(
    private val engine: RecommendationEngine,
    private val cache: RecommendationCache
) {
    /**
     * Retrieves recommendations for the given song.
     * Hits the in-memory cache first to avoid duplicate network requests.
     */
    suspend fun getRecommendations(song: OnlineSong): List<RecommendationSong> = withContext(Dispatchers.IO) {
        val cached = cache.get(song.videoId)
        if (cached != null) {
            return@withContext cached
        }

        val recommendations = engine.generateRecommendations(song)
        if (recommendations.isNotEmpty()) {
            cache.put(song.videoId, recommendations)
        }
        
        return@withContext recommendations
    }

    /**
     * Preloads recommendations into the cache in the background.
     */
    suspend fun preloadRecommendations(song: OnlineSong) = withContext(Dispatchers.IO) {
        // Only fetch if not already cached
        if (cache.get(song.videoId) == null) {
            getRecommendations(song)
        }
    }

    /**
     * Clears the in-memory recommendation cache.
     */
    fun clearCache() {
        cache.clear()
    }
}
