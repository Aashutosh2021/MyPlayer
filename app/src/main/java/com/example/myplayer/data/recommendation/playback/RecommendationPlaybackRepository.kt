package com.example.myplayer.data.recommendation.playback

import com.example.myplayer.data.recommendation.model.RecommendationSong
import com.example.myplayer.playback.PlayRequest
import com.example.myplayer.playback.PlaybackSourceType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecommendationPlaybackRepository @Inject constructor() {
    /**
     * Converts a RecommendationSong to a PlayRequest.
     */
    fun preparePlayRequest(rec: RecommendationSong): PlayRequest {
        return PlayRequest(
            songId = rec.videoId,
            title = rec.title,
            artist = rec.artist,
            playbackSource = PlaybackSourceType.RECOMMENDATION,
            localUri = "online://${rec.videoId}",
            albumArt = rec.thumbnailUrl
        )
    }
}
