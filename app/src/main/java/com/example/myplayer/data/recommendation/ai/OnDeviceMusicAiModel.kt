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

    // 5-minute in-memory cache to prevent background recommendation churn during song transitions
    @Volatile private var cachedRecos: List<RecommendationSong> = emptyList()
    @Volatile private var lastRecoTimeMs: Long = 0L

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

    // ── User Taste Onboarding (Languages, Genres & Favorite Artists) ──────────
    fun hasCompletedTasteOnboarding(): Boolean = prefs.getBoolean("taste_onboarding_done", false)
    fun getSelectedLanguages(): Set<String> = prefs.getStringSet("taste_languages", emptySet()) ?: emptySet()
    fun getSelectedGenres(): Set<String> = prefs.getStringSet("taste_genres", emptySet()) ?: emptySet()
    fun getSelectedArtists(): Set<String> = prefs.getStringSet("taste_artists", emptySet()) ?: emptySet()

    /**
     * Saves user's chosen Languages, Genres & Artists and immediately imprints them
     * into the 128-D Neural User Vector so recommendations update in <1s!
     */
    @Synchronized
    fun saveUserTasteProfile(
        languages: Set<String>,
        genres: Set<String>,
        artists: Set<String>
    ) {
        prefs.edit()
            .putStringSet("taste_languages", languages)
            .putStringSet("taste_genres", genres)
            .putStringSet("taste_artists", artists)
            .putBoolean("taste_onboarding_done", true)
            .apply()

        // Imprint chosen artists & genres directly into the 128-D taste vector
        artists.forEach { artist ->
            val vec = embedSong(title = genres.joinToString(" "), artist = artist, durationMs = 210_000L)
            for (i in 0 until DIM) {
                globalUserVector[i] += 0.45f * vec[i]
            }
        }
        genres.forEach { genre ->
            val vec = embedSong(title = "$genre ${languages.joinToString(" ")}", artist = "", durationMs = 210_000L)
            for (i in 0 until DIM) {
                globalUserVector[i] += 0.30f * vec[i]
            }
        }
        globalUserVector = l2Normalize(globalUserVector)
        saveVector("vec_global", globalUserVector)

        // Clear cached recommendations so feed regenerates immediately
        cachedRecos = emptyList()
        lastRecoTimeMs = 0L
    }

    /**
     * Generates personalized recommendations driven by:
     * 1. User's explicitly selected Artists, Languages & Genres (Spotify-style onboarding)
     * 2. Spotify-synced seeds & listening history
     * 3. 128-D Neural Cosine Similarity + Max 2 songs per artist diversification
     */
    suspend fun generateRecommendations(
        limit: Int = 45,
        forceRefresh: Boolean = false
    ): List<RecommendationSong> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedRecos.isNotEmpty() && (now - lastRecoTimeMs) < 5 * 60 * 1000L) {
            return@withContext cachedRecos.take(limit)
        }

        val entries = loadHistoryEntries()
        val youtubeIdRegex = Regex("^[a-zA-Z0-9_-]{11}$")
        val chosenLanguages = getSelectedLanguages().toList()
        val chosenGenres = getSelectedGenres().toList()
        val chosenArtists = getSelectedArtists().toList()

        val rankedEntries = entries.sortedByDescending { e ->
            val ageHours = ((now - e.timestamp).coerceAtLeast(0L) / (1000.0 * 3600.0))
            val recency = exp(-ageHours / 96.0).coerceIn(0.30, 1.0)
            val isSpotifySeed = !e.videoId.matches(youtubeIdRegex)
            e.score * recency * (if (isSpotifySeed) 1.35 else 1.0)
        }

        val topSeeds = rankedEntries
            .map { it.videoId }
            .filter { it.matches(youtubeIdRegex) }
            .distinct()
            .take(2)

        val topSpotifyTracks = rankedEntries
            .filter { !it.videoId.matches(youtubeIdRegex) && it.title.isNotBlank() }
            .distinctBy { "${it.title.lowercase().trim()}_${it.artist.lowercase().trim()}" }
            .take(6)

        val artistScores = mutableMapOf<String, Double>()
        // Give user's explicitly selected artists the highest base affinity!
        chosenArtists.forEach { art -> artistScores[art] = 12.0 }
        rankedEntries.forEach { e ->
            if (e.score <= 0f) return@forEach
            e.artist.split(",", "&", "feat.", "ft.", "|", "/")
                .map { it.replace(Regex("(?i)-\\s*topic"), "").trim() }
                .filter { it.length >= 2 && !it.equals("Unknown Artist", true) }
                .forEachIndexed { idx, art ->
                    val w = if (idx == 0) e.score.toDouble() else e.score * 0.5
                    artistScores[art] = (artistScores[art] ?: 0.0) + w
                }
        }

        // Mix user-selected artists (shuffled for fresh variety every refresh) + learned top artists
        val queryArtists = (chosenArtists.shuffled().take(4) + artistScores.entries.sortedByDescending { it.value }.map { it.key })
            .distinct()
            .take(5)

        // Build Language + Genre discovery queries from user's explicit selection
        val tasteDiscoveryQueries = buildList {
            if (chosenLanguages.isNotEmpty() && chosenGenres.isNotEmpty()) {
                chosenLanguages.shuffled().take(2).forEach { lang ->
                    chosenGenres.shuffled().take(2).forEach { genre ->
                        add("Best $lang $genre songs hits")
                    }
                }
            } else if (chosenLanguages.isNotEmpty()) {
                chosenLanguages.shuffled().take(3).forEach { lang ->
                    add("Top $lang trending songs")
                }
            } else if (chosenGenres.isNotEmpty()) {
                chosenGenres.shuffled().take(3).forEach { genre ->
                    add("Top $genre songs hits")
                }
            }
        }.take(4)

        val candidatePool = java.util.concurrent.ConcurrentHashMap<String, OnlineSong>()
        val graphBonusMap = java.util.concurrent.ConcurrentHashMap<String, Float>()

        fun registerCandidate(song: OnlineSong, graphBonus: Float) {
            if (song.videoId.isBlank()) return
            candidatePool.putIfAbsent(song.videoId, song)
            graphBonusMap[song.videoId] = (graphBonusMap[song.videoId] ?: 0f) + graphBonus
        }

        coroutineScope {
            val jobs = buildList {
                // 1. User-Selected Artists + Learned Artists
                queryArtists.forEachIndexed { idx, artist ->
                    add(async {
                        try {
                            val isChosen = chosenArtists.any { it.equals(artist, ignoreCase = true) }
                            val langHint = chosenLanguages.firstOrNull() ?: ""
                            val q = if (isChosen) "$artist best songs" else "$artist $langHint songs".trim()
                            val tracks = innertubeApi.search(q, filtered = true).songs
                            val baseWeight = if (isChosen) 11.5f else (6.0f / (idx + 1))
                            tracks.take(8).forEachIndexed { rankIdx, track ->
                                registerCandidate(track, baseWeight * (1.0f - rankIdx * 0.06f))
                            }
                        } catch (_: Exception) {}
                    })
                }

                // 2. User-Selected Language + Genre mixes
                tasteDiscoveryQueries.forEachIndexed { idx, query ->
                    add(async {
                        try {
                            val tracks = innertubeApi.search(query, filtered = true).songs
                            tracks.take(10).forEachIndexed { rankIdx, track ->
                                registerCandidate(track, (9.0f - idx) * (1.0f - rankIdx * 0.05f))
                            }
                        } catch (_: Exception) {}
                    })
                }

                // 3. Spotify-seeded tracks
                topSpotifyTracks.forEachIndexed { idx, spEntry ->
                    add(async {
                        try {
                            val query = "${spEntry.title} ${spEntry.artist}".trim()
                            val matchedSong = innertubeApi.search(query, filtered = true).songs.firstOrNull()
                            if (matchedSong != null) {
                                registerCandidate(matchedSong, (12.0f - idx).coerceAtLeast(5.0f))
                            }
                        } catch (_: Exception) {}
                    })
                }

                // 4. Collaborative Radio on top played seeds
                topSeeds.forEachIndexed { idx, seedId ->
                    add(async {
                        try {
                            val radio = innertubeApi.getUpNext(seedId)
                            val seedWeight = 6.5f / (idx + 1)
                            radio.take(12).forEachIndexed { rankIdx, track ->
                                registerCandidate(track, seedWeight * (1.0f - rankIdx / 16.0f))
                            }
                        } catch (_: Exception) {}
                    })
                }
            }
            jobs.awaitAll()
        }

        if (candidatePool.isEmpty()) {
            try {
                val fallbackQuery = if (chosenLanguages.isNotEmpty()) {
                    "Top ${chosenLanguages.first()} songs hits"
                } else {
                    "Trending Songs Hits"
                }
                innertubeApi.search(fallbackQuery, filtered = true).songs.forEachIndexed { i, track ->
                    registerCandidate(track, 1.0f - i * 0.02f)
                }
            } catch (_: Exception) {}
        }

        val recentlyPlayedIds = entries.sortedByDescending { it.timestamp }.take(8).map { it.videoId }.toSet()

        val scoredCandidates = candidatePool.values
            .map { cand ->
                val neuralSim = predictAffinityScore(cand)
                val graphScore = graphBonusMap[cand.videoId] ?: 0f

                // Extra boost if candidate matches user's explicitly chosen artists
                val matchesChosenArtist = chosenArtists.any {
                    cand.artist.contains(it, ignoreCase = true) || cand.title.contains(it, ignoreCase = true)
                }
                val chosenArtistBoost = if (matchesChosenArtist) 6.5f else 0f

                var learnedArtistBoost = 0f
                for ((art, w) in artistScores) {
                    if (cand.artist.contains(art, ignoreCase = true)) {
                        learnedArtistBoost += (ln(w + 1.0) * 1.2).toFloat()
                    }
                }
                learnedArtistBoost = learnedArtistBoost.coerceAtMost(3.5f)

                var totalScore = (neuralSim * 8.0f) + graphScore + chosenArtistBoost + learnedArtistBoost
                if (cand.videoId in recentlyPlayedIds) {
                    totalScore *= 0.25f
                }
                cand to totalScore
            }
            .sortedByDescending { it.second }

        // Diversify: Max 2 songs per primary artist so the feed stays rich & varied!
        val diversified = mutableListOf<OnlineSong>()
        val overflow = mutableListOf<OnlineSong>()
        val artistCount = mutableMapOf<String, Int>()

        for ((cand, _) in scoredCandidates) {
            val primaryArtist = cand.artist
                .split(",", "&", "feat.", "ft.", "|", "/")
                .firstOrNull()
                ?.replace(Regex("(?i)-\\s*topic"), "")
                ?.trim()
                ?.lowercase() ?: cand.artist.lowercase()

            val count = artistCount[primaryArtist] ?: 0
            if (count < 2) {
                diversified.add(cand)
                artistCount[primaryArtist] = count + 1
            } else {
                overflow.add(cand)
            }
        }

        val finalRanked = (diversified + overflow)
            .take(limit)
            .map { online ->
                RecommendationSong(
                    videoId = online.videoId,
                    title = online.title,
                    artist = online.artist,
                    thumbnailUrl = online.thumbnailUrl,
                    durationMs = online.durationMs,
                    source = "on_device_ai"
                )
            }

        if (finalRanked.isNotEmpty()) {
            cachedRecos = finalRanked
            lastRecoTimeMs = System.currentTimeMillis()
        }
        finalRanked
    }

    /**
     * Mood / Category / Hotlist recommendations tailored to User's Chosen Languages & Artists!
     */
    suspend fun generateCategoryRecommendations(category: String, limit: Int = 35): List<RecommendationSong> = withContext(Dispatchers.IO) {
        val chosenLanguages = getSelectedLanguages().toList()
        val chosenArtists = getSelectedArtists().shuffled().take(2)
        val queries = buildList {
            if (chosenLanguages.isNotEmpty()) {
                chosenLanguages.take(3).forEach { lang ->
                    add("$lang $category songs hits")
                }
            } else {
                add("Top $category songs hits")
                add("Best $category music playlist")
            }
            chosenArtists.forEach { artist ->
                add("$artist $category songs")
            }
        }

        val collected = mutableListOf<OnlineSong>()
        coroutineScope {
            queries.map { q ->
                async {
                    try {
                        innertubeApi.search(q, filtered = true).songs.take(12)
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
            }.awaitAll().forEach { collected.addAll(it) }
        }

        val artistCount = mutableMapOf<String, Int>()
        collected
            .distinctBy { it.videoId }
            .sortedByDescending { predictAffinityScore(it) }
            .filter { song ->
                val primary = song.artist.split(",", "&", "feat.", "ft.").first().trim().lowercase()
                val c = artistCount[primary] ?: 0
                if (c < 2) {
                    artistCount[primary] = c + 1
                    true
                } else false
            }
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
     * Generates the "Hotlist" (World Trending Songs tailored to User's Set Taste):
     * Combines Global Viral Charts + Trending Charts in User's Selected Languages & Genres
     * + Latest Viral Hits from User's Favorite Artists.
     */
    suspend fun generateWorldTrendingHotlist(limit: Int = 50): List<RecommendationSong> = withContext(Dispatchers.IO) {
        val chosenLanguages = getSelectedLanguages().toList()
        val chosenGenres = getSelectedGenres().toList()
        val chosenArtists = getSelectedArtists().toList()
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        // If user hasn't picked artists in onboarding yet, fallback to top 2 learned artists from history
        val historyArtists = loadHistoryEntries()
            .filter { it.score > 0f }
            .sortedByDescending { it.score }
            .map { it.artist.split(",", "&", "feat.", "ft.").first().trim() }
            .filter { it.length >= 2 && !it.equals("Unknown Artist", true) }
            .distinct()
            .take(2)

        val effectiveArtists = (chosenArtists.shuffled().take(3) + historyArtists).distinct().take(3)

        // Build World + Taste-Matched Trending Queries
        // Pair(query, baseChartWeight)
        val trendingQueries = buildList<Pair<String, Float>> {
            // 1. Language-Specific World Trending & Viral Charts (Highest Priority)
            if (chosenLanguages.isNotEmpty()) {
                chosenLanguages.shuffled().take(3).forEachIndexed { idx, lang ->
                    add("Top Trending $lang Songs $currentYear Viral Hits" to (14.0f - idx))
                    add("$lang Top 50 Chart Hits Right Now" to (12.5f - idx))
                }
            }

            // 2. Language + Genre Trending Charts (e.g., "Trending Punjabi Hip-Hop Hits 2026")
            if (chosenLanguages.isNotEmpty() && chosenGenres.isNotEmpty()) {
                val lang = chosenLanguages.random()
                chosenGenres.shuffled().take(2).forEach { genre ->
                    add("Trending $lang $genre Songs $currentYear" to 12.0f)
                }
            } else if (chosenGenres.isNotEmpty()) {
                chosenGenres.shuffled().take(2).forEach { genre ->
                    add("World Top Trending $genre Songs $currentYear" to 11.5f)
                }
            }

            // 3. Latest Trending / Viral Releases from User's Favorite Artists
            effectiveArtists.forEachIndexed { idx, artist ->
                add("$artist latest trending hit song $currentYear" to (11.0f - idx))
            }

            // 4. Global / Worldwide Viral Charts (So global monster hits are always included)
            add("Global Top 50 Trending Songs Worldwide $currentYear" to 10.5f)
            add("Viral Hits Worldwide Top Music Chart $currentYear" to 9.5f)
        }.take(8) // Cap parallel queries at 8 for fast loading

        val candidatePool = java.util.concurrent.ConcurrentHashMap<String, OnlineSong>()
        val trendingBonusMap = java.util.concurrent.ConcurrentHashMap<String, Float>()

        fun registerTrendingCandidate(song: OnlineSong, chartBonus: Float) {
            if (song.videoId.isBlank()) return
            candidatePool.putIfAbsent(song.videoId, song)
            // If a song appears in multiple trending charts, its viral score stacks!
            trendingBonusMap[song.videoId] = (trendingBonusMap[song.videoId] ?: 0f) + chartBonus
        }

        coroutineScope {
            trendingQueries.map { (query, baseWeight) ->
                async {
                    try {
                        val tracks = innertubeApi.search(query, filtered = true).songs
                        tracks.take(12).forEachIndexed { rankIdx, track ->
                            val rankFactor = (1.0f - (rankIdx * 0.055f)).coerceAtLeast(0.3f)
                            registerTrendingCandidate(track, baseWeight * rankFactor)
                        }
                    } catch (_: Exception) {}
                }
            }.awaitAll()
        }

        // Rank by (Trending Chart Viral Score + 128-D User Taste Affinity + Chosen Taste Match)
        val scored = candidatePool.values
            .map { cand ->
                val chartScore = trendingBonusMap[cand.videoId] ?: 0f
                val tasteSim = predictAffinityScore(cand) // -1.0f to +1.0f
                val matchesChosenArtist = chosenArtists.any {
                    cand.artist.contains(it, ignoreCase = true) || cand.title.contains(it, ignoreCase = true)
                }
                val artistMatchBonus = if (matchesChosenArtist) 4.0f else 0f
                val totalHotlistScore = chartScore + (tasteSim * 6.0f) + artistMatchBonus
                cand to totalHotlistScore
            }
            .sortedByDescending { it.second }

        // Artist Diversification: Max 2 songs per artist so Hotlist looks like a real Top 50 Chart!
        val diversified = mutableListOf<OnlineSong>()
        val overflow = mutableListOf<OnlineSong>()
        val artistCount = mutableMapOf<String, Int>()

        for ((cand, _) in scored) {
            val primaryArtist = cand.artist
                .split(",", "&", "feat.", "ft.", "|", "/")
                .firstOrNull()
                ?.replace(Regex("(?i)-\\s*topic"), "")
                ?.trim()
                ?.lowercase() ?: cand.artist.lowercase()

            val c = artistCount[primaryArtist] ?: 0
            if (c < 2) {
                diversified.add(cand)
                artistCount[primaryArtist] = c + 1
            } else {
                overflow.add(cand)
            }
        }

        (diversified + overflow)
            .take(limit)
            .map { online ->
                RecommendationSong(
                    videoId = online.videoId,
                    title = online.title,
                    artist = online.artist,
                    thumbnailUrl = online.thumbnailUrl,
                    durationMs = online.durationMs,
                    source = "world_hotlist_taste"
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
