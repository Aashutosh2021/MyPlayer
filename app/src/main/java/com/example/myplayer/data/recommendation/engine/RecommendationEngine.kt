package com.example.myplayer.data.recommendation.engine

import com.example.myplayer.data.online.model.OnlineSong
import com.example.myplayer.data.recommendation.api.RecommendationSource
import com.example.myplayer.data.recommendation.logging.RecommendationLogger
import com.example.myplayer.data.recommendation.logging.RecommendationMetrics
import com.example.myplayer.data.recommendation.mapper.RecommendationMapper
import com.example.myplayer.data.recommendation.model.RecommendationResult
import com.example.myplayer.data.recommendation.model.RecommendationSong
import com.example.myplayer.data.recommendation.strategy.RecommendationStrategy
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.measureTimeMillis

@Singleton
class RecommendationEngine @Inject constructor(
    private val source: RecommendationSource,
    private val validator: RecommendationValidator,
    var strategy: RecommendationStrategy,
    private val mapper: RecommendationMapper,
    private val logger: RecommendationLogger,
    private val metrics: RecommendationMetrics
) {
    suspend fun generateRecommendations(song: OnlineSong): RecommendationResult {
        logger.log("Starting recommendation generation for: '${song.title}' via ${source.sourceName}")
        var result: RecommendationResult
        
        val timeTaken = measureTimeMillis {
            try {
                // 1. Fetch raw candidates
                logger.log("Executing fetch from source: ${source.sourceName}")
                val rawCandidates = source.fetchRawRecommendations(song)
                
                // 2. Map to Domain Models
                val mappedCandidates = rawCandidates.map { 
                    mapper.toRecommendationSong(it, source = source.sourceName)
                }

                // 3. Validate and Filter
                val validCandidates = validator.filterAndValidate(song, mappedCandidates)

                // 4. Score and Rank
                val rankedCandidates = strategy.scoreAndRank(song, validCandidates)

                result = RecommendationResult.Success(rankedCandidates)
                
                if (rankedCandidates.isNotEmpty()) {
                    metrics.recordFetchSuccess(0) // Time recorded outside
                } else {
                    metrics.recordFetchFailure()
                }
                
            } catch (e: Exception) {
                logger.error("Exception during recommendation generation", e)
                metrics.recordFetchFailure()
                result = RecommendationResult.Error(e.message ?: "Unknown error", e)
            }
        }
        
        if (result is RecommendationResult.Success) {
            metrics.recordFetchSuccess(timeTaken) // Accurately record time
            logger.log("Completed recommendation generation in ${timeTaken}ms. Final ranked count: ${(result as RecommendationResult.Success).songs.size}")
        }
        
        return result
    }
}
