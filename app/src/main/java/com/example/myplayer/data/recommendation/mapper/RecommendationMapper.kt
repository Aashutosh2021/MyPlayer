package com.example.myplayer.data.recommendation.mapper

import com.example.myplayer.data.online.model.OnlineSong
import com.example.myplayer.data.recommendation.model.RecommendationSong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecommendationMapper @Inject constructor() {
    
    fun toRecommendationSong(
        onlineSong: OnlineSong, 
        source: String = "YouTube", 
        reason: String = "", 
        popularityScore: Int = 0,
        recommendationScore: Int = 0
    ): RecommendationSong {
        return RecommendationSong(
            videoId = onlineSong.videoId,
            title = onlineSong.title,
            artist = onlineSong.artist,
            album = "",
            durationMs = onlineSong.durationMs,
            thumbnailUrl = onlineSong.thumbnailUrl,
            popularityScore = popularityScore,
            recommendationScore = recommendationScore,
            source = source,
            reason = reason,
            metadata = emptyMap()
        )
    }
}
