package com.myplayer.app.data.recommendation.api

import com.myplayer.app.data.online.model.OnlineSong
import com.myplayer.app.data.recommendation.model.RecommendationSeed

interface RecommendationApi {
    suspend fun fetchRawRecommendations(seed: RecommendationSeed): List<OnlineSong>
}
