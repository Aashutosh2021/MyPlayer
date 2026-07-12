package com.example.myplayer.playback

import android.util.Log
import androidx.media3.common.PlaybackException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackErrorHandler @Inject constructor() {
    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    fun handleError(error: PlaybackException, hasNextItem: Boolean): String {
        Log.w("PlaybackErrorHandler", "Player error: ${error.errorCodeName}")
        val userMessage = when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> 
                "This song isn't available offline. The downloaded file may have been removed."
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ->
                "No internet connection. Cannot stream online song."
            else -> "Playback error: ${error.message ?: "Unknown error"}"
        }
        
        if (!hasNextItem) {
            _playbackError.value = userMessage
        }
        return userMessage
    }

    fun setCustomError(message: String) {
        _playbackError.value = message
    }

    fun clearPlaybackError() {
        _playbackError.value = null
    }
}
