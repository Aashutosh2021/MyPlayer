package com.example.myplayer.playback

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.media3.common.PlaybackException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

enum class PlaybackErrorType {
    NETWORK_UNAVAILABLE,
    NETWORK_TIMEOUT,
    STREAM_RESOLUTION_FAILED,
    STREAM_EXPIRED,
    HTTP_REJECTED,
    MEDIA_UNSUPPORTED,
    DECODER_ERROR,
    UNKNOWN
}

@Singleton
class PlaybackErrorHandler internal constructor(
    private val context: Context?,
    var networkOverride: Boolean? = null,
    private val playbackRouter: dagger.Lazy<PlaybackRouter>? = null
) {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        playbackRouter: dagger.Lazy<PlaybackRouter>
    ) : this(context as Context?, null, playbackRouter)

    constructor(context: Context?) : this(context, null, null)

    constructor(context: Context?, networkOverride: Boolean?) : this(context, networkOverride, null)

    constructor() : this(null, true, null)

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    private var consecutiveFailures = 0
    private var retryJob: Job? = null
    private val retryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun isNetworkAvailable(): Boolean {
        networkOverride?.let { return it }
        val ctx = context ?: return true
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun getErrorType(error: PlaybackException, isOnline: Boolean = false): PlaybackErrorType {
        if (!isNetworkAvailable()) return PlaybackErrorType.NETWORK_UNAVAILABLE
        return when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> PlaybackErrorType.NETWORK_TIMEOUT
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                if (isOnline) PlaybackErrorType.STREAM_EXPIRED else PlaybackErrorType.HTTP_REJECTED
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FAILED -> PlaybackErrorType.DECODER_ERROR
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED -> PlaybackErrorType.MEDIA_UNSUPPORTED
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ->
                if (isOnline) PlaybackErrorType.STREAM_RESOLUTION_FAILED else PlaybackErrorType.NETWORK_UNAVAILABLE
            else -> PlaybackErrorType.UNKNOWN
        }
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

    fun handleError(
        error: PlaybackException,
        hasNextItem: Boolean,
        isOnline: Boolean = false,
        videoId: String? = null
    ): String {
        Log.w("PlaybackErrorHandler", "Player error: ${error.errorCodeName} (code=${error.errorCode}) isOnline=$isOnline")
        val userMessage = classifyError(error, isOnline)
        val errorType = getErrorType(error, isOnline)

        val isNetworkOrStreamError = isOnline && (
            errorType == PlaybackErrorType.STREAM_EXPIRED ||
            errorType == PlaybackErrorType.NETWORK_UNAVAILABLE ||
            errorType == PlaybackErrorType.STREAM_RESOLUTION_FAILED ||
            errorType == PlaybackErrorType.NETWORK_TIMEOUT
        )

        if (isNetworkOrStreamError) {
            consecutiveFailures++
            Log.w("PlaybackErrorHandler", "Consecutive network/stream failures: $consecutiveFailures")
        } else {
            consecutiveFailures = 0
        }

        // Do NOT set _playbackError.value on the first network/stream error
        val isFirstNetworkFailure = isNetworkOrStreamError && consecutiveFailures <= 1

        if (!hasNextItem) {
            if (!isFirstNetworkFailure) {
                _playbackError.value = userMessage
            } else {
                Log.i("PlaybackErrorHandler", "Suppressing UI error on first network/stream failure to prevent killing UI")
            }

            // Auto-retry mechanism: if STREAM_EXPIRED or NETWORK_UNAVAILABLE and no next item,
            // wait 2 seconds and attempt to call PlaybackRouter to resolve a fresh stream URL before giving up.
            if (errorType == PlaybackErrorType.STREAM_EXPIRED || errorType == PlaybackErrorType.NETWORK_UNAVAILABLE) {
                retryJob?.cancel()
                retryJob = retryScope.launch {
                    try {
                        delay(2000L)
                        val router = playbackRouter?.get()
                        if (router != null) {
                            Log.i("PlaybackErrorHandler", "Auto-retry: Attempting PlaybackRouter resolution after 2s delay (videoId=$videoId)")
                            val success = if (!videoId.isNullOrBlank()) {
                                val freshUrl = router.resolveFreshStreamUrl(videoId)
                                !freshUrl.isNullOrBlank() || router.retryCurrentTrack()
                            } else {
                                router.retryCurrentTrack()
                            }

                            if (success) {
                                Log.i("PlaybackErrorHandler", "Auto-retry via PlaybackRouter succeeded")
                                consecutiveFailures = 0
                                _playbackError.value = null
                            } else {
                                Log.w("PlaybackErrorHandler", "Auto-retry via PlaybackRouter failed. Setting playback error.")
                                _playbackError.value = userMessage
                            }
                        } else {
                            if (isFirstNetworkFailure) {
                                _playbackError.value = userMessage
                            }
                        }
                    } catch (e: Exception) {
                        if (e !is kotlinx.coroutines.CancellationException) {
                            Log.e("PlaybackErrorHandler", "Auto-retry failed with exception", e)
                            _playbackError.value = userMessage
                        }
                    }
                }
            }
        }

        return userMessage
    }

    fun setCustomError(message: String) {
        _playbackError.value = message
    }

    fun clearPlaybackError() {
        _playbackError.value = null
        consecutiveFailures = 0
        retryJob?.cancel()
        retryJob = null
    }
}
