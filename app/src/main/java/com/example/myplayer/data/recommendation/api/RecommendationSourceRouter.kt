package com.example.myplayer.data.recommendation.api

import android.util.Log
import com.example.myplayer.data.online.model.OnlineSong
import com.example.myplayer.data.recommendation.model.RecommendationSeed
import com.example.myplayer.data.recommendation.state.RecommendationUserStateDetector
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Routes recommendation requests to the optimal source based on the user's
 * playback history and current playback context:
 * - Cold Start (no playback history) -> LastFmRecommendationSource (chart metadata -> matched online tracks)
 * - Returning User / Online Song -> YouTubeRecommendationSource (InnerTube next radio endpoint)
 * - Automatic graceful fallback hierarchy between sources
 */
@Singleton
class RecommendationSourceRouter @Inject constructor(
    private val userStateDetector: RecommendationUserStateDetector,
    private val lastFmSource: LastFmRecommendationSource,
    private val youTubeRadioSource: YouTubeRecommendationSource
) : RecommendationSource {

    companion object {
        private const val TAG = "RecoSourceRouter"
    }

    override val sourceName: String = "RecommendationSourceRouter"

    override suspend fun fetchRawRecommendations(seed: RecommendationSeed): List<OnlineSong> {
        val userState = userStateDetector.detectUserState()

        val isColdStart = userState.isColdStart || 
                          seed.songId.isBlank() || 
                          seed.source.equals("cold_start", ignoreCase = true)

        val selectedSource = if (isColdStart) {
            Log.d(TAG, "RECOMMENDATION_SOURCE_SELECTED: Cold start detected (totalPlays=${userState.totalPlaysCount}) -> LastFmChartSource")
            lastFmSource
        } else {
            Log.d(TAG, "RECOMMENDATION_SOURCE_SELECTED: Existing user / online seed ('${seed.songId}') -> YouTubeRadioSource")
            youTubeRadioSource
        }

        val candidates = selectedSource.fetchRawRecommendations(seed)
        if (candidates.isNotEmpty()) {
            return candidates
        }

        // Graceful fallback to alternate source if initial source produced no candidates
        if (selectedSource !== lastFmSource) {
            Log.w(TAG, "RECOMMENDATION_FALLBACK: YouTube radio produced 0 results, falling back to Last.fm/trending charts")
            return lastFmSource.fetchRawRecommendations(seed)
        }

        return emptyList()
    }
}
