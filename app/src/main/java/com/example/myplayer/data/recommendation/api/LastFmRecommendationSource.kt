package com.example.myplayer.data.recommendation.api

import android.util.Log
import com.example.myplayer.data.online.InnertubeApi
import com.example.myplayer.data.online.model.OnlineSong
import com.example.myplayer.data.recommendation.lastfm.LastFmChartSource
import com.example.myplayer.data.recommendation.lastfm.LastFmTrackMatcher
import com.example.myplayer.data.recommendation.model.RecommendationSeed
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cold-start discovery source providing popular/trending music.
 * Retrieves top chart metadata from Last.fm, resolves playable streams via YouTube Music,
 * and seamlessly falls back to YouTube trending tracks if Last.fm is rate-limited or unavailable.
 */
@Singleton
open class LastFmRecommendationSource @Inject constructor(
    private val lastFmChartSource: LastFmChartSource,
    private val lastFmTrackMatcher: LastFmTrackMatcher,
    private val innertubeApi: InnertubeApi
) : RecommendationSource {

    companion object {
        private const val TAG = "LastFmRecoSource"
    }

    override val sourceName: String = "LastFmChart"

    override suspend fun fetchRawRecommendations(seed: RecommendationSeed): List<OnlineSong> {
        Log.d(TAG, "Fetching live Trending Music from APIs")
        val allTrending = mutableListOf<OnlineSong>()

        // 1. Fetch Last.fm Top Chart tracks if API key is configured
        try {
            val chartTracks = lastFmChartSource.getTopTracks(limit = 50)
            if (chartTracks.isNotEmpty()) {
                val matchedTracks = lastFmTrackMatcher.matchTracks(chartTracks)
                allTrending.addAll(matchedTracks)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Last.fm chart fetch failed, continuing with YouTube Music Trending API", e)
        }

        // 2. Fetch live Trending & Top Hits feeds from YouTube Music Innertube API
        val trendingQueries = listOf(
            "Trending Songs",
            "Top Hits Global",
            "Trending Music Hits",
            "New Music Hits"
        )

        for (query in trendingQueries) {
            try {
                val page1 = innertubeApi.search(query, filtered = true)
                allTrending.addAll(page1.songs)

                // Fetch continuation page to get all trending tracks from the API
                if (page1.continuationToken != null && allTrending.size < 60) {
                    val page2 = innertubeApi.search(
                        query = query,
                        continuationToken = page1.continuationToken,
                        filtered = true
                    )
                    allTrending.addAll(page2.songs)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Trending API query '$query' failed", e)
            }
        }

        return allTrending.distinctBy { it.videoId }
    }
}
