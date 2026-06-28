package com.example.myplayer.data.recommendation.api

import com.example.myplayer.data.online.model.OnlineSong
import com.example.myplayer.data.recommendation.model.RecommendationSeed

interface RecommendationApi {
    suspend fun fetchRawRecommendations(seed: RecommendationSeed): List<OnlineSong>
}
