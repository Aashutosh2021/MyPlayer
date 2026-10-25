package com.example.myplayer.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myplayer.data.local.entity.PlaylistEntity
import com.example.myplayer.data.online.InnertubeApi
import com.example.myplayer.data.online.model.OnlineSong
import com.example.myplayer.data.repository.DownloadRepository
import com.example.myplayer.data.repository.HybridLibraryRepository
import com.example.myplayer.data.repository.MusicRepository
import com.example.myplayer.data.repository.PlayableSong
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val hybridLibraryRepository: HybridLibraryRepository,
    private val downloadRepository: DownloadRepository,
    private val innertubeApi: InnertubeApi
) : ViewModel() {

    private val _playlistId = MutableStateFlow(-1L)
    private val youtubeIdRegex = Regex("^[a-zA-Z0-9_-]{11}$")
    private var prewarmJob: Job? = null
    private val prewarmSemaphore = Semaphore(3)
    private val _resolvingIds = MutableStateFlow<Set<String>>(emptySet())
    val resolvingIds: StateFlow<Set<String>> = _resolvingIds.asStateFlow()

    val playlist: StateFlow<PlaylistEntity?> = _playlistId.flatMapLatest { id ->
        repository.getAllPlaylists().map { list -> list.find { it.id == id } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val songs: StateFlow<List<PlayableSong>> = _playlistId.flatMapLatest { id ->
        repository.getSongsInPlaylistAsPlayable(id)
    }.onEach { songList ->
        prewarmSpotifyPlaylistSongs(songList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** All songs available in the user's library (local + downloaded) to pick from */
    val libraryForPicker: StateFlow<List<PlayableSong>> =
        hybridLibraryRepository.getHybridLibrary()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setPlaylistId(id: Long) {
        _playlistId.value = id
    }

    /**
     * Background pre-resolver: Jaise hi user Spotify playlist open karega,
     * unresolving Spotify songs ke 11-char YouTube videoId resolve karke DB me save kar dega
     * aur starting songs ke stream URLs bhi cache kar lega taaki click karte hi 0-delay play ho!
     */
    private fun prewarmSpotifyPlaylistSongs(songList: List<PlayableSong>) {
        val unresolved = songList.mapNotNull { song ->
            when (song) {
                is PlayableSong.Online -> {
                    val candidate = song.id.removePrefix("online://").removePrefix("spotify:").trim()
                    if (!youtubeIdRegex.matches(candidate)) song else null
                }
                is PlayableSong.Local -> {
                    val candidate = (song.entity.videoId ?: song.entity.path.removePrefix("online://"))
                        .removePrefix("spotify:")
                        .trim()
                    if (song.entity.path.startsWith("online://") && !youtubeIdRegex.matches(candidate)) {
                        PlayableSong.Online(
                            id = candidate,
                            title = song.entity.title,
                            artist = song.entity.artist,
                            durationMs = song.entity.duration,
                            thumbnailUrl = song.entity.albumArt
                        )
                    } else null
                }
                else -> null
            }
        }

        if (unresolved.isEmpty()) return
        prewarmJob?.cancel()
        prewarmJob = viewModelScope.launch(Dispatchers.IO) {
            unresolved.forEachIndexed { index, song ->
                prewarmSemaphore.withPermit {
                    val rawId = song.id.removePrefix("online://").removePrefix("spotify:").trim()
                    val query = "${song.title} ${song.artist}".trim()
                    val matchedYtId = runCatching {
                        innertubeApi.search(query, filtered = true).songs.firstOrNull()?.videoId
                            ?: innertubeApi.search(query, filtered = false).songs.firstOrNull()?.videoId
                    }.getOrNull()

                    if (!matchedYtId.isNullOrBlank()) {
                        repository.cacheResolvedYoutubeId(
                            originalId = song.id,
                            rawSpotifyId = rawId,
                            youtubeVideoId = matchedYtId
                        )
                        // First 5 songs ka stream URL bhi pre-fetch kar lo instant click playback ke liye
                        if (index < 5) {
                            runCatching { innertubeApi.getStreamUrl(matchedYtId) }
                        }
                    }
                }
            }
        }
    }

    fun removeSong(songId: String) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(_playlistId.value, songId)
        }
    }

    fun renamePlaylist(newName: String) {
        viewModelScope.launch {
            repository.renamePlaylist(_playlistId.value, newName)
        }
    }

    fun addSong(song: PlayableSong) {
        viewModelScope.launch {
            val currentCount = songs.value.size
            repository.addSongToPlaylist(_playlistId.value, song, currentCount)
        }
    }

    /** Add multiple songs at once */
    fun addSongs(songList: List<PlayableSong>) {
        viewModelScope.launch {
            val currentCount = songs.value.size
            songList.forEachIndexed { i, song ->
                repository.addSongToPlaylist(_playlistId.value, song, currentCount + i)
            }
        }
    }

    /** Save reordered list of song IDs */
    fun saveReorder(newOrderIds: List<String>) {
        viewModelScope.launch {
            repository.reorderSongsInPlaylist(_playlistId.value, newOrderIds)
        }
    }

    fun addSongToPlaylist(playlistId: Long, song: PlayableSong) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, song, 0)
        }
    }

    fun deleteDownload(videoId: String) {
        viewModelScope.launch {
            downloadRepository.deleteDownloadById(videoId)
        }
    }

    fun downloadSong(song: PlayableSong.Online) {
        if (_resolvingIds.value.contains(song.id)) return
        _resolvingIds.value = _resolvingIds.value + song.id
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rawId = song.id.removePrefix("online://").removePrefix("spotify:").trim()
                val realYtId = if (youtubeIdRegex.matches(rawId)) {
                    rawId
                } else {
                    val query = "${song.title} ${song.artist}".trim()
                    val matched = runCatching {
                        innertubeApi.search(query, filtered = true).songs.firstOrNull()?.videoId
                            ?: innertubeApi.search(query, filtered = false).songs.firstOrNull()?.videoId
                    }.getOrNull()
                    if (!matched.isNullOrBlank()) {
                        repository.cacheResolvedYoutubeId(song.id, rawId, matched)
                    }
                    matched
                }

                if (realYtId.isNullOrBlank()) return@launch

                var streamUrl = innertubeApi.getCachedStreamUrl(realYtId)
                if (streamUrl.isNullOrBlank()) {
                    streamUrl = innertubeApi.getStreamUrl(realYtId)
                }
                if (!streamUrl.isNullOrBlank()) {
                    downloadRepository.startDownload(
                        OnlineSong(
                            videoId = realYtId,
                            title = song.title,
                            artist = song.artist,
                            thumbnailUrl = song.thumbnailUrl ?: "",
                            durationMs = song.durationMs,
                            durationText = "",
                            streamUrl = streamUrl
                        )
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("PlaylistDetailViewModel", "Failed to start download for ${song.title}", e)
            } finally {
                _resolvingIds.value = _resolvingIds.value - song.id
            }
        }
    }
}


