package com.example.myplayer.data.artwork.provider

import android.util.Log
import com.example.myplayer.data.artwork.matching.ArtworkMatcher
import com.example.myplayer.data.artwork.model.ArtworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ITunesArtworkProvider @Inject constructor(
    private val baseOkHttpClient: OkHttpClient,
    private val matcher: ArtworkMatcher
) : ArtworkProvider {

    override val name: String = "iTunes"
    override val priority: Int = 2

    companion object {
        private const val TAG = "ITunesArtworkProvider"
        private const val BASE_URL = "https://itunes.apple.com/search"
        private const val TIMEOUT_SECONDS = 2L
        private const val BACKOFF_DURATION_MS = 10 * 60_000L
        private val RESOLUTION_TAG_REGEX = Regex("""\b\d+x\d+bb\b""")
        private const val TARGET_RESOLUTION_TAG = "1000x1000bb"
    }

    private val httpClient: OkHttpClient by lazy {
        baseOkHttpClient.newBuilder()
            .callTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    private val backoffUntilMs = java.util.concurrent.atomic.AtomicLong(0L)

    override suspend fun fetchArtwork(
        title: String,
        artist: String,
        album: String?
    ): ArtworkResult? = withContext(Dispatchers.IO) {
        if (System.currentTimeMillis() < backoffUntilMs.get()) {
            return@withContext null
        }

        val cleanArtist = matcher.cleanArtist(artist)
        val cleanTitle = matcher.cleanTitle(title, cleanArtist)
        if (cleanTitle.isBlank() && cleanArtist.isBlank()) return@withContext null

        val query = "$cleanArtist $cleanTitle".trim()
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "$BASE_URL?term=$encodedQuery&entity=song&limit=5"
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null

                val body = response.body?.string() ?: return@withContext null
                val root = JSONObject(body)
                val results = root.optJSONArray("results") ?: return@withContext null
                if (results.length() == 0) return@withContext null

                for (i in 0 until results.length()) {
                    val item = results.optJSONObject(i) ?: continue
                    val candTrack = item.optString("trackName", "")
                    val candArtist = item.optString("artistName", "")

                    if (!matcher.isValidMatch(title, artist, candTrack, candArtist)) {
                        continue
                    }

                    val rawArtUrl = item.optString("artworkUrl100", "").takeIf { it.isNotBlank() }
                        ?: item.optString("artworkUrl60", "").takeIf { it.isNotBlank() }
                        ?: continue

                    // Upgrade resolution from 100x100 to 1000x1000
                    val highResUrl = if (RESOLUTION_TAG_REGEX.containsMatchIn(rawArtUrl)) {
                        rawArtUrl.replace(RESOLUTION_TAG_REGEX, TARGET_RESOLUTION_TAG)
                    } else {
                        rawArtUrl
                    }

                    Log.d(TAG, "iTunes matched: '$candTrack' by '$candArtist' -> $highResUrl")
                    return@withContext ArtworkResult(
                        url = highResUrl,
                        provider = name,
                        width = 1000,
                        height = 1000
                    )
                }

                null
            }
        } catch (e: Exception) {
            Log.d(TAG, "iTunes search failed for '$query': ${e.message}")
            if (e is java.io.IOException) {
                backoffUntilMs.set(System.currentTimeMillis() + BACKOFF_DURATION_MS)
            }
            null
        }
    }
}
