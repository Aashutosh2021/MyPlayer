package com.myplayer.app.data.recommendation.playback

import com.myplayer.app.data.recommendation.model.RecommendationSong
import com.myplayer.app.playback.PlayRequest
import com.myplayer.app.playback.PlaybackSourceType
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
