package com.example.myplayer.data.recommendation

import com.example.myplayer.data.recommendation.cache.RecommendationCache
import com.example.myplayer.data.recommendation.engine.RecommendationEngine
import com.example.myplayer.data.recommendation.model.RecommendationResult
import com.example.myplayer.data.recommendation.model.RecommendationSeed
import com.example.myplayer.data.recommendation.model.RecommendationSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecommendationRepository @Inject constructor(
    private val engine: RecommendationEngine,
    private val cache: RecommendationCache
) {
    suspend fun getRecommendations(seed: RecommendationSeed): List<RecommendationSong> = withContext(Dispatchers.IO) {
        val cached = cache.get(seed.songId)
        if (cached != null) {
            return@withContext cached
        }

        val result = engine.generateRecommendations(seed)
        if (result is RecommendationResult.Success) {
            cache.put(seed.songId, result.songs)
            return@withContext result.songs
        }
        
        return@withContext emptyList()
    }

    suspend fun preloadRecommendations(seed: RecommendationSeed) = withContext(Dispatchers.IO) {
        if (cache.get(seed.songId) == null) {
            val result = engine.generateRecommendations(seed)
            if (result is RecommendationResult.Success) {
                cache.put(seed.songId, result.songs)
            }
        }
    }

    fun clearCache() {
        cache.clear()
    }
}
