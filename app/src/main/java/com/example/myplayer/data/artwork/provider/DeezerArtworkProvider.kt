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
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeezerArtworkProvider @Inject constructor(
    private val baseOkHttpClient: OkHttpClient,
    private val matcher: ArtworkMatcher
) : ArtworkProvider {

    override val name: String = "Deezer"
    override val priority: Int = 1

    companion object {
        private const val TAG = "DeezerArtworkProvider"
        private const val BASE_URL = "https://api.deezer.com/search"
        private const val TIMEOUT_SECONDS = 2L
        private const val BACKOFF_DURATION_MS = 10 * 60_000L // 10 minute backoff on network failure or rate-limit
    }

    private val httpClient: OkHttpClient by lazy {
        baseOkHttpClient.newBuilder()
            .callTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    private val backoffUntilMs = AtomicLong(0L)

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

        // 1. Try advanced search: artist:"..." track:"..."
        val advQuery = "artist:\"$cleanArtist\" track:\"$cleanTitle\""
        val advResult = executeDeezerQuery(advQuery, title, artist)
        if (advResult != null) return@withContext advResult

        if (System.currentTimeMillis() < backoffUntilMs.get()) {
            return@withContext null
        }

        // 2. Try general search: "$cleanArtist $cleanTitle"
        val generalQuery = "$cleanArtist $cleanTitle".trim()
        if (generalQuery.isNotBlank()) {
            val generalResult = executeDeezerQuery(generalQuery, title, artist)
            if (generalResult != null) return@withContext generalResult
        }

        return@withContext null
    }

    private fun executeDeezerQuery(
        query: String,
        targetTitle: String,
        targetArtist: String
    ): ArtworkResult? {
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "$BASE_URL?q=$encodedQuery&limit=5"
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.code == 429) {
                    Log.w(TAG, "Deezer returned HTTP 429 Too Many Requests. Activating backoff.")
                    backoffUntilMs.set(System.currentTimeMillis() + BACKOFF_DURATION_MS)
                    return null
                }

                if (!response.isSuccessful) return null

                val body = response.body?.string() ?: return null
                val root = JSONObject(body)

                // Check API level error
                val errorObj = root.optJSONObject("error")
                if (errorObj != null) {
                    val errMsg = errorObj.optString("message", "")
                    if (errMsg.contains("limit", ignoreCase = true) || errMsg.contains("quota", ignoreCase = true)) {
                        backoffUntilMs.set(System.currentTimeMillis() + BACKOFF_DURATION_MS)
                    }
                    return null
                }

                val dataArray = root.optJSONArray("data") ?: return null
                if (dataArray.length() == 0) return null

                for (i in 0 until dataArray.length()) {
                    val item = dataArray.optJSONObject(i) ?: continue
                    val candTitle = item.optString("title", "")
                    val candArtist = item.optJSONObject("artist")?.optString("name", "") ?: ""

                    if (!matcher.isValidMatch(targetTitle, targetArtist, candTitle, candArtist)) {
                        continue
                    }

                    val albumObj = item.optJSONObject("album")
                    val coverXl = albumObj?.optString("cover_xl", "")?.takeIf { it.isNotBlank() }
                    val coverBig = albumObj?.optString("cover_big", "")?.takeIf { it.isNotBlank() }
                    val coverMedium = albumObj?.optString("cover_medium", "")?.takeIf { it.isNotBlank() }

                    val selectedUrl = coverXl ?: coverBig ?: coverMedium
                    if (selectedUrl != null) {
                        val width = if (selectedUrl == coverXl) 1000 else if (selectedUrl == coverBig) 500 else 250
                        Log.d(TAG, "Deezer matched: '$candTitle' by '$candArtist' -> $selectedUrl")
                        return ArtworkResult(
                            url = selectedUrl,
                            provider = name,
                            width = width,
                            height = width
                        )
                    }
                }

                null
            }
        } catch (e: Exception) {
            Log.d(TAG, "Deezer query failed for '$query': ${e.message}")
            if (e is java.io.IOException) {
                backoffUntilMs.set(System.currentTimeMillis() + BACKOFF_DURATION_MS)
            }
            null
        }
    }
}
