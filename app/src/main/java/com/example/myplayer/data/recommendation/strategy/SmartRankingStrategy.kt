package com.example.myplayer.data.recommendation.strategy

import com.example.myplayer.data.recommendation.model.RecommendationSeed
import com.example.myplayer.data.recommendation.model.RecommendationSong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Intelligent recommendation ranking and discovery strategy.
 * Inspired by LastWave's multi-tier scoring, Hidden Gem bonus algorithm,
 * genre neighborhood expansion, and artist/album diversity caps.
 */
@Singleton
class SmartRankingStrategy @Inject constructor() : RecommendationStrategy {

    companion object {
        const val RECO_ARTIST_CAP = 2
        const val RECO_ALBUM_CAP = 2

        /**
         * Widens genre discovery without drifting away from the user's taste.
         */
        val RECO_GENRE_NEIGHBORS = mapOf(
            "indie rock" to listOf("alternative rock", "indie pop"),
            "alternative rock" to listOf("indie rock", "post-grunge"),
            "house" to listOf("deep house", "tech house"),
            "deep house" to listOf("tech house", "house"),
            "techno" to listOf("tech house", "trance", "minimal techno"),
            "hip hop" to listOf("rap", "boom bap", "trap"),
            "rap" to listOf("hip hop", "trap"),
            "metal" to listOf("heavy metal", "hard rock"),
            "heavy metal" to listOf("metal", "hard rock"),
            "pop" to listOf("indie pop", "dance pop", "synth-pop"),
            "indie pop" to listOf("dream pop", "pop", "indie rock"),
            "dream pop" to listOf("shoegaze", "indie pop", "chillwave"),
            "shoegaze" to listOf("dream pop", "post-rock"),
            "jazz" to listOf("soul", "blues", "neo-soul"),
            "soul" to listOf("r&b", "neo-soul", "funk"),
            "r&b" to listOf("soul", "neo-soul", "contemporary r&b"),
            "ambient" to listOf("chillwave", "lo-fi", "downtempo"),
            "lo-fi" to listOf("chillwave", "ambient", "downtempo"),
            "chillwave" to listOf("synthwave", "dream pop", "lo-fi"),
            "synthwave" to listOf("chillwave", "retrowave", "electro"),
            "punk" to listOf("post-punk", "garage rock"),
            "post-punk" to listOf("new wave", "synth-pop", "darkwave"),
            "folk" to listOf("indie folk", "acoustic"),
            "indie folk" to listOf("folk", "acoustic", "singer-songwriter")
        )
    }

    override fun scoreAndRank(
        seed: RecommendationSeed,
        candidates: List<RecommendationSong>
    ): List<RecommendationSong> {
        val scoredList = candidates.map { candidate ->
            var score = 0
            val reasons = mutableListOf<String>()

            // 1. Same Artist Bonus
            if (candidate.artist.equals(seed.artist, ignoreCase = true)) {
                score += 25
                reasons.add("Same Artist")
            }

            // 2. Genre Alignment & Neighbor Expansion
            val seedGenre = seed.genre.trim().lowercase()
            val candidateGenre = (candidate.metadata["genre"] ?: "").trim().lowercase()

            if (seedGenre.isNotEmpty() && candidateGenre.isNotEmpty()) {
                if (seedGenre == candidateGenre) {
                    score += 15
                    reasons.add("Exact Genre")
                } else {
                    val neighbors = RECO_GENRE_NEIGHBORS[seedGenre] ?: emptyList()
                    if (neighbors.any { it.equals(candidateGenre, ignoreCase = true) }) {
                        score += 10
                        reasons.add("Genre Neighbor ($candidateGenre)")
                    }
                }
            }

            // 3. Hidden Gem Bonus Algorithm (Peak score for sweet spot discoveries)
            val gemBonus = calculateGemScore(candidate.popularityScore)
            if (gemBonus > 0) {
                score += gemBonus
                reasons.add("Hidden Gem")
            } else if (gemBonus < 0) {
                score += gemBonus // Minor penalty for extreme mainstream saturation
            }

            // 4. Token Similarity in Titles/Albums
            val seedTokens = (seed.title + " " + seed.album).lowercase().split(Regex("\\W+")).filter { it.length > 2 }
            val candidateTokens = (candidate.title + " " + candidate.album).lowercase().split(Regex("\\W+")).filter { it.length > 2 }
            val commonTokens = seedTokens.intersect(candidateTokens.toSet())
            if (commonTokens.isNotEmpty()) {
                score += (commonTokens.size * 4)
                reasons.add("Title Overlap")
            }

            candidate.copy(
                recommendationScore = score,
                reason = reasons.joinToString(", ")
            )
        }.sortedByDescending { it.recommendationScore }

        // 5. Diversity Interleaving: Enforce Artist and Album Caps
        return applyDiversityCaps(scoredList)
    }

    /**
     * LastWave-inspired "Hidden Gem" bonus scoring:
     * - Discovers high-quality, authentic tracks (score 20..60 get highest +9 reward).
     * - Moderate tracks get +4 to +7.
     * - Extremely mainstream tracks (>95 popularity) get -2 to encourage fresh discovery.
     */
    fun calculateGemScore(popularityScore: Int): Int {
        return when {
            popularityScore in 20..50 -> 9   // Peak hidden gem sweet spot
            popularityScore in 10..19 -> 7   // Emerging indie
            popularityScore in 51..75 -> 5   // Established discovery
            popularityScore in 1..9 -> 3     // Niche / rare
            popularityScore > 90 -> -2       // Saturated mainstream
            else -> 0
        }
    }

    /**
     * Filters and interleaves candidates so no artist or album dominates the queue.
     */
    private fun applyDiversityCaps(sorted: List<RecommendationSong>): List<RecommendationSong> {
        val artistCounts = mutableMapOf<String, Int>()
        val albumCounts = mutableMapOf<String, Int>()
        val cappedList = mutableListOf<RecommendationSong>()
        val deferredList = mutableListOf<RecommendationSong>()

        for (song in sorted) {
            val artistKey = song.artist.trim().lowercase()
            val albumKey = song.album.trim().lowercase()

            val currentArtistCount = artistCounts.getOrDefault(artistKey, 0)
            val currentAlbumCount = if (albumKey.isNotEmpty()) albumCounts.getOrDefault(albumKey, 0) else 0

            if (currentArtistCount < RECO_ARTIST_CAP && (albumKey.isEmpty() || currentAlbumCount < RECO_ALBUM_CAP)) {
                artistCounts[artistKey] = currentArtistCount + 1
                if (albumKey.isNotEmpty()) albumCounts[albumKey] = currentAlbumCount + 1
                cappedList.add(song)
            } else {
                deferredList.add(song)
            }
        }

        // Append deferred songs at the tail to preserve queue length
        return cappedList + deferredList
    }
}
