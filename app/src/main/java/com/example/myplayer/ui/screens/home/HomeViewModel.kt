package com.example.myplayer.ui.screens.home

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myplayer.data.local.dao.PlayedSongHistoryDao
import com.example.myplayer.data.local.datastore.SettingsDataStore
import com.example.myplayer.data.local.entity.SongEntity
import com.example.myplayer.data.online.InnertubeApi
import com.example.myplayer.data.online.model.OnlineSong
import com.example.myplayer.data.recommendation.RecommendationCoordinator
import com.example.myplayer.data.recommendation.api.LastFmRecommendationSource
import com.example.myplayer.data.recommendation.cache.RecommendationCache
import com.example.myplayer.data.recommendation.model.RecommendationSong
import com.example.myplayer.data.recommendation.state.RecommendationUserState
import com.example.myplayer.data.recommendation.state.RecommendationUserStateDetector
import com.example.myplayer.data.repository.DownloadRepository
import com.example.myplayer.data.repository.MusicRepository
import com.example.myplayer.data.repository.RecentHistoryRepository
import com.example.myplayer.playback.MusicController
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MusicRepository,
    val musicController: MusicController,
    private val settingsDataStore: SettingsDataStore,
    private val downloadRepository: DownloadRepository,
    private val recommendationCoordinator: RecommendationCoordinator,
    private val userStateDetector: RecommendationUserStateDetector,
    private val recentHistoryRepository: RecentHistoryRepository,
    private val playedSongHistoryDao: PlayedSongHistoryDao,
    private val innertubeApi: InnertubeApi,
    private val recommendationCache: RecommendationCache,
    private val lastFmRecommendationSource: LastFmRecommendationSource
) : ViewModel() {

    private val _userState = MutableStateFlow<RecommendationUserState?>(null)
    val userState: StateFlow<RecommendationUserState?> = _userState.asStateFlow()

    val selectedCategory = MutableStateFlow("All")

    private val _categoryRecommendations = MutableStateFlow<Map<String, List<RecommendationSong>>>(emptyMap())

    // Self-learning pattern recognition state (Zero hardcoded genres/languages!)
    private val _patternRecommendations = MutableStateFlow<List<RecommendationSong>>(emptyList())

    val recommendations: StateFlow<List<RecommendationSong>> = combine(
        selectedCategory,
        _patternRecommendations,
        _categoryRecommendations,
        recommendationCoordinator.queueState
    ) { category, patternList, catMap, liveQueue ->
        if (category == "All") {
            patternList.ifEmpty { liveQueue }
        } else {
            catMap[category] ?: emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoadingRecommendations = MutableStateFlow(true)
    val isLoadingRecommendations: StateFlow<Boolean> = _isLoadingRecommendations.asStateFlow()

    // Refresh control state: auto-refresh every 15 min, not constantly
    private var isRefreshingPattern = false
    private var lastPatternRefreshTime = 0L
    private val PATTERN_REFRESH_INTERVAL_MS = 15 * 60 * 1000L // 15 minutes

    // Stop-words ignored when learning vibe/style tokens from song titles
    private val stopWords = setOf(
        "official", "video", "audio", "full", "song", "songs", "lyrics", "lyrical",
        "hd", "4k", "mv", "feat", "ft", "the", "and", "with", "from", "music"
    )

    init {
        // Load persisted fallback from cache if available for immediate display
        viewModelScope.launch {
            val persisted = withContext(Dispatchers.IO) {
                recommendationCache.getPersistedFallback(limit = 30)
            }
            if (persisted.isNotEmpty() && _patternRecommendations.value.isEmpty()) {
                _patternRecommendations.value = persisted
                _isLoadingRecommendations.value = false
            }
        }

        // 1. Observe every song the user plays (online or offline) to continuously learn their pattern
        viewModelScope.launch {
            combine(
                musicController.currentSong,
                musicController.currentOnlineSong
            ) { local, online ->
                when {
                    online != null -> Triple(online.videoId, online.title, online.artist)
                    local != null -> Triple(local.videoId ?: local.id, local.title, local.artist)
                    else -> null
                }
            }.collect { played ->
                if (played != null && played.first.isNotBlank()) {
                    recordUserListeningPattern(played.first, played.second, played.third)
                    // Only trigger on first song if recommendation feed is currently empty
                    if (_patternRecommendations.value.isEmpty()) {
                        recognizePatternAndRecommend(force = false)
                    }
                }
            }
        }

        // 2. Initial pattern recognition on startup
        viewModelScope.launch {
            recognizePatternAndRecommend(force = false)
        }

        // 3. Periodic auto-refresh every 15 minutes (not constantly)
        viewModelScope.launch {
            while (isActive) {
                delay(PATTERN_REFRESH_INTERVAL_MS)
                recognizePatternAndRecommend(force = true)
            }
        }

        viewModelScope.launch {
            recommendations.collect { list ->
                if (list.isNotEmpty()) {
                    _isLoadingRecommendations.value = false
                }
            }
        }

        viewModelScope.launch {
            kotlinx.coroutines.delay(4000)
            _isLoadingRecommendations.value = false
        }
    }

    fun refreshTrendingMusic() {
        viewModelScope.launch {
            recognizePatternAndRecommend(force = true)
        }
    }

    /**
     * Records every played track (including online streams!) with play count & recency
     * so the pattern recognizer always knows the user's true taste profile.
     */
    private fun recordUserListeningPattern(rawId: String, title: String, artist: String) {
        val cleanId = rawId.removePrefix("online://").trim()
        if (cleanId.isBlank() || title.isBlank()) return
        val prefs = context.getSharedPreferences("user_taste_pattern_store", Context.MODE_PRIVATE)
        val existingRaw = prefs.getString("history_entries", "") ?: ""
        val entries = existingRaw.lines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split("\t")
                if (parts.size >= 5) {
                    PatternEntry(
                        videoId = parts[0],
                        title = parts[1],
                        artist = parts[2],
                        playCount = parts[3].toIntOrNull() ?: 1,
                        timestamp = parts[4].toLongOrNull() ?: 0L
                    )
                } else null
            }.toMutableList()

        val idx = entries.indexOfFirst { it.videoId == cleanId }
        val now = System.currentTimeMillis()
        if (idx != -1) {
            val old = entries[idx]
            entries[idx] = old.copy(
                title = title.ifBlank { old.title },
                artist = artist.ifBlank { old.artist },
                playCount = old.playCount + 1,
                timestamp = now
            )
        } else {
            entries.add(0, PatternEntry(cleanId, title, artist, 1, now))
        }

        val serialized = entries
            .sortedByDescending { it.timestamp }
            .take(80)
            .joinToString("\n") { "${it.videoId}\t${it.title.replace("\t", " ")}\t${it.artist.replace("\t", " ")}\t${it.playCount}\t${it.timestamp}" }

        prefs.edit().putString("history_entries", serialized).apply()
    }

    private data class PatternEntry(
        val videoId: String,
        val title: String,
        val artist: String,
        val playCount: Int,
        val timestamp: Long
    )

    private data class LearnedUserPattern(
        val seedVideoIds: List<String>,
        val artistWeights: Map<String, Double>,
        val styleKeywords: List<String>,
        val playedIds: Set<String>
    )

    /**
     * Analyzes all user listening signals (Online Stream History, Most Played,
     * Favorites, Downloads, Recent History, 15-day Played Songs History) to extract:
     * 1. Affinity-weighted seed videoIds
     * 2. Artist affinity distribution (exponential recency + playCount + favorite weight)
     * 3. Recurring title/vibe tokens
     */
    private suspend fun analyzeUserPattern(): LearnedUserPattern = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val prefs = context.getSharedPreferences("user_taste_pattern_store", Context.MODE_PRIVATE)
        val storedEntries = (prefs.getString("history_entries", "") ?: "")
            .lines()
            .mapNotNull { line ->
                val p = line.split("\t")
                if (p.size >= 5) PatternEntry(p[0], p[1], p[2], p[3].toIntOrNull() ?: 1, p[4].toLongOrNull() ?: 0L)
                else null
            }

        val mostPlayed = try { repository.getMostPlayedSongs().firstOrNull().orEmpty() } catch (_: Exception) { emptyList() }
        val favorites = try { repository.getFavoriteSongs().firstOrNull().orEmpty() } catch (_: Exception) { emptyList() }
        val downloads = try { downloadedEntities.firstOrNull().orEmpty() } catch (_: Exception) { emptyList() }
        val recentDb = try { recentHistoryRepository.getRecentHistoryEntries(limit = 20) } catch (_: Exception) { emptyList() }
        val playedDb = try {
            val cutoff = now - (15L * 24 * 60 * 60 * 1000)
            playedSongHistoryDao.getPlayedHistory(cutoff).firstOrNull().orEmpty()
        } catch (_: Exception) { emptyList() }

        val seedScores = mutableMapOf<String, Double>()
        val artistScores = mutableMapOf<String, Double>()
        val tokenScores = mutableMapOf<String, Double>()
        val playedIds = mutableSetOf<String>()

        fun processSignal(id: String, title: String, artist: String, weight: Double, timestamp: Long) {
            val cleanId = id.removePrefix("online://").trim()
            val ageHours = ((now - timestamp).coerceAtLeast(0L) / (1000.0 * 3600.0))
            // Exponential recency decay: recent songs weigh more, older songs still contribute to taste
            val recencyMultiplier = kotlin.math.exp(-ageHours / 72.0).coerceIn(0.25, 1.0)
            val finalScore = weight * recencyMultiplier

            if (cleanId.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
                playedIds.add(cleanId)
                seedScores[cleanId] = (seedScores[cleanId] ?: 0.0) + finalScore
            }

            // Learn primary & featured artists dynamically
            artist.split(",", "&", "feat.", "ft.", "|", "/")
                .map { it.replace(Regex("(?i)-\\s*topic"), "").trim() }
                .filter { it.length >= 2 && !it.equals("Unknown Artist", true) && !it.equals("YouTube Music", true) }
                .forEachIndexed { idx, art ->
                    val artWeight = if (idx == 0) finalScore else finalScore * 0.6
                    artistScores[art] = (artistScores[art] ?: 0.0) + artWeight
                }

            // Learn recurring vibe/genre/language tokens from titles
            title.lowercase()
                .replace(Regex("[^a-z0-9\\s]"), " ")
                .split(Regex("\\s+"))
                .filter { it.length >= 3 && it !in stopWords }
                .forEach { token ->
                    tokenScores[token] = (tokenScores[token] ?: 0.0) + (finalScore * 0.5)
                }
        }

        // 1. Online & local pattern store signals
        storedEntries.forEach { e ->
            processSignal(e.videoId, e.title, e.artist, weight = 3.0 + (e.playCount * 1.5), timestamp = e.timestamp)
        }
        // 2. 15-day Played Songs History (online + offline)
        playedDb.forEachIndexed { idx, p ->
            val w = 5.0 / (idx + 1).coerceAtLeast(1)
            processSignal(p.id, p.title, p.artist, weight = 4.0 + w, timestamp = p.playedAt)
        }
        // 3. Favorites (high affinity signal)
        favorites.forEach { s ->
            processSignal(s.videoId ?: s.id, s.title, s.artist, weight = 6.0 + s.playCount, timestamp = s.dateAdded)
        }
        // 4. Most played
        mostPlayed.forEach { s ->
            processSignal(s.videoId ?: s.id, s.title, s.artist, weight = 2.5 + (s.playCount * 2.0), timestamp = s.dateAdded)
        }
        // 5. Downloads
        downloads.forEach { s ->
            processSignal(s.videoId ?: s.id, s.title, s.artist, weight = 4.0, timestamp = s.dateAdded)
        }
        // 6. Recent DB IDs
        recentDb.forEachIndexed { index, entry ->
            val cleanId = entry.songId.removePrefix("online://").trim()
            if (cleanId.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
                seedScores[cleanId] = (seedScores[cleanId] ?: 0.0) + (5.0 / (index + 1))
            }
        }

        val sortedSeeds = seedScores.entries.sortedByDescending { it.value }.map { it.key }.take(5)
        val topTokens = tokenScores.entries.sortedByDescending { it.value }.map { it.key }.take(6)

        LearnedUserPattern(
            seedVideoIds = sortedSeeds,
            artistWeights = artistScores,
            styleKeywords = topTokens,
            playedIds = playedIds
        )
    }

    /**
     * Generates recommendations purely from the user's learned listening pattern:
     * - Queries YouTube's collaborative graph (getUpNext) for top seed videoIds
     * - Queries learned artist + style token combinations
     * - Scores & ranks candidates by Graph Co-Occurrence + Artist Affinity + Token Similarity
     *
     * Protected by 15-minute interval so the feed does not constantly refresh on every track play.
     */
    private suspend fun recognizePatternAndRecommend(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && _patternRecommendations.value.isNotEmpty() && (now - lastPatternRefreshTime) < PATTERN_REFRESH_INTERVAL_MS) {
            return
        }
        if (isRefreshingPattern) return
        isRefreshingPattern = true
        _isLoadingRecommendations.value = true
        try {
            val rankedRecommendations = withContext(Dispatchers.IO) {
                val pattern = analyzeUserPattern()
                val candidatePool = mutableMapOf<String, OnlineSong>()
                val candidateScores = mutableMapOf<String, Double>()

                fun addCandidate(song: OnlineSong, baseScore: Double) {
                    if (song.videoId.isBlank()) return
                    candidatePool.putIfAbsent(song.videoId, song)
                    // Co-occurrence boost: if multiple seeds recommend the same song, its score stacks!
                    candidateScores[song.videoId] = (candidateScores[song.videoId] ?: 0.0) + baseScore
                }

                // 1. Collaborative Graph Expansion (YouTube UpNext Radio on user's top seeds)
                // Automatically preserves exact language, genre, sub-culture & mood of the seed track!
                pattern.seedVideoIds.forEachIndexed { seedIdx, videoId ->
                    val seedPriority = 10.0 / (seedIdx + 1)
                    try {
                        val radioCluster = innertubeApi.getUpNext(videoId)
                        radioCluster.forEachIndexed { rankIdx, track ->
                            val positionWeight = 1.0 - (rankIdx.coerceAtMost(20) / 25.0)
                            addCandidate(track, baseScore = seedPriority * positionWeight)
                        }
                    } catch (_: Exception) {}
                }

                // 2. Learned Artist & Style Pattern Expansion
                val topArtists = pattern.artistWeights.entries
                    .sortedByDescending { it.value }
                    .map { it.key }
                    .take(4)

                val topStyle = pattern.styleKeywords.firstOrNull() ?: ""

                for ((idx, artist) in topArtists.withIndex()) {
                    try {
                        val query = if (topStyle.isNotBlank() && idx % 2 == 1) "$artist $topStyle" else artist
                        val searchTracks = innertubeApi.search(query, filtered = true).songs
                        searchTracks.take(12).forEachIndexed { rankIdx, track ->
                            addCandidate(track, baseScore = (6.0 / (idx + 1)) * (1.0 - rankIdx * 0.05))
                        }
                    } catch (_: Exception) {}
                }

                // Cold start fallback if user has 0 listening history
                if (candidatePool.isEmpty()) {
                    try {
                        val fallbackQueries = listOf("Trending Songs", "Top Hits")
                        for (q in fallbackQueries) {
                            val res = innertubeApi.search(q, filtered = true).songs
                            res.take(15).forEachIndexed { rankIdx, track ->
                                addCandidate(track, baseScore = 3.0 - (rankIdx * 0.1))
                            }
                        }
                    } catch (_: Exception) {}
                }

                // 3. Score every candidate against the user's learned artist & keyword profile
                candidatePool.values.forEach { cand ->
                    var score = candidateScores[cand.videoId] ?: 1.0

                    // Boost if candidate artist matches user's learned artist affinity map
                    for ((learnedArtist, weight) in pattern.artistWeights) {
                        if (cand.artist.contains(learnedArtist, ignoreCase = true) ||
                            cand.title.contains(learnedArtist, ignoreCase = true)
                        ) {
                            score += kotlin.math.ln(weight + 1.0) * 4.5
                        }
                    }

                    // Boost if candidate title shares learned vibe/style keywords
                    val lowerTitle = cand.title.lowercase()
                    for (kw in pattern.styleKeywords) {
                        if (lowerTitle.contains(kw)) {
                            score += 2.0
                        }
                    }

                    // Slight penalty if user already played this exact track heavily so new similar songs surface first
                    if (cand.videoId in pattern.playedIds) {
                        score *= 0.65
                    }

                    candidateScores[cand.videoId] = score
                }

                candidatePool.values
                    .sortedByDescending { candidateScores[it.videoId] ?: 0.0 }
                    .map { online ->
                        RecommendationSong(
                            videoId = online.videoId,
                            title = online.title,
                            artist = online.artist,
                            thumbnailUrl = online.thumbnailUrl,
                            durationMs = online.durationMs,
                            source = "pattern_recognized"
                        )
                    }
            }

            if (rankedRecommendations.isNotEmpty()) {
                _patternRecommendations.value = rankedRecommendations
                lastPatternRefreshTime = System.currentTimeMillis()
            }
        } finally {
            _isLoadingRecommendations.value = false
            isRefreshingPattern = false
        }
    }

    val userName: StateFlow<String> = settingsDataStore.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Alex Rivera")

    val isDarkMode: StateFlow<Boolean> = settingsDataStore.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun toggleDarkMode() {
        viewModelScope.launch {
            settingsDataStore.setDarkMode(!isDarkMode.value)
        }
    }

    private val downloadedEntities: Flow<List<SongEntity>> = downloadRepository.getAllDownloads()
        .map { list ->
            list.map { d ->
                SongEntity(
                    id = d.id,
                    title = d.title,
                    artist = d.artist,
                    album = d.album.ifBlank { "Downloaded" },
                    duration = d.durationMs,
                    path = d.localPath,
                    albumArt = d.thumbnailUrl,
                    dateAdded = d.downloadedAt,
                    videoId = d.id
                )
            }
        }

    val recentSongs: StateFlow<List<SongEntity>> = downloadedEntities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mostPlayed: StateFlow<List<SongEntity>> = repository.getMostPlayedSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteSongs: StateFlow<List<SongEntity>> = repository.getFavoriteSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSongs: StateFlow<List<SongEntity>> = downloadedEntities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val displayedDownloadedSongs: StateFlow<List<SongEntity>> = combine(
        selectedCategory,
        downloadedEntities
    ) { category, songs ->
        if (category == "All") {
            songs
        } else {
            val keywords = getGenreKeywords(category)
            songs.filter { song ->
                val text = "${song.title} ${song.artist} ${song.album}".lowercase()
                keywords.any { kw -> text.contains(kw) }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentSong = musicController.currentSong
    val isPlaying = musicController.isPlaying

    fun selectCategory(category: String) {
        selectedCategory.value = category
        if (category == "All") {
            _isLoadingRecommendations.value = false
            return
        }
        if (_categoryRecommendations.value.containsKey(category)) {
            _isLoadingRecommendations.value = false
            return
        }

        viewModelScope.launch {
            _isLoadingRecommendations.value = true
            try {
                val results = fetchCategoryRecommendations(category)
                _categoryRecommendations.update { it + (category to results) }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Failed to fetch category recommendations for $category", e)
            } finally {
                _isLoadingRecommendations.value = false
            }
        }
    }

    private suspend fun fetchCategoryRecommendations(category: String): List<RecommendationSong> = withContext(Dispatchers.IO) {
        val pattern = analyzeUserPattern()
        val topUserArtists = pattern.artistWeights.entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(3)

        val collected = mutableListOf<OnlineSong>()

        if (topUserArtists.isNotEmpty()) {
            // Combine user's OWN learned artists with the selected mood!
            for (artist in topUserArtists) {
                try {
                    val res = innertubeApi.search("$artist $category", filtered = true)
                    collected.addAll(res.songs.take(12))
                } catch (_: Exception) {}
            }
        } else if (pattern.seedVideoIds.isNotEmpty()) {
            // Use user's seed track radio if artist list isn't built yet
            for (seedId in pattern.seedVideoIds.take(2)) {
                try {
                    collected.addAll(innertubeApi.getUpNext(seedId))
                } catch (_: Exception) {}
            }
        } else {
            // Pure cold start (0 history at all)
            try {
                collected.addAll(innertubeApi.search("$category songs", filtered = true).songs)
            } catch (_: Exception) {}
        }

        collected.distinctBy { it.videoId }.map { online ->
            RecommendationSong(
                videoId = online.videoId,
                title = online.title,
                artist = online.artist,
                thumbnailUrl = online.thumbnailUrl,
                durationMs = online.durationMs,
                source = "pattern_$category"
            )
        }
    }

    private fun getGenreKeywords(category: String): List<String> = when (category.lowercase()) {
        "deep focus" -> listOf("focus", "ambient", "drone", "study", "lofi", "lo-fi", "instrumental", "calm", "binaural", "relax")
        "acoustic chill" -> listOf("acoustic", "guitar", "unplugged", "folk", "indie", "warm", "strings", "chill", "vocal")
        "late night" -> listOf("night", "synthwave", "electro", "retro", "midnight", "drive", "dark", "neon", "after hours")
        "discoveries" -> listOf("discover", "indie", "new", "fresh", "rising", "alt", "underground", "gems")
        "party" -> listOf("party", "dance", "club", "dj", "remix", "edm", "disco", "celebration", "fest", "bass", "electro", "house", "groove", "rave")
        "blues" -> listOf("blues", "soul", "jazz", "r&b", "rhythm", "acoustic", "guitar", "swing")
        "sad" -> listOf("sad", "slow", "cry", "broken", "heartbreak", "tear", "alone", "lonely", "dark", "pain", "melancholy", "grief")
        "hip hop" -> listOf("hip hop", "hiphop", "rap", "trap", "flow", "rhyme", "drank", "drill", "freestyle", "cypher", "mc")
        "chill" -> listOf("chill", "lofi", "lo-fi", "relax", "calm", "peace", "ambient", "meditation", "sleep", "soft", "breeze", "easy")
        "workout" -> listOf("workout", "gym", "fitness", "run", "running", "energy", "power", "motivation", "hard", "pump", "training", "beast")
        "pop" -> listOf("pop", "hit", "summer", "dance", "upbeat", "mainstream", "billboard", "chart", "star")
        else -> listOf(category.lowercase())
    }

    fun playCategory(category: String, onStartPlaying: () -> Unit = {}) {
        selectCategory(category)
        viewModelScope.launch {
            val cached = _categoryRecommendations.value[category]
            val songsToPlay = if (!cached.isNullOrEmpty()) {
                cached
            } else {
                try {
                    fetchCategoryRecommendations(category).also { fetched ->
                        if (fetched.isNotEmpty()) {
                            _categoryRecommendations.update { it + (category to fetched) }
                        }
                    }
                } catch (e: Exception) {
                    emptyList()
                }
            }
            if (songsToPlay.isNotEmpty()) {
                playRecommendedSongs(songsToPlay, 0)
                withContext(Dispatchers.Main) {
                    onStartPlaying()
                }
            }
        }
    }

    fun setUserName(name: String) {
        viewModelScope.launch {
            settingsDataStore.setUserName(name)
        }
    }

    fun playSong(songs: List<SongEntity>, startIndex: Int) {
        musicController.playSongs(songs, startIndex)
        val song = songs.getOrNull(startIndex) ?: return
        recordUserListeningPattern(song.videoId ?: song.id, song.title, song.artist)
        viewModelScope.launch { repository.incrementPlayCount(song.id) }
        viewModelScope.launch { repository.addRecentHistory(song.id) }
    }

    fun toggleFavorite(song: SongEntity, isCurrentlyFavorite: Boolean) {
        viewModelScope.launch { repository.toggleFavorite(com.example.myplayer.data.repository.PlayableSong.Local(song), isCurrentlyFavorite) }
    }

    fun playRecommendedSongs(songs: List<RecommendationSong>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        val clampedIndex = startIndex.coerceIn(0, songs.size - 1)
        val onlineSongs = songs.map { song ->
            OnlineSong(
                videoId = song.videoId,
                title = song.title,
                artist = song.artist,
                thumbnailUrl = song.thumbnailUrl,
                durationMs = song.durationMs,
                durationText = "${song.durationMs / 60000}:${"%02d".format((song.durationMs % 60000) / 1000)}",
                streamUrl = "online://${song.videoId}"
            )
        }
        musicController.playOnlineSongs(onlineSongs, clampedIndex)
        val selected = songs.getOrNull(clampedIndex)
        if (selected != null) {
            recordUserListeningPattern(selected.videoId, selected.title, selected.artist)
            viewModelScope.launch {
                repository.addRecentHistory(selected.videoId)
            }
        }
    }

    fun playRecommendedSong(song: RecommendationSong) {
        playRecommendedSongs(listOf(song), 0)
    }
}


