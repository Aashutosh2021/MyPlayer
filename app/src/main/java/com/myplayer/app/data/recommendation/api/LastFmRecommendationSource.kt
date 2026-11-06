package com.myplayer.app.data.recommendation.api

import com.myplayer.app.data.online.model.OnlineSong
import com.myplayer.app.data.recommendation.ai.OnDeviceMusicAiModel
import com.myplayer.app.data.recommendation.model.RecommendationSeed
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class LastFmRecommendationSource : RecommendationSource {
    private val aiModel: OnDeviceMusicAiModel?

    @Inject
    constructor(aiModel: OnDeviceMusicAiModel) {
        this.aiModel = aiModel
    }

    constructor() {
        this.aiModel = null
    }

    override val sourceName: String = "OnDeviceAiSource"

    override suspend fun fetchRawRecommendations(seed: RecommendationSeed): List<OnlineSong> {
        val model = aiModel ?: return emptyList()
        if (seed.songId.isNotBlank() && seed.title.isNotBlank()) {
            model.onSongTransition(seed.songId, seed.title, seed.artist)
        }
        return model.generateRecommendations(limit = 40).map { reco ->
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
