package com.example.myplayer.data.recommendation.ai

import android.content.Context
import android.util.Log
import com.example.myplayer.data.local.entity.SongEntity
import com.example.myplayer.data.online.InnertubeApi
import com.example.myplayer.data.online.model.OnlineSong
import com.example.myplayer.data.recommendation.model.RecommendationSong
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * 128-Dimension On-Device Neural Hash Embedder + Contextual Bandit RL Engine.
 *
 * - Memory Footprint: ~2.5 KB RAM (5 vectors x 128 floats)
 * - Storage Footprint: Bounded at max 80 pattern entries (<15 KB on disk forever, O(1) speed)
 * - Zero hardcoded genres, languages, or countries.
 */
@Singleton
class OnDeviceMusicAiModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val innertubeApi: InnertubeApi
) {
    companion object {
        private const val TAG = "OnDeviceMusicAI"
        private const val DIM = 128
        private const val MAX_HISTORY_ENTRIES = 80
        private const val PREFS_NAME = "on_device_music_ai_brain"

        private val STOP_WORDS = setOf(
            "official", "video", "audio", "full", "song", "songs", "lyrics", "lyrical",
            "hd", "4k", "mv", "feat", "ft", "the", "and", "with", "from", "music", "version"
        )
    }

    private data class PatternEntry(
        val videoId: String,
        val title: String,
        val artist: String,
        val durationMs: Long,
        val score: Float,
        val timestamp: Long
    )

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private var globalUserVector: FloatArray = loadVector("vec_global")
    private val timeSlotVectors: Array<FloatArray> = Array(4) { slot ->
        loadVector("vec_slot_$slot")
    }

    // Track last song start time to auto-detect early skips (<22s) vs full listens (>=55s)
    private var lastStartedVideoId: String? = null
    private var lastStartedTitle: String = ""
    private var lastStartedArtist: String = ""
    private var lastStartedDurationMs: Long = 0L
    private var lastStartedTimestampMs: Long = 0L

    /**
     * Call whenever a new song starts playing (online or offline).
     * Automatically evaluates how long the user listened to the PREVIOUS song:
     * - Played < 22s -> Negative RL Penalty (-0.75f)
     * - Played >= 55s -> Positive RL Reward (+1.15f)
     */
    @Synchronized
    fun onSongTransition(videoId: String, title: String, artist: String, durationMs: Long = 0L) {
        val cleanId = videoId.removePrefix("online://").trim()
        if (cleanId.isBlank() || title.isBlank()) return

        val now = System.currentTimeMillis()
        val prevId = lastStartedVideoId
        if (prevId != null && prevId != cleanId && lastStartedTimestampMs > 0L) {
            val listenedMs = now - lastStartedTimestampMs
            when {
                listenedMs in 1_500L..22_000L -> {
                    // User skipped previous song quickly -> Negative RL penalty!
                    Log.d(TAG, "RL Penalty (-0.75) for early skip: $lastStartedTitle (${listenedMs}ms)")
                    learnFromInteraction(prevId, lastStartedTitle, lastStartedArtist, lastStartedDurationMs, reward = -0.75f)
                }
                listenedMs >= 55_000L -> {
                    // User listened to previous song deeply -> Strong positive RL reward!
                    Log.d(TAG, "RL Reward (+1.15) for deep listen: $lastStartedTitle (${listenedMs}ms)")
                    learnFromInteraction(prevId, lastStartedTitle, lastStartedArtist, lastStartedDurationMs, reward = 1.15f)
                }
            }
        }

        lastStartedVideoId = cleanId
        lastStartedTitle = title
        lastStartedArtist = artist
        lastStartedDurationMs = durationMs
        lastStartedTimestampMs = now

        // Initial play intent signal (+0.45f)
        learnFromInteraction(cleanId, title, artist, durationMs, reward = 0.45f)
    }

    /**
     * Call when user favorites or downloads a song (Strongest positive signal: +1.8f).
     */
    @Synchronized
    fun onPositiveFeedback(videoId: String, title: String, artist: String, durationMs: Long = 0L) {
        val cleanId = videoId.removePrefix("online://").trim()
        if (cleanId.isBlank() || title.isBlank()) return
        learnFromInteraction(cleanId, title, artist, durationMs, reward = 1.80f)
    }

    /**
     * Syncs local Favorites, Most Played, and Downloaded songs into the AI brain on startup.
     */
    @Synchronized
    fun syncLibrarySignals(songs: List<SongEntity>, favoriteIds: Set<String> = emptySet()) {
        if (songs.isEmpty()) return
        val existing = loadHistoryEntries().associateBy { it.videoId }.toMutableMap()
        var changed = false

        songs.take(30).forEach { s ->
            val cleanId = (s.videoId ?: s.id).removePrefix("online://").trim()
            if (cleanId.isNotBlank() && s.title.isNotBlank() && !existing.containsKey(cleanId)) {
                val isFav = favoriteIds.contains(s.id) || (s.videoId != null && favoriteIds.contains(s.videoId))
                val baseReward = 0.6f + (s.playCount.coerceAtMost(10) * 0.25f) + (if (isFav) 1.2f else 0f)
                val vec = embedSong(s.title, s.artist, s.duration)
                for (i in 0 until DIM) {
                    globalUserVector[i] += 0.10f * baseReward * vec[i]
                }
                existing[cleanId] = PatternEntry(
                    videoId = cleanId,
                    title = s.title,
                    artist = s.artist,
                    durationMs = s.duration,
                    score = baseReward,
                    timestamp = s.dateAdded.takeIf { it > 0L } ?: System.currentTimeMillis()
                )
                changed = true
            }
        }

        if (changed) {
            globalUserVector = l2Normalize(globalUserVector)
            saveVector("vec_global", globalUserVector)
            saveHistoryEntries(existing.values.toList())
        }
    }

    /**
     * Core Online Reinforcement Learning Update:
     * Updates both the 128-D Global Vector & Current Time-of-Day Vector,
     * and maintains the bounded Top-80 Pattern Memory.
     */
    @Synchronized
    fun learnFromInteraction(
        videoId: String,
        title: String,
        artist: String,
        durationMs: Long,
        reward: Float
    ) {
        val songVec = embedSong(title, artist, durationMs)
        val slot = currentTimeSlot()
        val lr = 0.18f * reward

        for (i in 0 until DIM) {
            globalUserVector[i] = (globalUserVector[i] * 0.985f) + (lr * songVec[i])
            timeSlotVectors[slot][i] = (timeSlotVectors[slot][i] * 0.980f) + (lr * 1.25f * songVec[i])
        }

        globalUserVector = l2Normalize(globalUserVector)
        timeSlotVectors[slot] = l2Normalize(timeSlotVectors[slot])

        saveVector("vec_global", globalUserVector)
        saveVector("vec_slot_$slot", timeSlotVectors[slot])

        // Update bounded pattern memory (Max 80 items -> O(1) constant size forever!)
        val cleanId = videoId.removePrefix("online://").trim()
        val entries = loadHistoryEntries().toMutableList()
        val idx = entries.indexOfFirst { it.videoId == cleanId }
        val now = System.currentTimeMillis()

        if (idx != -1) {
            val old = entries[idx]
            val newScore = (old.score + reward).coerceIn(-2.0f, 25.0f)
            if (newScore <= -0.5f) {
                // Prune disliked/repeatedly-skipped songs from seed memory!
                entries.removeAt(idx)
            } else {
                entries[idx] = old.copy(
                    title = title.ifBlank { old.title },
                    artist = artist.ifBlank { old.artist },
                    score = newScore,
                    timestamp = now
                )
            }
        } else if (reward > 0f) {
            entries.add(0, PatternEntry(cleanId, title, artist, durationMs, reward, now))
        }

        saveHistoryEntries(entries)
    }

    /**
     * Generates 100% personalized recommendations by combining:
     * 1. Collaborative Graph Radio on top affinity seeds
     * 2. Learned Artist & Style Keyword clusters
     * 3. 128-D Neural Cosine Similarity + Time-of-Day Contextual Bandit ranking
     */
    suspend fun generateRecommendations(limit: Int = 45): List<RecommendationSong> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val entries = loadHistoryEntries()

        // Rank seeds by (Affinity Score * Exponential Recency Decay)
        val rankedEntries = entries.sortedByDescending { e ->
            val ageHours = ((now - e.timestamp).coerceAtLeast(0L) / (1000.0 * 3600.0))
            val recency = exp(-ageHours / 72.0).coerceIn(0.25, 1.0)
            e.score * recency
        }

        val topSeeds = rankedEntries
            .map { it.videoId }
            .filter { it.matches(Regex("^[a-zA-Z0-9_-]{11}$")) }
            .distinct()
            .take(4)

        // Learn top artists & recurring title vibe keywords dynamically
        val artistScores = mutableMapOf<String, Double>()
        val keywordScores = mutableMapOf<String, Double>()

        rankedEntries.forEach { e ->
            if (e.score <= 0f) return@forEach
            e.artist.split(",", "&", "feat.", "ft.", "|", "/")
                .map { it.replace(Regex("(?i)-\\s*topic"), "").trim() }
                .filter { it.length >= 2 && !it.equals("Unknown Artist", true) && !it.equals("YouTube Music", true) }
                .forEachIndexed { idx, art ->
                    val w = if (idx == 0) e.score.toDouble() else e.score * 0.5
                    artistScores[art] = (artistScores[art] ?: 0.0) + w
                }

            e.title.lowercase()
                .replace(Regex("[^a-z0-9\\s]"), " ")
                .split(Regex("\\s+"))
                .filter { it.length >= 3 && it !in STOP_WORDS }
                .forEach { token ->
                    keywordScores[token] = (keywordScores[token] ?: 0.0) + (e.score * 0.4)
                }
        }

        val topArtists = artistScores.entries.sortedByDescending { it.value }.map { it.key }.take(3)
        val topKeywords = keywordScores.entries.sortedByDescending { it.value }.map { it.key }.take(4)
        val topStyle = topKeywords.firstOrNull() ?: ""

        val candidatePool = java.util.concurrent.ConcurrentHashMap<String, OnlineSong>()
        val graphBonusMap = java.util.concurrent.ConcurrentHashMap<String, Float>()

        fun registerCandidate(song: OnlineSong, graphBonus: Float) {
            if (song.videoId.isBlank()) return
            candidatePool.putIfAbsent(song.videoId, song)
            graphBonusMap[song.videoId] = (graphBonusMap[song.videoId] ?: 0f) + graphBonus
        }

        // Fetch candidates in parallel across top seeds & learned artist patterns
        coroutineScope {
            val jobs = buildList {
                // 1. Collaborative Graph Radio for user's top seed videoIds
                topSeeds.forEachIndexed { idx, seedId ->
                    add(async {
                        try {
                            val radio = innertubeApi.getUpNext(seedId)
                            val seedWeight = 8.0f / (idx + 1)
                            radio.forEachIndexed { rankIdx, track ->
                                val posFactor = (1.0f - (rankIdx.coerceAtMost(20) / 25.0f))
                                registerCandidate(track, seedWeight * posFactor)
                            }
                        } catch (_: Exception) {}
                    })
                }

                // 2. Learned Artist + Learned Vibe Keyword queries
                topArtists.forEachIndexed { idx, artist ->
                    add(async {
                        try {
                            val q = if (topStyle.isNotBlank() && idx % 2 == 1) "$artist $topStyle" else artist
                            val tracks = innertubeApi.search(q, filtered = true).songs
                            val artWeight = 5.0f / (idx + 1)
                            tracks.take(12).forEachIndexed { rankIdx, track ->
                                registerCandidate(track, artWeight * (1.0f - rankIdx * 0.04f))
                            }
                        } catch (_: Exception) {}
                    })
                }
            }
            jobs.awaitAll()
        }

        // Pure cold-start fallback ONLY if user has 0 history on fresh install
        if (candidatePool.isEmpty()) {
            try {
                innertubeApi.search("Trending Songs", filtered = true).songs.forEachIndexed { i, track ->
                    registerCandidate(track, 1.0f - i * 0.02f)
                }
            } catch (_: Exception) {}
        }

        val recentlyPlayedIds = rankedEntries.take(5).map { it.videoId }.toSet()

        // Final Ranking using 128-D Neural Cosine Similarity + Graph Overlap + Artist Affinity
        candidatePool.values
            .map { cand ->
                val neuralSim = predictAffinityScore(cand) // -1.0f to +1.0f
                val graphScore = graphBonusMap[cand.videoId] ?: 0f

                var artistBoost = 0f
                for ((art, w) in artistScores) {
                    if (cand.artist.contains(art, ignoreCase = true) || cand.title.contains(art, ignoreCase = true)) {
                        artistBoost += (ln(w + 1.0) * 3.0).toFloat()
                    }
                }

                var totalScore = (neuralSim * 14.0f) + graphScore + artistBoost
                if (cand.videoId in recentlyPlayedIds) {
                    totalScore *= 0.55f // Let fresh matching songs rise to the top
                }
                cand to totalScore
            }
            .sortedByDescending { it.second }
            .take(limit)
            .map { (online, _) ->
                RecommendationSong(
                    videoId = online.videoId,
                    title = online.title,
                    artist = online.artist,
                    thumbnailUrl = online.thumbnailUrl,
                    durationMs = online.durationMs,
                    source = "on_device_ai"
                )
            }
    }

    /**
     * Generates Mood/Category songs (e.g. Sad, Chill, Happy, Romance) tailored to the
     * user's learned artists and ranked by the 128-D AI Taste Vector.
     */
    suspend fun generateCategoryRecommendations(category: String, limit: Int = 35): List<RecommendationSong> = withContext(Dispatchers.IO) {
        val entries = loadHistoryEntries()
        val artistScores = mutableMapOf<String, Double>()
        entries.filter { it.score > 0f }.forEach { e ->
            val primary = e.artist.split(",", "&", "feat.", "ft.").first().trim()
            if (primary.length >= 2 && !primary.equals("Unknown Artist", true)) {
                artistScores[primary] = (artistScores[primary] ?: 0.0) + e.score
            }
        }

        val topArtists = artistScores.entries.sortedByDescending { it.value }.map { it.key }.take(3)
        val collected = mutableListOf<OnlineSong>()

        coroutineScope {
            if (topArtists.isNotEmpty()) {
                topArtists.map { artist ->
                    async {
                        try {
                            innertubeApi.search("$artist $category", filtered = true).songs.take(15)
                        } catch (_: Exception) { emptyList() }
                    }
                }.awaitAll().forEach { collected.addAll(it) }
            } else {
                try {
                    collected.addAll(innertubeApi.search("$category songs", filtered = true).songs)
                } catch (_: Exception) {}
            }
        }

        collected
            .distinctBy { it.videoId }
            .sortedByDescending { predictAffinityScore(it) }
            .take(limit)
            .map { online ->
                RecommendationSong(
                    videoId = online.videoId,
                    title = online.title,
                    artist = online.artist,
                    thumbnailUrl = online.thumbnailUrl,
                    durationMs = online.durationMs,
                    source = "ai_category_$category"
                )
            }
    }

    /**
     * Embeds (title, artist, duration) into a 128-D L2-normalized vector.
     */
    fun embedSong(title: String, artist: String, durationMs: Long = 0L): FloatArray {
        val vec = FloatArray(DIM)
        val cleanTitle = title.lowercase().replace(Regex("[^a-z0-9\\s]"), " ")
        val cleanArtist = artist.lowercase().replace(Regex("(?i)-\\s*topic"), "").trim()

        projectFeatures(vec, extractNGrams(cleanArtist, 3, 4), weight = 2.8f)
        projectFeatures(vec, extractNGrams(cleanTitle, 3, 5), weight = 1.6f)

        if (durationMs > 0L) {
            val durSec = durationMs / 1000L
            val durToken = when {
                durSec < 150 -> "dur_short"
                durSec <= 300 -> "dur_medium"
                else -> "dur_long"
            }
            projectFeatures(vec, listOf(durToken), weight = 0.5f)
        }
        return l2Normalize(vec)
    }

    fun predictAffinityScore(candidate: OnlineSong): Float {
        val candVec = embedSong(candidate.title, candidate.artist, candidate.durationMs)
        val slot = currentTimeSlot()
        val globalSim = cosineSimilarity(globalUserVector, candVec)
        val timeSim = cosineSimilarity(timeSlotVectors[slot], candVec)
        return (0.60f * globalSim) + (0.40f * timeSim)
    }

    private fun currentTimeSlot(): Int {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> 0  // Morning
            in 12..16 -> 1 // Afternoon
            in 17..21 -> 2 // Evening
            else -> 3      // Late Night
        }
    }

    private fun extractNGrams(text: String, minN: Int, maxN: Int): List<String> {
        val out = mutableListOf<String>()
        val words = text.split(Regex("\\s+")).filter { it.length >= 2 && it !in STOP_WORDS }
        out.addAll(words)
        val padded = " $text "
        for (n in minN..maxN) {
            if (padded.length >= n) {
                for (i in 0..(padded.length - n)) {
                    out.add(padded.substring(i, i + n))
                }
            }
        }
        return out
    }

    private fun projectFeatures(vec: FloatArray, features: List<String>, weight: Float) {
        for (f in features) {
            val h = f.hashCode()
            val idx = (h and 0x7FFFFFFF) % DIM
            val sign = if ((h shr 31) == 0) 1f else -1f
            vec[idx] += sign * weight
        }
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in 0 until DIM) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom > 1e-6f) (dot / denom) else 0f
    }

    private fun l2Normalize(v: FloatArray): FloatArray {
        var sumSq = 0f
        for (x in v) sumSq += x * x
        val norm = sqrt(sumSq)
        if (norm < 1e-6f) return v
        return FloatArray(v.size) { i -> v[i] / norm }
    }

    private fun saveVector(key: String, vec: FloatArray) {
        prefs.edit().putString(key, vec.joinToString(",") { "%.5f".format(Locale.US, it) }).apply()
    }

    private fun loadVector(key: String): FloatArray {
        val raw = prefs.getString(key, null) ?: return FloatArray(DIM)
        val parts = raw.split(",")
        if (parts.size != DIM) return FloatArray(DIM)
        return FloatArray(DIM) { i -> parts[i].toFloatOrNull() ?: 0f }
    }

    private fun loadHistoryEntries(): List<PatternEntry> {
        val raw = prefs.getString("pattern_entries", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.lines().mapNotNull { line ->
            val p = line.split("\t")
            if (p.size >= 6) {
                PatternEntry(
                    videoId = p[0],
                    title = p[1],
                    artist = p[2],
                    durationMs = p[3].toLongOrNull() ?: 0L,
                    score = p[4].toFloatOrNull() ?: 1f,
                    timestamp = p[5].toLongOrNull() ?: 0L
                )
            } else null
        }
    }

    private fun saveHistoryEntries(entries: List<PatternEntry>) {
        // Strictly capped at MAX_HISTORY_ENTRIES (80) so storage never grows beyond ~12 KB!
        val capped = entries
            .sortedByDescending { it.timestamp }
            .take(MAX_HISTORY_ENTRIES)
            .joinToString("\n") {
                "${it.videoId}\t${it.title.replace("\t", " ")}\t${it.artist.replace("\t", " ")}\t${it.durationMs}\t${"%.2f".format(Locale.US, it.score)}\t${it.timestamp}"
            }
        prefs.edit().putString("pattern_entries", capped).apply()
    }
}
