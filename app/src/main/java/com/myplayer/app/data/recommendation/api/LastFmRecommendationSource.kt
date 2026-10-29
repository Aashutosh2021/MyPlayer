package com.myplayer.app.data.recommendation.api

import com.myplayer.app.data.online.model.OnlineSong
import com.myplayer.app.data.recommendation.ai.OnDeviceMusicAiModel
import com.myplayer.app.data.recommendation.model.RecommendationSeed
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class LastFmRecommendationSource @Inject constructor(
    private val aiModel: OnDeviceMusicAiModel
) : RecommendationSource {

    override val sourceName: String = "OnDeviceAiSource"

    override suspend fun fetchRawRecommendations(seed: RecommendationSeed): List<OnlineSong> {
        if (seed.songId.isNotBlank() && seed.title.isNotBlank()) {
            aiModel.onSongTransition(seed.songId, seed.title, seed.artist)
        }
        return aiModel.generateRecommendations(limit = 40).map { reco ->
            OnlineSong(
                videoId = reco.videoId,
                title = reco.title,
                artist = reco.artist,
                thumbnailUrl = reco.thumbnailUrl,
                durationMs = reco.durationMs,
                durationText = "",
                streamUrl = null
            )
        }
    }
}

