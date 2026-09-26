package com.example.myplayer.playback

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.media3.common.PlaybackException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackErrorHandler internal constructor(
    private val context: Context?,
    var networkOverride: Boolean? = null
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context as Context?, null)

    constructor() : this(null, true)

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    fun isNetworkAvailable(): Boolean {
        networkOverride?.let { return it }
        val ctx = context ?: return true
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun classifyError(error: PlaybackException, isOnline: Boolean = false): String {
        return when {
            !isNetworkAvailable() ->
                "No internet connection. Please check your network."

            error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                if (isOnline) "Audio stream expired or server rejected request."
                else "This song isn't available offline. The downloaded file may have been removed."

            error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                "Connection timed out while loading audio stream."

            error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ->
                if (isOnline) "Network error while streaming audio. Stream server could not be reached."
                else "No internet connection."

            error.errorCode == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                "Local or downloaded file not found."

            error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
            error.errorCode == PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED ->
                "Audio decoder error. This media format is not supported on this device."

            error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ||
            error.errorCode == PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED ->
                "Corrupt audio stream format."

            else ->
                if (isOnline) "Failed to play online stream: ${error.message ?: error.errorCodeName}"
                else "Playback error: ${error.message ?: error.errorCodeName}"
        }
    }

    fun handleError(error: PlaybackException, hasNextItem: Boolean, isOnline: Boolean = false): String {
        Log.w("PlaybackErrorHandler", "Player error: ${error.errorCodeName} (code=${error.errorCode}) isOnline=$isOnline")
        val userMessage = classifyError(error, isOnline)
        
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
