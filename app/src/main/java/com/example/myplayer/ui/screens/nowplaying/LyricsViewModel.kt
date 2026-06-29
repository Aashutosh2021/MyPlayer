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
            // Watch both local and online song
            combine(musicController.currentSong, musicController.currentOnlineSong) { local, online ->
                when {
                    local != null -> Pair(local.title, local.artist)
                    online != null -> Pair(online.title, online.artist)
                    else -> null
                }
            }.distinctUntilChanged().collectLatest { songInfo ->
                if (songInfo == null) {
                    _lyricsState.value = LyricsUiState.Idle
                    return@collectLatest
                }
                val (title, artist) = songInfo
                fetchLyrics(title, artist)
            }
        }
    }

    fun fetchLyrics(title: String, artist: String) {
        viewModelScope.launch {
            _lyricsState.value = LyricsUiState.Loading
            val result = lyricsRepository.fetchLyrics(title, artist)
            _lyricsState.value = if (result != null) {
                val synced = result.syncedLyrics?.let { lyricsRepository.parseSyncedLyrics(it) } ?: emptyList()
                LyricsUiState.Found(result, synced)
            } else {
                LyricsUiState.NotFound
            }
        }
    }
}
