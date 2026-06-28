package com.example.myplayer.data.recommendation.api

import com.example.myplayer.data.online.InnertubeApi
import com.example.myplayer.data.online.model.OnlineSong
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

    override suspend fun fetchRawRecommendations(seedSong: OnlineSong): List<OnlineSong> {
        val query = "${seedSong.artist} ${seedSong.title}"
        val searchPage = innertubeApi.search(query)
        return searchPage.songs
    }
}
