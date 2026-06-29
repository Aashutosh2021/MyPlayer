package com.example.myplayer.data.recommendation.api

import com.example.myplayer.data.online.InnertubeApi
import com.example.myplayer.data.online.model.OnlineSong
import com.example.myplayer.data.recommendation.model.RecommendationSeed
import javax.inject.Inject
import javax.inject.Singleton

interface RecommendationSource : RecommendationApi {
    val sourceName: String
}

@Singleton
class YouTubeRecommendationSource @Inject constructor(
    private val innertubeApi: InnertubeApi
) : RecommendationSource {
    
    override val sourceName: String = "YouTube"

    override suspend fun fetchRawRecommendations(seed: RecommendationSeed): List<OnlineSong> {
        // Build a meaningful search query from human-readable metadata.
        // NOTE: seed.songId is an opaque id (e.g. YouTube videoId), so it must
        // never be used directly in the search query.
        val query = listOf(seed.artist, seed.title)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { seed.album }
            .trim()

        if (query.isBlank()) return emptyList()

        val searchPage = innertubeApi.search(query)
        return searchPage.songs
    }
}
