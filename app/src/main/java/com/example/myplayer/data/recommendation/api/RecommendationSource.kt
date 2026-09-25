package com.example.myplayer.data.recommendation.api

import android.util.Log
import com.example.myplayer.data.online.InnertubeApi
import com.example.myplayer.data.online.model.OnlineSong
import com.example.myplayer.data.recommendation.model.RecommendationSeed
import javax.inject.Inject
import javax.inject.Singleton

interface RecommendationSource : RecommendationApi {
    val sourceName: String
}

@Singleton
open class YouTubeRecommendationSource @Inject constructor(
    private val innertubeApi: InnertubeApi
) : RecommendationSource {

    companion object {
        private const val TAG = "YouTubeRecoSource"
    }

    override val sourceName: String = "YouTubeRadio"

    override suspend fun fetchRawRecommendations(seed: RecommendationSeed): List<OnlineSong> {
        // 1. Identify canonical online videoId
        val rawId = seed.songId.removePrefix("online://").trim()
        val videoId = when {
            rawId.matches(Regex("^[a-zA-Z0-9_-]{11}$")) -> rawId
            seed.source.equals("youtube", ignoreCase = true) && rawId.isNotBlank() -> rawId
            else -> null
        }

        // 2. If reliable videoId is available, fetch true radio/related tracks from InnerTube next endpoint
        if (videoId != null) {
            try {
                Log.d(TAG, "Fetching radio recommendations for seed videoId: $videoId ('${seed.title}')")
                val radioSongs = innertubeApi.getNextRadioTracks(videoId)
                if (radioSongs.isNotEmpty()) {
                    Log.d(TAG, "Successfully fetched ${radioSongs.size} radio songs from YouTube Music")
                    return radioSongs
                }
                Log.w(TAG, "YouTube radio returned 0 tracks for $videoId, falling back to search")
            } catch (e: Exception) {
                Log.w(TAG, "Error fetching YouTube radio for $videoId, falling back to search", e)
            }
        }

        // 3. Fallback: Search using metadata if videoId was missing or radio returned empty
        val query = listOf(seed.artist, seed.title)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { seed.album }
            .trim()

        if (query.isBlank()) return emptyList()

        return try {
            Log.d(TAG, "Fetching search-based fallback recommendations for query: '$query'")
            val searchPage = innertubeApi.search(query)
            searchPage.songs.filter { it.videoId != videoId }
        } catch (e: Exception) {
            Log.w(TAG, "Search fallback failed for '$query'", e)
            emptyList()
        }
    }
}

