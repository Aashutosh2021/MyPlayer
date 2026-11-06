package com.myplayer.app.data.lyrics

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Environment
import android.util.Log
import com.myplayer.app.data.local.dao.CachedLyricsDao
import com.myplayer.app.data.local.entity.CachedLyricsEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
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

@Singleton
class LyricsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val cachedLyricsDao: CachedLyricsDao
) {
    companion object {
        private const val TAG = "LyricsRepository"
        private const val BASE_URL = "https://lrclib.net/api"
        private const val USER_AGENT = "MyPlayer/1.0.0 (https://github.com/example/myplayer)"

        private val LRC_LINE_REGEX = Regex("""^\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?\]\s*(.*)$""")
        private val BRACKET_NOISE_REGEX = Regex("""[\(\[].*?[\)\]]""")
        private val KEYWORD_NOISE_REGEX = Regex(
            """(?i)\b(official|video|audio|lyrics?|lyrical|full song|song|hd|4k|mv|m/v|remix|reprise|cover|live|version|visualizer|color coded)\b"""
        )
        private val WHITESPACE_REGEX = Regex("""\s+""")
        private val FEAT_REGEX = Regex("""(?i)\b(feat\.?|ft\.?)\s+.*$""")
    }

    /**
     * Primary entry point:
     * 1. Checks Room SQLite cache (by exact songId OR cleaned Title + Artist).
     * 2. Checks local companion `.lrc` files on disk and embedded ID3/MP4 lyrics (works 100% offline!).
     * 3. Queries LRCLIB API when online and caches the result in Room + companion `.lrc`.
     */
    suspend fun fetchLyrics(
        songId: String,
        trackName: String,
        artistName: String,
        durationSeconds: Int = 0,
        albumName: String? = null,
        localPath: String? = null
    ): LyricsResult? = withContext(Dispatchers.IO) {
        val cleanId = songId.removePrefix("online://").removePrefix("spotify:track:").trim()
        val cleanArtist = cleanArtistName(artistName)
        val cleanTrack = cleanTrackTitle(trackName, cleanArtist)

        // 1. Check local SQLite cache first (Exact ID -> Clean ID -> Track + Artist metadata)
        try {
            val cached = cachedLyricsDao.getLyricsForSong(songId)
                ?: cachedLyricsDao.getLyricsForSong(cleanId)
                ?: cachedLyricsDao.getLyricsForSongOrMetadata(cleanId, trackName, artistName)
                ?: cachedLyricsDao.getLyricsForSongOrMetadata(cleanId, cleanTrack, cleanArtist)

            if (cached != null && (!cached.syncedLyrics.isNullOrBlank() || !cached.plainLyrics.isNullOrBlank())) {
                Log.d(TAG, "Loaded cached offline lyrics for: $trackName ($songId)")
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

        // 2. Offline Fallback: Check companion .lrc files & embedded audio tags
        readLocalOfflineLyrics(songId, trackName, artistName, localPath)?.let { localResult ->
            return@withContext localResult
        }

        // 3. Online Fetch from LRCLIB API
        val result = fetchLyricsFromLrcLib(trackName, artistName, durationSeconds, albumName)
        if (result != null) {
            try {
                cachedLyricsDao.insertLyrics(
                    CachedLyricsEntity(
                        songId = songId,
                        plainLyrics = result.plainLyrics,
                        syncedLyrics = result.syncedLyrics,
                        trackName = trackName.ifBlank { result.trackName },
                        artistName = artistName.ifBlank { result.artistName }
                    )
                )
                if (cleanId.isNotBlank() && cleanId != songId) {
                    cachedLyricsDao.insertLyrics(
                        CachedLyricsEntity(
                            songId = cleanId,
                            plainLyrics = result.plainLyrics,
                            syncedLyrics = result.syncedLyrics,
                            trackName = trackName.ifBlank { result.trackName },
                            artistName = artistName.ifBlank { result.artistName }
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to cache lyrics for $songId", e)
            }
        }

        return@withContext result
    }

    private suspend fun readLocalOfflineLyrics(
        songId: String,
        trackName: String,
        artistName: String,
        localPath: String?
    ): LyricsResult? {
        try {
            val candidateLrcFiles = mutableListOf<File>()

            if (!localPath.isNullOrBlank() && !localPath.startsWith("http")) {
                val cleanFilePath = if (localPath.startsWith("file://")) {
                    android.net.Uri.parse(localPath).path ?: localPath
                } else {
                    localPath
                }
                val audioFile = File(cleanFilePath)
                if (audioFile.exists()) {
                    audioFile.parentFile?.let { parent ->
                        candidateLrcFiles.add(File(parent, "${audioFile.nameWithoutExtension}.lrc"))
                    }
                }
            }

            // Also check app's Music download directory by safe title
            val musicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir
            val safeTitle = trackName
                .replace(Regex("[^a-zA-Z0-9 _\\-]"), "")
                .replace(Regex(" {2,}"), " ")
                .trim()
                .take(60)
            if (safeTitle.isNotBlank()) {
                candidateLrcFiles.add(File(musicDir, "$safeTitle.lrc"))
            }

            for (lrcFile in candidateLrcFiles) {
                if (lrcFile.exists() && lrcFile.length() > 0L) {
                    val lrcText = lrcFile.readText()
                    if (lrcText.isNotBlank()) {
                        Log.d(TAG, "Loaded offline companion .lrc file: ${lrcFile.absolutePath}")
                        return saveCustomLyrics(songId, trackName, artistName, lrcText)
                    }
                }
            }

            // Check embedded ID3/MP4 lyrics inside local audio file
            if (!localPath.isNullOrBlank() && !localPath.startsWith("http") && !localPath.startsWith("content://")) {
                val cleanFilePath = if (localPath.startsWith("file://")) {
                    android.net.Uri.parse(localPath).path ?: localPath
                } else {
                    localPath
                }
                val audioFile = File(cleanFilePath)
                if (audioFile.exists()) {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(audioFile.absolutePath)
                        val embedded = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LYRICS)
                        if (!embedded.isNullOrBlank()) {
                            Log.d(TAG, "Extracted embedded ID3 lyrics from ${audioFile.name}")
                            return saveCustomLyrics(songId, trackName, artistName, embedded)
                        }
                    } catch (_: Exception) {
                    } finally {
                        try { retriever.release() } catch (_: Exception) {}
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed reading local offline lyrics", e)
        }
        return null
    }

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

    suspend fun deleteLyrics(songId: String) = withContext(Dispatchers.IO) {
        try {
            cachedLyricsDao.deleteLyricsForSong(songId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete lyrics for $songId", e)
        }
    }

    private suspend fun fetchLyricsFromLrcLib(
        trackName: String,
        artistName: String,
        durationSeconds: Int = 0,
        albumName: String? = null
    ): LyricsResult? {
        val cleanArtist = cleanArtistName(artistName)
        val cleanTrack = cleanTrackTitle(trackName, cleanArtist)
        val coreTrack = cleanTrack.split(" - ", " – ", " — ", ":").first().trim().ifBlank { cleanTrack }
        val validAlbum = albumName?.takeIf {
            !it.equals("YouTube Music", ignoreCase = true) &&
                !it.equals("Online", ignoreCase = true) &&
                !it.equals("Recommended", ignoreCase = true) &&
                !it.equals("Downloads", ignoreCase = true) &&
                !it.equals("Unknown Album", ignoreCase = true)
        }

        if (cleanTrack.isNotBlank() && cleanArtist.isNotBlank() && durationSeconds > 0) {
            executeGetRequest(cleanTrack, cleanArtist, validAlbum, durationSeconds)?.let { return it }
        }
        if (cleanTrack.isNotBlank() && cleanArtist.isNotBlank()) {
            executeGetRequest(cleanTrack, cleanArtist, validAlbum, 0)?.let { return it }
        }
        if (cleanTrack.isNotBlank() && cleanArtist.isNotBlank()) {
            executeSearchWithParams(cleanTrack, cleanArtist, durationSeconds)?.let { return it }
        }
        val query = listOf(cleanArtist, coreTrack).filter { it.isNotBlank() }.joinToString(" ")
        if (query.isNotBlank()) {
            executeSearchQuery(query, coreTrack, cleanArtist, durationSeconds)?.let { return it }
        }
        if (coreTrack.isNotBlank()) {
            executeSearchQuery(coreTrack, coreTrack, "", durationSeconds)?.let { return it }
        }
        if (trackName.isNotBlank() && trackName != coreTrack) {
            val rawClean = trackName.replace(BRACKET_NOISE_REGEX, " ").trim()
            executeSearchQuery(rawClean, coreTrack.ifBlank { trackName }, cleanArtist, durationSeconds)?.let { return it }
        }
        return null
    }

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

            if (synced != null) {
                score += 0.15
            }

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
        return bestCandidate
    }

    fun cleanArtistName(raw: String): String {
        if (raw.isBlank()) return ""
        val labelRegex = Regex(
            """(?i)^(t-series|sony music.*|zee music.*|yrf|saregama.*|tips official.*|speed records.*|desi melodies.*|universal music.*|warner music.*|various artists|unknown artist)$"""
        )
        val primary = raw
            .replace(Regex("""(?i)\s*-\s*Topic\b"""), "")
            .replace(Regex("""(?i)VEVO\b"""), "")
            .replace(Regex("""(?i)\bOfficial\b"""), "")
            .split(",", "&", " feat.", " ft.", " x ", " X ")
            .firstOrNull()
            ?.replace(WHITESPACE_REGEX, " ")
            ?.trim() ?: ""

        return if (labelRegex.matches(primary)) "" else primary
    }

    fun cleanTrackTitle(raw: String, cleanArtist: String): String {
        if (raw.isBlank()) return ""
        var title = raw
        title = title.replace(BRACKET_NOISE_REGEX, " ")
        title = title.split('|', '•', '/').first()
        title = title.replace(KEYWORD_NOISE_REGEX, " ")
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
        title = title.replace(FEAT_REGEX, " ")
        return title.replace(WHITESPACE_REGEX, " ").trim()
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

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
