package com.example.myplayer.data.recommendation.api

import com.example.myplayer.data.online.model.OnlineSong

interface RecommendationApi {
    suspend fun fetchRawRecommendations(seedSong: OnlineSong): List<OnlineSong>
}
