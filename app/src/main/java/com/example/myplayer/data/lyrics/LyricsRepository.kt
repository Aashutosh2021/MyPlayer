package com.example.myplayer.data.lyrics

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class LyricsResult(
    val plainLyrics: String?,
    val syncedLyrics: String?,   // LRC format: "[mm:ss.xx] line"
    val trackName: String,
    val artistName: String
)

sealed class LyricsState {
    object Idle : LyricsState()
    object Loading : LyricsState()
    data class Found(val result: LyricsResult) : LyricsState()
    data class NotFound(val reason: String) : LyricsState()
}

/**
 * Fetches lyrics from LRCLIB (https://lrclib.net) — a free, open-source lyrics API.
 * No API key required. Supports both plain and synced (LRC) lyrics.
 */
@Singleton
class LyricsRepository @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "LyricsRepository"
        private const val BASE_URL = "https://lrclib.net/api"
    }

    /**
     * Fetch lyrics for a song. Returns null if not found.
     * Tries synced first, falls back to plain.
     */
    suspend fun fetchLyrics(
        trackName: String,
        artistName: String,
        durationSeconds: Int = 0
    ): LyricsResult? = withContext(Dispatchers.IO) {
        try {
            val encodedTrack = java.net.URLEncoder.encode(trackName, "UTF-8")
            val encodedArtist = java.net.URLEncoder.encode(artistName, "UTF-8")

            val url = if (durationSeconds > 0) {
                "$BASE_URL/get?track_name=$encodedTrack&artist_name=$encodedArtist&duration=$durationSeconds"
            } else {
                "$BASE_URL/get?track_name=$encodedTrack&artist_name=$encodedArtist"
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "MyPlayer/1.0.0 (Android)")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.d(TAG, "Lyrics not found for '$trackName' by '$artistName' (HTTP ${response.code})")
                return@withContext null
            }

            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)

            val plain = json.optString("plainLyrics", "").takeIf { it.isNotBlank() }
            val synced = json.optString("syncedLyrics", "").takeIf { it.isNotBlank() }

            if (plain == null && synced == null) {
                Log.d(TAG, "Both plainLyrics and syncedLyrics are empty for '$trackName', trying search fallback")
                return@withContext searchLyrics(trackName, artistName)
            }

            LyricsResult(
                plainLyrics = plain,
                syncedLyrics = synced,
                trackName = json.optString("trackName", trackName),
                artistName = json.optString("artistName", artistName)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch lyrics for '$trackName' via /get, trying search fallback", e)
            searchLyrics(trackName, artistName)
        }
    }

    /**
     * Lenient fallback using LRCLIB's /search endpoint.
     * Useful when the exact /get match fails (e.g. messy YouTube-style titles).
     * Cleans the query and returns the first result that has lyrics.
     */
    private suspend fun searchLyrics(
        trackName: String,
        artistName: String
    ): LyricsResult? = withContext(Dispatchers.IO) {
        try {
            val cleanedTrack = cleanQueryText(trackName)
            val cleanedArtist = cleanQueryText(artistName)

            // Build a search query. If artist is unknown/blank, search by track only.
            val queryParts = listOf(cleanedTrack, cleanedArtist)
                .filter { it.isNotBlank() && !it.equals("Unknown Artist", ignoreCase = true) }
            if (queryParts.isEmpty()) return@withContext null

            val encodedQuery = java.net.URLEncoder.encode(queryParts.joinToString(" "), "UTF-8")
            val url = "$BASE_URL/search?q=$encodedQuery"

            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "MyPlayer/1.0.0 (Android)")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.d(TAG, "Search lyrics failed for '$trackName' (HTTP ${response.code})")
                return@withContext null
            }

            val body = response.body?.string() ?: return@withContext null
            val array = org.json.JSONArray(body)
            if (array.length() == 0) {
                Log.d(TAG, "No search results for '$trackName'")
                return@withContext null
            }

            // Prefer a result that has synced lyrics, otherwise the first with any lyrics.
            var best: LyricsResult? = null
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val plain = item.optString("plainLyrics", "").takeIf { it.isNotBlank() }
                val synced = item.optString("syncedLyrics", "").takeIf { it.isNotBlank() }
                if (plain == null && synced == null) continue

                val candidate = LyricsResult(
                    plainLyrics = plain,
                    syncedLyrics = synced,
                    trackName = item.optString("trackName", trackName),
                    artistName = item.optString("artistName", artistName)
                )
                if (synced != null) return@withContext candidate
                if (best == null) best = candidate
            }
            best
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search lyrics for '$trackName'", e)
            null
        }
    }

    /**
     * Cleans common noise from YouTube-style titles to improve lyric matching.
     * e.g. "Baarish (Official Video) [4K] | Yaariyan" -> "Baarish"
     */
    private fun cleanQueryText(text: String): String {
        if (text.isBlank()) return ""
        var cleaned = text
        // Remove bracketed/parenthesised segments
        cleaned = cleaned.replace(Regex("""[\(\[].*?[\)\]]"""), " ")
        // Drop anything after a separator like |, -, • (often "| Movie", "- Audio")
        cleaned = cleaned.split('|', '•').first()
        // Remove common noise keywords
        cleaned = cleaned.replace(
            Regex(
                """(?i)\b(official|video|audio|lyrics?|lyrical|full song|song|hd|4k|mv|m/v|remix|reprise|cover|live|version|feat\.?|ft\.?)\b"""
            ),
            " "
        )
        // Collapse whitespace
        return cleaned.replace(Regex("""\s+"""), " ").trim()
    }

    /**
     * Parse LRC synced lyrics into a list of (timestamp ms, line) pairs.
     * LRC format: "[01:23.45] lyric line"
     */
    fun parseSyncedLyrics(lrc: String): List<Pair<Long, String>> {
        return lrc.lines()
            .mapNotNull { line ->
                val match = Regex("""^\[(\d+):(\d+)\.(\d+)]\s*(.*)$""").matchEntire(line.trim())
                match?.let {
                    val mins = it.groupValues[1].toLongOrNull() ?: return@let null
                    val secs = it.groupValues[2].toLongOrNull() ?: return@let null
                    val centis = it.groupValues[3].toLongOrNull() ?: return@let null
                    val text = it.groupValues[4]
                    val ms = (mins * 60 + secs) * 1000 + centis * 10
                    Pair(ms, text)
                }
            }
            .sortedBy { it.first }
    }
}
