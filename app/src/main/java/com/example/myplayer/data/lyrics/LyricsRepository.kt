package com.example.myplayer.data.lyrics

import android.util.Log
import com.example.myplayer.data.local.dao.CachedLyricsDao
import com.example.myplayer.data.local.entity.CachedLyricsEntity
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
 * Leverages local caching in SQLite via Room (CachedLyricsDao) and performs rigorous
 * metadata similarity verification to prevent incorrect lyrics matching.
 */
@Singleton
class LyricsRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val cachedLyricsDao: CachedLyricsDao
) {
    companion object {
        private const val TAG = "LyricsRepository"
        private const val BASE_URL = "https://lrclib.net/api"
        private val LRC_LINE_REGEX = Regex("""^\[(\d+):(\d+)\.(\d+)]\s*(.*)$""")
        private val BRACKET_NOISE_REGEX = Regex("""[\(\[].*?[\)\]]""")
        private val KEYWORD_NOISE_REGEX = Regex(
            """(?i)\b(official|video|audio|lyrics?|lyrical|full song|song|hd|4k|mv|m/v|remix|reprise|cover|live|version|feat\.?|ft\.?)\b"""
        )
        private val WHITESPACE_REGEX = Regex("""\s+""")
        
        // Confidence threshold for accepting a match (0.75 = 75% similarity)
        private const val CONFIDENCE_THRESHOLD = 0.75
    }

    /**
     * Primary entry point: Retrieves lyrics for a song, checking local Room cache
     * first, then querying the API and caching the result if matched securely.
     */
    suspend fun fetchLyrics(
        songId: String,
        trackName: String,
        artistName: String,
        durationSeconds: Int = 0,
        albumName: String? = null
    ): LyricsResult? = withContext(Dispatchers.IO) {
        // 1. Check local SQLite Cache first
        try {
            val cached = cachedLyricsDao.getLyricsForSong(songId)
            if (cached != null) {
                Log.d(TAG, "Instantly loaded cached lyrics for: $trackName ($songId)")
                return@withContext LyricsResult(
                    plainLyrics = cached.plainLyrics,
                    syncedLyrics = cached.syncedLyrics,
                    trackName = cached.trackName,
                    artistName = cached.artistName
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read cached lyrics for $songId", e)
        }

        // 2. Cache miss: Fetch from API
        val result = fetchLyricsFromApi(trackName, artistName, durationSeconds, albumName)
        if (result != null) {
            // Cache the newly fetched lyrics
            try {
                cachedLyricsDao.insertLyrics(
                    CachedLyricsEntity(
                        songId = songId,
                        plainLyrics = result.plainLyrics,
                        syncedLyrics = result.syncedLyrics,
                        trackName = result.trackName,
                        artistName = result.artistName
                    )
                )
                Log.d(TAG, "Cached lyrics locally for: $trackName ($songId)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to cache lyrics for $songId", e)
            }
        }

        return@withContext result
    }

    /**
     * Fetches and caches lyrics directly. Used during background music downloading.
     */
    suspend fun fetchAndCacheLyrics(
        songId: String,
        trackName: String,
        artistName: String,
        durationSeconds: Int = 0,
        albumName: String? = null
    ): Boolean {
        val result = fetchLyrics(songId, trackName, artistName, durationSeconds, albumName)
        return result != null
    }

    /**
     * Queries LRCLIB's /get and /search APIs with similarity verification.
     */
    private suspend fun fetchLyricsFromApi(
        trackName: String,
        artistName: String,
        durationSeconds: Int = 0,
        albumName: String? = null
    ): LyricsResult? {
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
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                val plain = json.optString("plainLyrics", "").takeIf { it.isNotBlank() }
                val synced = json.optString("syncedLyrics", "").takeIf { it.isNotBlank() }

                if (plain != null || synced != null) {
                    val respTrack = json.optString("trackName", "")
                    val respArtist = json.optString("artistName", "")
                    val respAlbum = json.optString("albumName", "")
                    val respDuration = json.optDouble("duration", 0.0).toInt()

                    // Verify result similarity
                    val score = calculateMetadataScore(
                        targetTitle = trackName,
                        targetArtist = artistName,
                        targetAlbum = albumName,
                        targetDurationSec = durationSeconds,
                        candidateTitle = respTrack,
                        candidateArtist = respArtist,
                        candidateAlbum = respAlbum,
                        candidateDurationSec = respDuration
                    )

                    if (score >= CONFIDENCE_THRESHOLD) {
                        return LyricsResult(
                            plainLyrics = plain,
                            syncedLyrics = synced,
                            trackName = respTrack.ifBlank { trackName },
                            artistName = respArtist.ifBlank { artistName }
                        )
                    } else {
                        Log.d(TAG, "Exact get match rejected due to low confidence: $score for '$respTrack'")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed /get fetch for $trackName, falling back to search", e)
        }

        // Fallback to searching and sorting by highest metadata similarity
        return searchLyrics(trackName, artistName, durationSeconds, albumName)
    }

    /**
     * Fuzzy search fallback on LRCLIB's /search endpoint.
     * Evaluates all candidates and returns the best match above our threshold.
     */
    private suspend fun searchLyrics(
        trackName: String,
        artistName: String,
        durationSeconds: Int = 0,
        albumName: String? = null
    ): LyricsResult? = withContext(Dispatchers.IO) {
        try {
            val cleanedTrack = cleanQueryText(trackName)
            val cleanedArtist = cleanQueryText(artistName)

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
            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val array = org.json.JSONArray(body)
            if (array.length() == 0) return@withContext null

            var bestCandidate: LyricsResult? = null
            var highestScore = 0.0

            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val plain = item.optString("plainLyrics", "").takeIf { it.isNotBlank() }
                val synced = item.optString("syncedLyrics", "").takeIf { it.isNotBlank() }
                if (plain == null && synced == null) continue

                val candTrack = item.optString("trackName", "")
                val candArtist = item.optString("artistName", "")
                val candAlbum = item.optString("albumName", "")
                val candDuration = item.optDouble("duration", 0.0).toInt()

                val score = calculateMetadataScore(
                    targetTitle = trackName,
                    targetArtist = artistName,
                    targetAlbum = albumName,
                    targetDurationSec = durationSeconds,
                    candidateTitle = candTrack,
                    candidateArtist = candArtist,
                    candidateAlbum = candAlbum,
                    candidateDurationSec = candDuration
                )

                if (score > highestScore) {
                    highestScore = score
                    bestCandidate = LyricsResult(
                        plainLyrics = plain,
                        syncedLyrics = synced,
                        trackName = candTrack,
                        artistName = candArtist
                    )
                }
            }

            if (highestScore >= CONFIDENCE_THRESHOLD) {
                Log.d(TAG, "Selected best search match with confidence $highestScore: ${bestCandidate?.trackName}")
                bestCandidate
            } else {
                Log.d(TAG, "Best search match rejected due to low confidence: $highestScore")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Search lyrics failed for $trackName", e)
            null
        }
    }

    /**
     * Cleans common noise from YouTube-style titles to improve search querying.
     */
    private fun cleanQueryText(text: String): String {
        if (text.isBlank()) return ""
        var cleaned = text
        cleaned = cleaned.replace(BRACKET_NOISE_REGEX, " ")
        cleaned = cleaned.split('|', '•').first()
        cleaned = cleaned.replace(KEYWORD_NOISE_REGEX, " ")
        return cleaned.replace(WHITESPACE_REGEX, " ").trim()
    }

    /**
     * Levenshtein distance calculations.
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length
        var dp = IntArray(len2 + 1) { it }
        var nextDp = IntArray(len2 + 1)

        for (i in 1..len1) {
            nextDp[0] = i
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                nextDp[j] = minOf(
                    dp[j] + 1,
                    nextDp[j - 1] + 1,
                    dp[j - 1] + cost
                )
            }
            val temp = dp
            dp = nextDp
            nextDp = temp
        }
        return dp[len2]
    }

    private fun normalizedSimilarity(s1: String, s2: String): Double {
        if (s1.lowercase().trim() == s2.lowercase().trim()) return 1.0
        val maxLength = maxOf(s1.length, s2.length)
        if (maxLength == 0) return 1.0
        val distance = levenshteinDistance(s1.lowercase().trim(), s2.lowercase().trim())
        return 1.0 - (distance.toDouble() / maxLength)
    }

    private fun calculateMetadataScore(
        targetTitle: String,
        targetArtist: String,
        targetAlbum: String?,
        targetDurationSec: Int,
        candidateTitle: String,
        candidateArtist: String,
        candidateAlbum: String?,
        candidateDurationSec: Int
    ): Double {
        val titleSim = normalizedSimilarity(targetTitle, candidateTitle)
        val artistSim = normalizedSimilarity(targetArtist, candidateArtist)
        
        // Duration score (tolerance ±3s gets 1.0, ±6s gets 0.5, >6s gets 0.0)
        val durationDiff = Math.abs(targetDurationSec - candidateDurationSec)
        val durationScore = when {
            targetDurationSec <= 0 || candidateDurationSec <= 0 -> 0.8
            durationDiff <= 3 -> 1.0
            durationDiff <= 6 -> 0.5
            else -> 0.0
        }

        val hasAlbumInfo = !targetAlbum.isNullOrBlank() && !candidateAlbum.isNullOrBlank()
        val albumScore = if (hasAlbumInfo) {
            normalizedSimilarity(targetAlbum!!, candidateAlbum!!)
        } else {
            1.0
        }

        return if (hasAlbumInfo) {
            (titleSim * 0.35) + (artistSim * 0.35) + (durationScore * 0.20) + (albumScore * 0.10)
        } else {
            (titleSim * 0.40) + (artistSim * 0.40) + (durationScore * 0.20)
        }
    }

    /**
     * Parse LRC synced lyrics into a list of (timestamp ms, line) pairs.
     */
    fun parseSyncedLyrics(lrc: String): List<Pair<Long, String>> {
        return lrc.lines()
            .mapNotNull { line ->
                val match = LRC_LINE_REGEX.matchEntire(line.trim())
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
