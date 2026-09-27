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
    companion object {
        private val TOKEN_REGEX = Regex("\\W+")
    }

    override fun scoreAndRank(seed: RecommendationSeed, candidates: List<RecommendationSong>): List<RecommendationSong> {
        val seedTokens = if (seed.album.isNotBlank()) {
            seed.album.lowercase().split(TOKEN_REGEX).filter { it.isNotBlank() }.toSet()
        } else emptySet()

        val scored = candidates.map { candidate ->
            var score = 0
            val reasons = mutableListOf<String>()

            // Score Same Artist
            if (candidate.artist.isNotBlank() && candidate.artist.equals(seed.artist, ignoreCase = true)) {
                score += RecommendationConstants.SCORE_SAME_ARTIST
                reasons.add("Same Artist")
            }

            // Simple Album/Title matching
            if (seedTokens.isNotEmpty()) {
                val candidateTokens = candidate.title.lowercase().split(TOKEN_REGEX).filter { it.isNotBlank() }.toSet()
                val commonTokens = seedTokens.intersect(candidateTokens)
                if (commonTokens.isNotEmpty()) {
                    score += (commonTokens.size * 5)
                    reasons.add("Similar Title")
                }
            }

            candidate.copy(
                recommendationScore = score,
                reason = reasons.joinToString(", ")
            )
        }.sortedByDescending { it.recommendationScore }

        // Artist diversity interleaving: avoid consecutive tracks from the exact same artist
        val artistCounts = mutableMapOf<String, Int>()
        return scored.map { candidate ->
            val artistKey = candidate.artist.lowercase().trim()
            val count = artistCounts.getOrDefault(artistKey, 0)
            artistCounts[artistKey] = count + 1

            // If an artist has already appeared 2+ times in the top rankings, apply a slight penalty
            val adjustedScore = if (count >= 2) {
                (candidate.recommendationScore - (count * 10)).coerceAtLeast(0)
            } else {
                candidate.recommendationScore
            }
            candidate.copy(recommendationScore = adjustedScore)
        }.sortedByDescending { it.recommendationScore }
    }
}
