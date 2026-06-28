package com.example.myplayer.data.recommendation.strategy

import com.example.myplayer.data.recommendation.model.RecommendationSeed
import com.example.myplayer.data.recommendation.model.RecommendationSong
import com.example.myplayer.data.recommendation.utils.RecommendationConstants
import javax.inject.Inject
import javax.inject.Singleton

interface RecommendationStrategy {
    fun scoreAndRank(seed: RecommendationSeed, candidates: List<RecommendationSong>): List<RecommendationSong>
}

@Singleton
class DefaultRankingStrategy @Inject constructor() : RecommendationStrategy {
    override fun scoreAndRank(seed: RecommendationSeed, candidates: List<RecommendationSong>): List<RecommendationSong> {
        return candidates.map { candidate ->
            var score = 0
            val reasons = mutableListOf<String>()

            // Score Same Artist
            if (candidate.artist.equals(seed.artist, ignoreCase = true)) {
                score += RecommendationConstants.SCORE_SAME_ARTIST
                reasons.add("Same Artist")
            }

            // Simple Album matching (fuzzy)
            val seedTokens = seed.album.lowercase().split(Regex("\\W+"))
            val candidateTokens = candidate.title.lowercase().split(Regex("\\W+"))
            val commonTokens = seedTokens.intersect(candidateTokens.toSet())
            
            if (commonTokens.isNotEmpty()) {
                score += (commonTokens.size * 5)
                reasons.add("Similar Title")
            }
            
            candidate.copy(
                recommendationScore = score,
                reason = reasons.joinToString(", ")
            )
        }.sortedByDescending { it.recommendationScore }
    }
}
