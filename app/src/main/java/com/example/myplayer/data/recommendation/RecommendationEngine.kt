package com.example.myplayer.data.recommendation

import com.example.myplayer.data.online.InnertubeApi
import com.example.myplayer.data.online.model.OnlineSong
import com.example.myplayer.data.recommendation.model.RecommendationSong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pure business logic for generating recommendations.
 * Transforms API data into RecommendationSong objects.
 */
@Singleton
class RecommendationEngine @Inject constructor(
    private val innertubeApi: InnertubeApi
) {
    suspend fun generateRecommendations(song: OnlineSong): List<RecommendationSong> {
        return try {
            // Very simple recommendation strategy: search for similar tracks
            // using the same artist and a portion of the title, or simply "artist title".
            val query = "${song.artist} ${song.title}"
            
            val searchPage = innertubeApi.search(query)
            
            searchPage.songs
                .filter { it.videoId != song.videoId }
                .map { onlineSong ->
                    RecommendationSong(
                        videoId = onlineSong.videoId,
                        title = onlineSong.title,
                        artist = onlineSong.artist,
                        thumbnailUrl = onlineSong.thumbnailUrl,
                        durationMs = onlineSong.durationMs
                    )
                }
        } catch (e: Exception) {
            // Never throw uncaught exceptions, return empty list on failure
            emptyList()
        }
    }
}
