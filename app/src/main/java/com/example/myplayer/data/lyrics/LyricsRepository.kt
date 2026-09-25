package com.example.myplayer.data.lyrics

import android.util.Log
import com.example.myplayer.data.local.dao.CachedLyricsDao
import com.example.myplayer.data.local.entity.CachedLyricsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
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
 * Fetches lyrics from LRCLIB (https://lrclib.net) — an open-source, community-driven lyrics API.
 * Uses both https://lrclib.net/api/get and https://lrclib.net/api/search with intelligent
 * title cleaning, multi-tier fallback, and local SQLite caching via Room.
 */
@Singleton
class LyricsRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val cachedLyricsDao: CachedLyricsDao
) {
    companion object {
        private const val TAG = "LyricsRepository"
        private const val BASE_URL = "https://lrclib.net/api"
        private const val USER_AGENT = "MyPlayer/1.0.0 (https://github.com/example/myplayer)"

        // Robust LRC line regex handling [mm:ss], [mm:ss.xx], [mm:ss.xxx]
        private val LRC_LINE_REGEX = Regex("""^\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?\]\s*(.*)$""")

        private val BRACKET_NOISE_REGEX = Regex("""[\(\[].*?[\)\]]""")
        private val KEYWORD_NOISE_REGEX = Regex(
            """(?i)\b(official|video|audio|lyrics?|lyrical|full song|song|hd|4k|mv|m/v|remix|reprise|cover|live|version|visualizer|color coded)\b"""
        )
        private val WHITESPACE_REGEX = Regex("""\s+""")
        private val FEAT_REGEX = Regex("""(?i)\b(feat\.?|ft\.?)\s+.*$""")
    }

    /**
     * Primary entry point: Retrieves lyrics for a song, checking local Room cache
     * first, then querying the LRCLIB API (/get and /search) and caching the result.
     */
    suspend fun fetchLyrics(
        songId: String,
        trackName: String,
        artistName: String,
        durationSeconds: Int = 0,
        albumName: String? = null
    ): LyricsResult? = withContext(Dispatchers.IO) {
        // 1. Check local SQLite cache first
        try {
            val cached = cachedLyricsDao.getLyricsForSong(songId)
            if (cached != null) {
                Log.d(TAG, "Loaded cached lyrics for: $trackName ($songId)")
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

        // 2. Cache miss: Fetch from LRCLIB API (/get & /search)
        val result = fetchLyricsFromLrcLib(trackName, artistName, durationSeconds, albumName)
        if (result != null) {
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
     * Saves user-provided custom lyrics directly to the Room cache for [songId].
     * Supports both plain text and timestamped LRC format.
     */
    suspend fun saveCustomLyrics(
        songId: String,
        trackName: String,
        artistName: String,
        lyricsText: String
    ): LyricsResult = withContext(Dispatchers.IO) {
        val trimmed = lyricsText.trim()
        val parsedSynced = parseSyncedLyrics(trimmed)
        val hasSynced = parsedSynced.isNotEmpty()

        val syncedLyrics = if (hasSynced) trimmed else null
        val plainLyrics = if (hasSynced) {
            trimmed.lines().joinToString("\n") { line ->
                val match = LRC_LINE_REGEX.matchEntire(line.trim())
                match?.groupValues?.get(4) ?: line
            }.trim()
        } else {
            trimmed
        }

        val entity = CachedLyricsEntity(
            songId = songId,
            plainLyrics = plainLyrics,
            syncedLyrics = syncedLyrics,
            trackName = trackName,
            artistName = artistName,
            cachedAt = System.currentTimeMillis()
        )

        try {
            cachedLyricsDao.insertLyrics(entity)
            Log.d(TAG, "Saved custom lyrics locally for: $trackName ($songId, synced=$hasSynced)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save custom lyrics for $songId", e)
        }

        LyricsResult(
            plainLyrics = plainLyrics,
            syncedLyrics = syncedLyrics,
            trackName = trackName,
            artistName = artistName
        )
    }

    /**
     * Deletes cached lyrics for [songId] so they can be re-fetched.
     */
    suspend fun deleteLyrics(songId: String) = withContext(Dispatchers.IO) {
        try {
            cachedLyricsDao.deleteLyricsForSong(songId)
            Log.d(TAG, "Deleted cached lyrics for $songId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete lyrics for $songId", e)
        }
    }

    /**
     * Multi-tier LRCLIB fetching:
     * Tier 1: /api/get (clean track & artist + duration)
     * Tier 2: /api/get (clean track & artist, no duration)
     * Tier 3: /api/search?track_name=...&artist_name=...
     * Tier 4: /api/search?q=cleanArtist cleanTrack
     * Tier 5: /api/search?q=rawTitle
     */
    private suspend fun fetchLyricsFromLrcLib(
        trackName: String,
        artistName: String,
        durationSeconds: Int = 0,
        albumName: String? = null
    ): LyricsResult? {
        val cleanArtist = cleanArtistName(artistName)
        val cleanTrack = cleanTrackTitle(trackName, cleanArtist)
        val validAlbum = albumName?.takeIf {
            !it.equals("YouTube Music", ignoreCase = true) &&
            !it.equals("Downloads", ignoreCase = true) &&
            !it.equals("Unknown Album", ignoreCase = true)
        }

        Log.d(TAG, "Fetching lyrics for track='$cleanTrack' (raw='$trackName'), artist='$cleanArtist' (raw='$artistName'), dur=${durationSeconds}s")

        // Tier 1: /api/get with duration (exact lookup)
        if (cleanTrack.isNotBlank() && cleanArtist.isNotBlank() && durationSeconds > 0) {
            val getResult = executeGetRequest(cleanTrack, cleanArtist, validAlbum, durationSeconds)
            if (getResult != null) return getResult
        }

        // Tier 2: /api/get without duration constraint
        if (cleanTrack.isNotBlank() && cleanArtist.isNotBlank()) {
            val getResult = executeGetRequest(cleanTrack, cleanArtist, validAlbum, 0)
            if (getResult != null) return getResult
        }

        // Tier 3: /api/search with specific track_name and artist_name
        if (cleanTrack.isNotBlank() && cleanArtist.isNotBlank()) {
            val searchParamResult = executeSearchWithParams(cleanTrack, cleanArtist, durationSeconds)
            if (searchParamResult != null) return searchParamResult
        }

        // Tier 4: /api/search with query "artist track"
        val query = listOf(cleanArtist, cleanTrack).filter { it.isNotBlank() }.joinToString(" ")
        if (query.isNotBlank()) {
            val searchResult = executeSearchQuery(query, cleanTrack, cleanArtist, durationSeconds)
            if (searchResult != null) return searchResult
        }

        // Tier 5: /api/search with raw trackName as fallback
        if (trackName.isNotBlank() && trackName != cleanTrack) {
            val rawClean = trackName.replace(BRACKET_NOISE_REGEX, " ").trim()
            val rawResult = executeSearchQuery(rawClean, cleanTrack.ifBlank { trackName }, cleanArtist, durationSeconds)
            if (rawResult != null) return rawResult
        }

        Log.d(TAG, "No lyrics found on LRCLIB for '$trackName'")
        return null
    }

    /**
     * Executes GET request against https://lrclib.net/api/get
     */
    private fun executeGetRequest(
        trackName: String,
        artistName: String,
        albumName: String?,
        durationSeconds: Int
    ): LyricsResult? {
        try {
            val params = mutableListOf(
                "track_name=${encode(trackName)}",
                "artist_name=${encode(artistName)}"
            )
            if (!albumName.isNullOrBlank()) {
                params.add("album_name=${encode(albumName)}")
            }
            if (durationSeconds > 0) {
                params.add("duration=$durationSeconds")
            }

            val url = "$BASE_URL/get?${params.joinToString("&")}"
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", USER_AGENT)
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return null
                    val json = JSONObject(body)
                    val plain = json.optString("plainLyrics", "").takeIf { it.isNotBlank() }
                    val synced = json.optString("syncedLyrics", "").takeIf { it.isNotBlank() }
                    if (plain != null || synced != null) {
                        val respTrack = json.optString("trackName", trackName)
                        val respArtist = json.optString("artistName", artistName)
                        Log.d(TAG, "LRCLIB /get matched: '$respTrack' by '$respArtist' (synced=${synced != null})")
                        return LyricsResult(
                            plainLyrics = plain,
                            syncedLyrics = synced,
                            trackName = respTrack,
                            artistName = respArtist
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "LRCLIB /get failed: ${e.message}")
        }
        return null
    }

    /**
     * Executes SEARCH request against https://lrclib.net/api/search?track_name=...&artist_name=...
     */
    private fun executeSearchWithParams(
        trackName: String,
        artistName: String,
        targetDurationSec: Int
    ): LyricsResult? {
        try {
            val url = "$BASE_URL/search?track_name=${encode(trackName)}&artist_name=${encode(artistName)}"
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", USER_AGENT)
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return null
                    return pickBestCandidate(JSONArray(body), trackName, artistName, targetDurationSec)
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "LRCLIB /search (params) failed: ${e.message}")
        }
        return null
    }

    /**
     * Executes SEARCH request against https://lrclib.net/api/search?q=...
     */
    private fun executeSearchQuery(
        query: String,
        targetTrack: String,
        targetArtist: String,
        targetDurationSec: Int
    ): LyricsResult? {
        try {
            val url = "$BASE_URL/search?q=${encode(query)}"
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", USER_AGENT)
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return null
                    return pickBestCandidate(JSONArray(body), targetTrack, targetArtist, targetDurationSec)
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "LRCLIB /search?q failed: ${e.message}")
        }
        return null
    }

    /**
     * Evaluates LRCLIB candidate array and selects the highest-scoring candidate.
     */
    private fun pickBestCandidate(
        array: JSONArray,
        targetTrack: String,
        targetArtist: String,
        targetDurationSec: Int
    ): LyricsResult? {
        if (array.length() == 0) return null

        var bestCandidate: LyricsResult? = null
        var highestScore = -1.0

        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val plain = item.optString("plainLyrics", "").takeIf { it.isNotBlank() }
            val synced = item.optString("syncedLyrics", "").takeIf { it.isNotBlank() }
            if (plain == null && synced == null) continue

            val candTrack = item.optString("trackName", item.optString("name", ""))
            val candArtist = item.optString("artistName", "")
            val candDuration = item.optDouble("duration", 0.0).toInt()

            var score = 0.0

            val titleSim = if (targetTrack.isNotBlank()) {
                maxOf(
                    normalizedSimilarity(targetTrack, candTrack),
                    if (candTrack.contains(targetTrack, ignoreCase = true) || targetTrack.contains(candTrack, ignoreCase = true)) 0.8 else 0.0
                )
            } else 0.5

            val artistSim = if (targetArtist.isNotBlank()) {
                maxOf(
                    normalizedSimilarity(targetArtist, candArtist),
                    if (candArtist.contains(targetArtist, ignoreCase = true) || targetArtist.contains(candArtist, ignoreCase = true)) 0.8 else 0.0
                )
            } else 0.5

            score += (titleSim * 0.5) + (artistSim * 0.3)

            // Bonus for synced lyrics
            if (synced != null) {
                score += 0.15
            }

            // Duration match bonus
            if (targetDurationSec > 0 && candDuration > 0) {
                val diff = Math.abs(targetDurationSec - candDuration)
                if (diff <= 5) score += 0.10
                else if (diff <= 15) score += 0.05
            }

            if (score > highestScore && score >= 0.45) {
                highestScore = score
                bestCandidate = LyricsResult(
                    plainLyrics = plain,
                    syncedLyrics = synced,
                    trackName = candTrack.ifBlank { targetTrack },
                    artistName = candArtist.ifBlank { targetArtist }
                )
            }
        }

        if (bestCandidate != null) {
            Log.d(TAG, "Selected best LRCLIB candidate: '${bestCandidate.trackName}' by '${bestCandidate.artistName}' (score=$highestScore, synced=${bestCandidate.syncedLyrics != null})")
        }
        return bestCandidate
    }

    /**
     * Cleans common YouTube channel noise from artist names.
     */
    fun cleanArtistName(raw: String): String {
        if (raw.isBlank()) return ""
        return raw
            .replace(Regex("""(?i)\s*-\s*Topic\b"""), "")
            .replace(Regex("""(?i)VEVO\b"""), "")
            .replace(Regex("""(?i)\bOfficial\b"""), "")
            .replace(WHITESPACE_REGEX, " ")
            .trim()
    }

    /**
     * Cleans common video title noise from YouTube-style track titles.
     */
    fun cleanTrackTitle(raw: String, cleanArtist: String): String {
        if (raw.isBlank()) return ""
        var title = raw
        // Strip parenthetical/bracketed descriptors
        title = title.replace(BRACKET_NOISE_REGEX, " ")
        // Strip delimiter suffixes
        title = title.split('|', '•', '/').first()
        // Strip common keywords
        title = title.replace(KEYWORD_NOISE_REGEX, " ")
        // Strip "Artist - Title" prefix if present
        val dashSeparators = listOf(" - ", " – ", " — ")
        for (sep in dashSeparators) {
            if (title.contains(sep)) {
                val parts = title.split(sep, limit = 2)
                val prefix = parts[0].trim().lowercase()
                val cleanA = cleanArtist.lowercase()
                if (cleanA.isNotBlank() && (prefix == cleanA || cleanA.contains(prefix) || prefix.contains(cleanA))) {
                    title = parts[1]
                    break
                }
            }
        }
        // Strip trailing featured artist clauses
        title = title.replace(FEAT_REGEX, " ")
        return title.replace(WHITESPACE_REGEX, " ").trim()
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    /**
     * Parse LRC synced lyrics into a sorted list of (timestamp ms, line text) pairs.
     * Supports [mm:ss], [mm:ss.xx], [mm:ss.xxx] formats.
     */
    fun parseSyncedLyrics(lrc: String): List<Pair<Long, String>> {
        return lrc.lines()
            .mapNotNull { line ->
                val match = LRC_LINE_REGEX.matchEntire(line.trim()) ?: return@mapNotNull null
                val mins = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
                val secs = match.groupValues[2].toLongOrNull() ?: return@mapNotNull null
                val fracStr = match.groupValues[3]
                val fracMs = when (fracStr.length) {
                    1 -> fracStr.toLong() * 100
                    2 -> fracStr.toLong() * 10
                    3 -> fracStr.toLong()
                    else -> 0L
                }
                val text = match.groupValues[4]
                val ms = (mins * 60 + secs) * 1000 + fracMs
                Pair(ms, text)
            }
            .sortedBy { it.first }
    }

    private fun normalizedSimilarity(s1: String, s2: String): Double {
        val a = s1.lowercase().trim()
        val b = s2.lowercase().trim()
        if (a == b) return 1.0
        val maxLength = maxOf(a.length, b.length)
        if (maxLength == 0) return 1.0
        val distance = levenshteinDistance(a, b)
        return 1.0 - (distance.toDouble() / maxLength)
    }

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
}
