package com.example.myplayer.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myplayer.data.local.datastore.SettingsDataStore
import com.example.myplayer.data.local.entity.SongEntity
import com.example.myplayer.data.repository.DownloadRepository
import com.example.myplayer.data.repository.MusicRepository
import com.example.myplayer.playback.MusicController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import com.example.myplayer.data.recommendation.RecommendationCoordinator
import com.example.myplayer.data.recommendation.model.RecommendationSong
import com.example.myplayer.data.recommendation.state.RecommendationUserState
import com.example.myplayer.data.recommendation.state.RecommendationUserStateDetector
import com.example.myplayer.data.online.model.OnlineSong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.example.myplayer.data.online.InnertubeApi
import com.example.myplayer.data.recommendation.cache.RecommendationCache
import com.example.myplayer.data.recommendation.model.RecommendationSeed
import com.example.myplayer.data.repository.RecentHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MusicRepository,
    val musicController: MusicController,
    private val settingsDataStore: SettingsDataStore,
    private val downloadRepository: DownloadRepository,
    private val recommendationCoordinator: RecommendationCoordinator,
    private val userStateDetector: RecommendationUserStateDetector,
    private val recentHistoryRepository: RecentHistoryRepository,
    private val innertubeApi: InnertubeApi,
    private val recommendationCache: RecommendationCache
) : ViewModel() {

    private val _userState = MutableStateFlow<RecommendationUserState?>(null)
    val userState: StateFlow<RecommendationUserState?> = _userState.asStateFlow()

    val selectedCategory = MutableStateFlow("All")

    private val _categoryRecommendations = MutableStateFlow<Map<String, List<RecommendationSong>>>(emptyMap())

    // Persisted recommendations loaded from DB (shown while network fetch is in progress or offline)
    private val _persistedRecommendations = MutableStateFlow<List<RecommendationSong>>(emptyList())

    val recommendations: StateFlow<List<RecommendationSong>> = combine(
        selectedCategory,
        recommendationCoordinator.queueState,
        _categoryRecommendations,
        _persistedRecommendations
    ) { category, queue, catMap, persisted ->
        if (category == "All") {
            // Prefer live queue; fall back to persisted DB data when queue is empty
            queue.ifEmpty { persisted }
        } else {
            catMap[category] ?: emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoadingRecommendations = MutableStateFlow(true)
    val isLoadingRecommendations: StateFlow<Boolean> = _isLoadingRecommendations.asStateFlow()

    init {
        // Load persisted recommendations from DB first (instant, works offline)
        viewModelScope.launch {
            val persisted = withContext(Dispatchers.IO) {
                recommendationCache.getPersistedFallback(limit = 30)
            }
            if (persisted.isNotEmpty()) {
                _persistedRecommendations.value = persisted
                _isLoadingRecommendations.value = false
            }
        }

        viewModelScope.launch {
            loadInitialRecommendations()
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

    private suspend fun loadInitialRecommendations() {
        val state = userStateDetector.detectUserState()
        _userState.value = state

        if (recommendationCoordinator.queueState.value.isEmpty()) {
            try {
                val seed = if (state.isColdStart) {
                    RecommendationSeed.ColdStartSeed
                } else {
                    val recent = recentHistoryRepository.getRecentHistoryEntries(limit = 1).firstOrNull()
                    if (recent != null && recent.songId.isNotBlank()) {
                        RecommendationSeed(
                            songId = recent.songId,
                            title = "",
                            artist = "",
                            source = "youtube"
                        )
                    } else {
                        val firstDownload = downloadRepository.getAllDownloads().firstOrNull()?.firstOrNull()
                        if (firstDownload != null && firstDownload.id.isNotBlank()) {
                            RecommendationSeed(
                                songId = firstDownload.id,
                                title = firstDownload.title,
                                artist = firstDownload.artist,
                                source = "youtube"
                            )
                        } else {
                            RecommendationSeed.ColdStartSeed
                        }
                    }
                }
                recommendationCoordinator.prefetchRecommendations(seed)
            } catch (e: Exception) {
                _isLoadingRecommendations.value = false
            }
        } else {
            _isLoadingRecommendations.value = false
        }
    }

    val userName: StateFlow<String> = settingsDataStore.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Alex Rivera")

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

        val cached = _categoryRecommendations.value[category]
        if (cached != null && cached.size >= 30) {
            _isLoadingRecommendations.value = false
            return
        }

        viewModelScope.launch {
            _isLoadingRecommendations.value = true
            try {
                val songs = fetchGenreRecommendations(category)
                if (songs.isNotEmpty()) {
                    _categoryRecommendations.update { it + (category to songs) }
                }
            } catch (e: Exception) {
                // Fail gracefully
            } finally {
                _isLoadingRecommendations.value = false
            }
        }
    }

    private suspend fun fetchGenreRecommendations(category: String): List<RecommendationSong> = withContext(Dispatchers.IO) {
        val collected = mutableListOf<OnlineSong>()

        val primaryQuery = "$category songs"
        val firstPage = innertubeApi.search(primaryQuery)
        collected.addAll(firstPage.songs)

        if (collected.size < 30 && firstPage.continuationToken != null) {
            try {
                val secondPage = innertubeApi.search(primaryQuery, continuationToken = firstPage.continuationToken)
                collected.addAll(secondPage.songs)
            } catch (e: Exception) {
                // Ignore continuation failure
            }
        }

        if (collected.size < 30) {
            try {
                val complementaryQuery = when (category.lowercase()) {
                    "party" -> "Party club dance hits"
                    "blues" -> "Classic blues acoustic soul"
                    "sad" -> "Sad songs emotional heartbreak"
                    "hip hop" -> "Top hip hop rap hits"
                    "chill" -> "Chill lofi beats relax"
                    "workout" -> "Gym workout motivation music"
                    "pop" -> "Top pop hits global"
                    else -> "Best $category music hits"
                }
                val compPage = innertubeApi.search(complementaryQuery)
                collected.addAll(compPage.songs)
            } catch (e: Exception) {
                // Ignore
            }
        }

        val distinctSongs = collected.distinctBy { it.videoId }

        distinctSongs.map { online ->
            RecommendationSong(
                videoId = online.videoId,
                title = online.title,
                artist = online.artist,
                thumbnailUrl = online.thumbnailUrl,
                durationMs = online.durationMs,
                source = "genre_$category"
            )
        }
    }

    private fun getGenreKeywords(category: String): List<String> = when (category.lowercase()) {
        "party" -> listOf("party", "dance", "club", "dj", "remix", "edm", "disco", "celebration", "fest", "bass", "electro", "house", "groove", "rave")
        "blues" -> listOf("blues", "soul", "jazz", "r&b", "rhythm", "acoustic", "guitar", "swing")
        "sad" -> listOf("sad", "slow", "cry", "broken", "heartbreak", "tear", "alone", "lonely", "dark", "pain", "melancholy", "grief")
        "hip hop" -> listOf("hip hop", "hiphop", "rap", "trap", "flow", "rhyme", "drank", "drill", "freestyle", "cypher", "mc")
        "chill" -> listOf("chill", "lofi", "lo-fi", "relax", "calm", "peace", "ambient", "meditation", "sleep", "soft", "breeze", "easy")
        "workout" -> listOf("workout", "gym", "fitness", "run", "running", "energy", "power", "motivation", "hard", "pump", "training", "beast")
        "pop" -> listOf("pop", "hit", "summer", "dance", "upbeat", "mainstream", "billboard", "chart", "star")
        else -> listOf(category.lowercase())
    }

    fun setUserName(name: String) {
        viewModelScope.launch {
            settingsDataStore.setUserName(name)
        }
    }

    fun playSong(songs: List<SongEntity>, startIndex: Int) {
        musicController.playSongs(songs, startIndex)
        val song = songs.getOrNull(startIndex) ?: return
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
            viewModelScope.launch {
                repository.addRecentHistory(selected.videoId)
            }
        }
    }

    fun playRecommendedSong(song: RecommendationSong) {
        playRecommendedSongs(listOf(song), 0)
    }
}
