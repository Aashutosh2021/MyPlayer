package com.example.myplayer.data.recommendation.playback

import com.example.myplayer.data.online.InnertubeApi
import com.example.myplayer.data.online.model.OnlineSong
import com.example.myplayer.data.recommendation.logging.RecommendationLogger
import com.example.myplayer.data.recommendation.model.RecommendationSong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecommendationPlaybackRepository @Inject constructor(
    private val innertubeApi: InnertubeApi,
    private val logger: RecommendationLogger
) {
    /**
     * Resolves the stream URL for a RecommendationSong and converts it to an OnlineSong.
     */
    suspend fun resolveStreamUrl(rec: RecommendationSong): OnlineSong? {
        return try {
            val url = innertubeApi.getStreamUrl(rec.videoId)
            if (url != null) {
                OnlineSong(
                    videoId = rec.videoId,
                    title = rec.title,
                    artist = rec.artist,
                    thumbnailUrl = rec.thumbnailUrl,
                    durationMs = rec.durationMs,
                    durationText = rec.durationMs.toString(),
                    streamUrl = url
                )
            } else {
                logger.logEvent("AutoplayRecommendationStreamFailed", mapOf(
                    "videoId" to rec.videoId
                ))
                null
            }
        } catch (e: Exception) {
            logger.logEvent("AutoplayRecommendationError", mapOf(
                "videoId" to rec.videoId,
                "error" to (e.message ?: "Unknown")
            ))
            null
        }
    }
}
