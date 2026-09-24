package com.example.myplayer.data.artwork.provider

import android.util.Log
import com.example.myplayer.data.artwork.matching.ArtworkMatcher
import com.example.myplayer.data.artwork.model.ArtworkResult
import com.example.myplayer.data.online.InnertubeApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YouTubeMusicArtworkProvider @Inject constructor(
    private val innertubeApi: InnertubeApi,
    private val matcher: ArtworkMatcher
) : ArtworkProvider {

    override val name: String = "YouTubeMusic"
    override val priority: Int = 3

    companion object {
        private const val TAG = "YouTubeMusicArtworkProvider"
        private val GOOGLE_IMAGE_DIM_REGEX = Regex("""=w\d+-h\d+[^=]*$""")
        private const val HIGH_RES_GOOGLE_DIM = "=w800-h800-l90-rj"
    }

    override suspend fun fetchArtwork(
        title: String,
        artist: String,
        album: String?
    ): ArtworkResult? = withContext(Dispatchers.IO) {
        val cleanArtist = matcher.cleanArtist(artist)
        val cleanTitle = matcher.cleanTitle(title, cleanArtist)
        if (cleanTitle.isBlank() && cleanArtist.isBlank()) return@withContext null

        val query = "$cleanArtist $cleanTitle".trim()
        try {
            val page = innertubeApi.search(query)
            if (page.songs.isEmpty()) return@withContext null

            for (cand in page.songs) {
                if (!matcher.isValidMatch(title, artist, cand.title, cand.artist)) {
                    continue
                }

                val rawThumb = cand.thumbnailUrl.takeIf { it.isNotBlank() } ?: continue
                val highResUrl = upgradeYouTubeThumbnail(rawThumb, cand.videoId)

                Log.d(TAG, "YouTube Music matched: '${cand.title}' by '${cand.artist}' -> $highResUrl")
                return@withContext ArtworkResult(
                    url = highResUrl,
                    provider = name,
                    width = 800,
                    height = 800
                )
            }

            null
        } catch (e: Exception) {
            Log.d(TAG, "YouTube Music search failed for '$query': ${e.message}")
            null
        }
    }

    private fun upgradeYouTubeThumbnail(rawUrl: String, videoId: String): String {
        return when {
            rawUrl.contains("googleusercontent.com") -> {
                if (GOOGLE_IMAGE_DIM_REGEX.containsMatchIn(rawUrl)) {
                    rawUrl.replace(GOOGLE_IMAGE_DIM_REGEX, HIGH_RES_GOOGLE_DIM)
                } else if (rawUrl.contains("=")) {
                    rawUrl.substringBeforeLast("=") + HIGH_RES_GOOGLE_DIM
                } else {
                    "$rawUrl$HIGH_RES_GOOGLE_DIM"
                }
            }
            rawUrl.contains("i.ytimg.com") && videoId.isNotBlank() -> {
                "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
            }
            else -> rawUrl.replace(Regex("""w\d+-h\d+"""), "w800-h800")
        }
    }
}
