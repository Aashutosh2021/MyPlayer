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
        Log.d(TAG, "Fetching Last.fm chart recommendations for cold start")

        // 1. Fetch Last.fm chart tracks (uses memory cache or fresh network or stale fallback)
        val chartTracks = lastFmChartSource.getTopTracks(limit = 25)

        if (chartTracks.isNotEmpty()) {
            // 2. Match Last.fm metadata to playable YouTube Music candidates with bounded concurrency
            Log.d(TAG, "Matching ${chartTracks.size} Last.fm tracks to playable online sources")
            val matchedTracks = lastFmTrackMatcher.matchTracks(chartTracks)
            if (matchedTracks.isNotEmpty()) {
                Log.d(TAG, "Successfully matched ${matchedTracks.size} playable tracks from Last.fm charts")
                return matchedTracks
            }
            Log.w(TAG, "All Last.fm chart tracks failed to match playable sources, trying fallback")
        } else {
            Log.w(TAG, "Last.fm chart returned 0 tracks (rate limit or network error), using trending fallback")
        }

        // 3. Graceful Fallback: Query YouTube Music for trending / top hit songs
        return try {
            Log.d(TAG, "RECOMMENDATION_FALLBACK: Fetching trending YouTube Music tracks for cold-start")
            val fallbackSearch = innertubeApi.search("Top Hits Global")
            if (fallbackSearch.songs.isNotEmpty()) {
                fallbackSearch.songs
            } else {
                innertubeApi.search("Trending Songs").songs
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cold start fallback failed", e)
            emptyList()
        }
    }
}
