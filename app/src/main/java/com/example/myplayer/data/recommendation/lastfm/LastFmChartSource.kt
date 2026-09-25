package com.example.myplayer.data.recommendation.lastfm

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LastFmChartSource @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val config: LastFmConfig,
    private val cache: LastFmCache
) {
    companion object {
        private const val TAG = "LastFmChartSource"
        private const val CACHE_KEY_GLOBAL = "global_top_tracks"
    }

    suspend fun getTopTracks(limit: Int = 30): List<LastFmTrackMetadata> = withContext(Dispatchers.IO) {
        // 1. Check fresh cache
        val cached = cache.get(CACHE_KEY_GLOBAL)
        if (cached != null && cached.isNotEmpty()) {
            Log.d(TAG, "LASTFM_CACHE_HIT: Returning ${cached.size} cached tracks")
            return@withContext cached
        }

        // 2. Validate API key
        if (!config.hasValidKey()) {
            Log.w(TAG, "Last.fm API key missing, checking stale cache")
            return@withContext cache.getStale(CACHE_KEY_GLOBAL) ?: emptyList()
        }

        // 3. Network fetch
        try {
            val url = "${config.baseUrl}?method=chart.gettoptracks&api_key=${config.apiKey}&format=json&limit=$limit"
            Log.d(TAG, "LASTFM_REQUEST: Fetching top tracks from Last.fm API")

            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "MyPlayer/3.3 (Android)")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Last.fm HTTP error ${response.code}: falling back to stale cache")
                return@withContext cache.getStale(CACHE_KEY_GLOBAL) ?: emptyList()
            }

            val body = response.body?.string() ?: return@withContext cache.getStale(CACHE_KEY_GLOBAL) ?: emptyList()
            val parsedTracks = parseChartTracks(body)

            if (parsedTracks.isNotEmpty()) {
                cache.put(CACHE_KEY_GLOBAL, parsedTracks)
                Log.d(TAG, "LASTFM_REQUEST_SUCCESS: Fetched and cached ${parsedTracks.size} tracks")
                return@withContext parsedTracks
            } else {
                Log.w(TAG, "Last.fm response contained 0 valid tracks, attempting stale cache")
                return@withContext cache.getStale(CACHE_KEY_GLOBAL) ?: emptyList()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Last.fm request failed: ${e.message}. Using stale cache fallback if available.", e)
            return@withContext cache.getStale(CACHE_KEY_GLOBAL) ?: emptyList()
        }
    }

    private fun parseChartTracks(json: String): List<LastFmTrackMetadata> {
        val tracks = mutableListOf<LastFmTrackMetadata>()
        try {
            val root = JSONObject(json)

            // Check for API errors (e.g. Rate Limit 29)
            if (root.has("error")) {
                val errorCode = root.optInt("error")
                val message = root.optString("message")
                Log.w(TAG, "Last.fm API error ($errorCode): $message")
                return emptyList()
            }

            val tracksObj = root.optJSONObject("tracks") ?: return emptyList()
            val trackArray = tracksObj.optJSONArray("track") ?: return emptyList()

            for (i in 0 until trackArray.length()) {
                val item = trackArray.optJSONObject(i) ?: continue
                val name = item.optString("name").trim()
                if (name.isBlank()) continue

                val artistObj = item.optJSONObject("artist")
                val artist = artistObj?.optString("name")?.trim() ?: "Unknown Artist"

                val playcount = item.optString("playcount").toLongOrNull() ?: 0L
                val listeners = item.optString("listeners").toLongOrNull() ?: 0L
                val duration = item.optString("duration").toLongOrNull() ?: 0L
                val mbid = item.optString("mbid")

                // Extract highest resolution image
                var imageUrl = ""
                val images = item.optJSONArray("image")
                if (images != null && images.length() > 0) {
                    for (imgIdx in images.length() - 1 downTo 0) {
                        val img = images.optJSONObject(imgIdx)
                        val url = img?.optString("#text") ?: ""
                        if (url.isNotBlank()) {
                            imageUrl = url
                            break
                        }
                    }
                }

                tracks.add(
                    LastFmTrackMetadata(
                        name = name,
                        artist = artist,
                        playcount = playcount,
                        listeners = listeners,
                        durationSeconds = duration,
                        imageUrl = imageUrl,
                        mbid = mbid
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Last.fm chart JSON", e)
        }
        return tracks
    }
}
