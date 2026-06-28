package com.example.myplayer.data.recommendation

import com.example.myplayer.data.online.model.OnlineSong
import com.example.myplayer.data.recommendation.cache.RecommendationCache
import com.example.myplayer.data.recommendation.engine.RecommendationEngine
import com.example.myplayer.data.recommendation.logging.RecommendationLogger
import com.example.myplayer.data.recommendation.logging.RecommendationMetrics
import com.example.myplayer.data.recommendation.model.RecommendationSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecommendationRepository @Inject constructor(
    private val engine: RecommendationEngine,
    private val cache: RecommendationCache,
    private val logger: RecommendationLogger,
    private val metrics: RecommendationMetrics
) {
    suspend fun getRecommendations(song: OnlineSong): List<RecommendationSong> = withContext(Dispatchers.IO) {
        val cached = cache.get(song.videoId)
        if (cached != null) {
            metrics.recordCacheHit()
            return@withContext cached
        }
        
        metrics.recordCacheMiss()

        val result = engine.generateRecommendations(song)
        val recommendations = result.getSongsOrEmpty()

        if (recommendations.isNotEmpty()) {
            cache.put(song.videoId, recommendations)
        }
        
        return@withContext recommendations
    }

    suspend fun preloadRecommendations(song: OnlineSong): Unit = withContext(Dispatchers.IO) {
        logger.log("Preload Started")
        
        if (cache.isCached(song.videoId)) {
             logger.log("Cache Hit")
             return@withContext
        }

        getRecommendations(song)
    }

    fun clearCache() {
        cache.clear()
    }
    
    fun invalidate(videoId: String) {
        cache.invalidate(videoId)
    }
    
    suspend fun refresh(song: OnlineSong): List<RecommendationSong> = withContext(Dispatchers.IO) {
        invalidate(song.videoId)
        return@withContext getRecommendations(song)
    }
    
    fun isCached(videoId: String): Boolean {
        return cache.isCached(videoId)
    }
    
    fun setStrategy(strategy: com.example.myplayer.data.recommendation.strategy.RecommendationStrategy) {
        engine.strategy = strategy
    }
}
