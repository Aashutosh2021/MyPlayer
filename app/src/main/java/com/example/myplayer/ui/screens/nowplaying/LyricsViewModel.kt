package com.example.myplayer.ui.screens.nowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myplayer.data.lyrics.LyricsRepository
import com.example.myplayer.data.lyrics.LyricsResult
import com.example.myplayer.playback.MusicController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LyricsUiState {
    object Idle : LyricsUiState()
    object Loading : LyricsUiState()
    data class Found(val result: LyricsResult, val syncedLines: List<Pair<Long, String>>) : LyricsUiState()
    object NotFound : LyricsUiState()
}

data class LyricsMetadata(
    val songId: String,
    val title: String,
    val artist: String,
    val album: String?,
    val durationMs: Long
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LyricsViewModel @Inject constructor(
    private val lyricsRepository: LyricsRepository,
    private val musicController: MusicController
) : ViewModel() {

    private val _lyricsState = MutableStateFlow<LyricsUiState>(LyricsUiState.Idle)
    val lyricsState: StateFlow<LyricsUiState> = _lyricsState.asStateFlow()

    val currentPosition = musicController.currentPosition

    // Auto-fetch whenever the playing song changes
    init {
        viewModelScope.launch {
            combine(musicController.currentSong, musicController.currentOnlineSong) { local, online ->
                when {
                    local != null -> LyricsMetadata(
                        songId = local.id,
                        title = local.title,
                        artist = local.artist,
                        album = local.album,
                        durationMs = local.duration
                    )
                    online != null -> LyricsMetadata(
                        songId = online.videoId,
                        title = online.title,
                        artist = online.artist,
                        album = null,
                        durationMs = online.durationMs
                    )
                    else -> null
                }
            }.distinctUntilChanged().collectLatest { metadata ->
                lastMetadata = metadata
                if (metadata == null) {
                    _lyricsState.value = LyricsUiState.Idle
                    return@collectLatest
                }
                fetchLyrics(metadata)
            }
        }
    }

    private var lastMetadata: LyricsMetadata? = null

    fun retryLyrics() {
        lastMetadata?.let { fetchLyrics(it) }
    }

    fun fetchLyrics(metadata: LyricsMetadata) {
        viewModelScope.launch {
            _lyricsState.value = LyricsUiState.Loading
            val durationSec = (metadata.durationMs / 1000).toInt()
            val result = lyricsRepository.fetchLyrics(
                songId = metadata.songId,
                trackName = metadata.title,
                artistName = metadata.artist,
                durationSeconds = durationSec,
                albumName = metadata.album
            )
            _lyricsState.value = if (result != null) {
                val synced = result.syncedLyrics?.let { lyricsRepository.parseSyncedLyrics(it) } ?: emptyList()
                LyricsUiState.Found(result, synced)
            } else {
                LyricsUiState.NotFound
            }
        }
    }
}
