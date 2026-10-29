package com.myplayer.app.ui.screens.nowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myplayer.app.data.lyrics.LyricsRepository
import com.myplayer.app.data.lyrics.DetectedScript
import com.myplayer.app.data.lyrics.LyricsResult
import com.myplayer.app.data.lyrics.LyricsScript
import com.myplayer.app.data.lyrics.LyricsScriptConverter
import com.myplayer.app.playback.MusicController
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
    private val scriptConverter: LyricsScriptConverter,
    private val musicController: MusicController,
    private val audioPlaybackStore: com.myplayer.app.data.preferences.AudioPlaybackStore
) : ViewModel() {

    private val rawState = MutableStateFlow<LyricsUiState>(LyricsUiState.Idle)

    // Script picker: Original / Hinglish / Hindi / Urdu. English lyrics are never converted.
    private val _scriptMode = MutableStateFlow(LyricsScript.ORIGINAL)
    val scriptMode: StateFlow<LyricsScript> = _scriptMode.asStateFlow()
    private val _detectedScript = MutableStateFlow(DetectedScript.UNKNOWN)
    val detectedScript: StateFlow<DetectedScript> = _detectedScript.asStateFlow()
    private val _converting = MutableStateFlow(false)
    val converting: StateFlow<Boolean> = _converting.asStateFlow()

    fun setScriptMode(mode: LyricsScript) { _scriptMode.value = mode }

    /** What the screens show: the fetched lyrics, converted to the picked script. */
    val lyricsState: StateFlow<LyricsUiState> = combine(rawState, _scriptMode) { raw, mode -> raw to mode }
        .mapLatest { (raw, mode) -> applyScript(raw, mode) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, LyricsUiState.Idle)

    private suspend fun applyScript(raw: LyricsUiState, mode: LyricsScript): LyricsUiState {
        if (raw !is LyricsUiState.Found) {
            _detectedScript.value = DetectedScript.UNKNOWN
            return raw
        }
        val synced = raw.syncedLines
        val lines = if (synced.isNotEmpty()) synced.map { it.second } else (raw.result.plainLyrics ?: "").lines()
        val from = scriptConverter.detect(lines)
        _detectedScript.value = from
        if (mode == LyricsScript.ORIGINAL || !from.convertible) return raw
        _converting.value = true
        try {
            val out = scriptConverter.convert(lines, from, mode)
            return if (synced.isNotEmpty()) {
                raw.copy(syncedLines = synced.mapIndexed { i, (t, _) -> t to out[i] })
            } else {
                raw.copy(result = raw.result.copy(plainLyrics = out.joinToString("\n")))
            }
        } finally {
            _converting.value = false
        }
    }

    val currentPosition = musicController.currentPosition

    val keepScreenAwakeLyrics = audioPlaybackStore.stateFlow
        .map { it.keepScreenAwakeLyrics }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

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
                    rawState.value = LyricsUiState.Idle
                    return@collectLatest
                }
                fetchLyrics(metadata)
            }
        }
    }

    private var lastMetadata: LyricsMetadata? = null
    private var fetchJob: kotlinx.coroutines.Job? = null

    fun retryLyrics() {
        val local = musicController.currentSong.value
        val online = musicController.currentOnlineSong.value
        val fallback = lastMetadata
        val meta = when {
            local != null -> LyricsMetadata(
                songId = local.id,
                title = local.title,
                artist = local.artist,
                album = local.album,
                durationMs = if (local.duration > 0L) local.duration else musicController.currentDuration.value
            )
            online != null -> LyricsMetadata(
                songId = online.videoId,
                title = online.title,
                artist = online.artist,
                album = null,
                durationMs = if (online.durationMs > 0L) online.durationMs else musicController.currentDuration.value
            )
            fallback != null -> {
                val dur = if (fallback.durationMs > 0L) fallback.durationMs else musicController.currentDuration.value
                fallback.copy(durationMs = dur)
            }
            else -> null
        } ?: return

        lastMetadata = meta
        fetchLyricsInternal(meta, forceRefresh = true)
    }

    fun saveCustomLyrics(lyricsText: String) {
        val metadata = lastMetadata ?: return
        viewModelScope.launch {
            rawState.value = LyricsUiState.Loading
            val result = lyricsRepository.saveCustomLyrics(
                songId = metadata.songId,
                trackName = metadata.title,
                artistName = metadata.artist,
                lyricsText = lyricsText
            )
            val synced = result.syncedLyrics?.let { lyricsRepository.parseSyncedLyrics(it) } ?: emptyList()
            rawState.value = LyricsUiState.Found(result, synced)
        }
    }

    fun deleteLyrics() {
        val metadata = lastMetadata ?: return
        viewModelScope.launch {
            lyricsRepository.deleteLyrics(metadata.songId)
            retryLyrics()
        }
    }

    fun getCurrentMetadata(): LyricsMetadata? = lastMetadata

    fun fetchLyrics(metadata: LyricsMetadata) {
        fetchLyricsInternal(metadata, forceRefresh = false)
    }

    private fun fetchLyricsInternal(metadata: LyricsMetadata, forceRefresh: Boolean) {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            rawState.value = LyricsUiState.Loading
            if (forceRefresh) {
                lyricsRepository.deleteLyrics(metadata.songId)
            }
            val durationSec = (metadata.durationMs / 1000).toInt()
            val result = lyricsRepository.fetchLyrics(
                songId = metadata.songId,
                trackName = metadata.title,
                artistName = metadata.artist,
                durationSeconds = durationSec,
                albumName = metadata.album
            )
            rawState.value = if (result != null) {
                val synced = result.syncedLyrics?.let { lyricsRepository.parseSyncedLyrics(it) } ?: emptyList()
                LyricsUiState.Found(result, synced)
            } else {
                LyricsUiState.NotFound
            }
        }
    }
}


