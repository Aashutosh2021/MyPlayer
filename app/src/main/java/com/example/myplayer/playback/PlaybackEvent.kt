package com.example.myplayer.playback

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed class PlaybackEvent {
    data class SongStarted(
        val songId: String,
        val title: String,
        val artist: String,
        val isOnline: Boolean
    ) : PlaybackEvent()

    data class SongCompleted(val songId: String) : PlaybackEvent()
    data class SongSkipped(val songId: String) : PlaybackEvent()
    data class SongError(val songId: String, val error: String) : PlaybackEvent()
    object QueueEmpty : PlaybackEvent()
    object AutoplayRequested : PlaybackEvent()

    data class PlayRequestReady(
        val requests: List<PlayRequest>,
        val startIndex: Int
    ) : PlaybackEvent()
}

@Singleton
class PlaybackEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<PlaybackEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<PlaybackEvent> = _events.asSharedFlow()

    fun emit(event: PlaybackEvent) {
        _events.tryEmit(event)
    }
}
