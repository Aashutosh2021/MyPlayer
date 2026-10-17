package com.example.myplayer.playback

import androidx.media3.common.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface PlaybackRouterDelegate {
    fun playMediaItems(mediaItems: List<MediaItem>, startIndex: Int)
    fun prepareMediaItems(mediaItems: List<MediaItem>, startIndex: Int, startPositionMs: Long = 0L, onPrepared: () -> Unit = {}) {}
    fun replaceMediaItem(index: Int, mediaItem: MediaItem)
    fun setCustomError(message: String)
    fun stopPlayback()
    fun onStreamResolving(request: PlayRequest) {}
    fun onStreamResolutionFailed(request: PlayRequest, error: String) {
        setCustomError(error)
        stopPlayback()
    }
    fun getMediaItemAt(index: Int): MediaItem?
    fun getMediaItemCount(): Int
    fun getNextPlayRequest(): PlayRequest? = null
}

@Singleton
class PlaybackRouter @Inject constructor(
    private val sourceResolver: PlaybackSourceResolver,
    private val mediaItemFactory: MediaItemFactory,
    private val playbackStateManager: PlaybackStateManager
) {
    val currentAudioQuality: StateFlow<AudioQualityInfo> = sourceResolver.currentAudioQuality
    private val _isResolving = MutableStateFlow(false)
    val isResolving: StateFlow<Boolean> = _isResolving.asStateFlow()

    private var activePlayJob: Job? = null
    private var prefetchJob: Job? = null
    private var currentRequests: List<PlayRequest> = emptyList()
    private var lastScope: CoroutineScope? = null
    private var lastDelegate: PlaybackRouterDelegate? = null
    private var lastIndex: Int = 0

    private fun isOnlineRequest(request: PlayRequest): Boolean {
        return request.playbackSource == PlaybackSourceType.ONLINE ||
                request.playbackSource == PlaybackSourceType.RECOMMENDATION ||
                request.localUri?.startsWith("online://") == true ||
                request.localUri?.startsWith("http") == true
    }

    @Synchronized
    fun prepare(
        scope: CoroutineScope,
        delegate: PlaybackRouterDelegate,
        request: PlayRequest,
        startPositionMs: Long = 0L,
        onPrepared: () -> Unit = {}
    ) {
        activePlayJob?.cancel()
        prefetchJob?.cancel()
        lastScope = scope
        lastDelegate = delegate
        currentRequests = listOf(request)
        lastIndex = 0

        val isOnline = isOnlineRequest(request)
        if (isOnline) {
            _isResolving.value = true
            playbackStateManager.updateMachineState(PlaybackMachineState.RESOLVING)
            delegate.onStreamResolving(request)
        }
        activePlayJob = scope.launch {
            try {
                val resolvedPath = sourceResolver.resolve(request) { err ->
                    delegate.setCustomError(err)
                }
                if (resolvedPath == null) {
                    if (isOnline) {
                        playbackStateManager.updateMachineState(PlaybackMachineState.FAILED)
                    }
                    return@launch
                }

                if (isOnline) {
                    playbackStateManager.updateMachineState(PlaybackMachineState.PREPARING)
                }

                val mediaItem = mediaItemFactory.createMediaItem(
                    songId = request.songId,
                    path = resolvedPath,
                    title = request.title,
                    artist = request.artist,
                    albumArt = request.albumArt
                )

                withContext(Dispatchers.Main) {
                    delegate.prepareMediaItems(listOf(mediaItem), 0, startPositionMs, onPrepared)
                }
            } finally {
                if (isOnline) {
                    _isResolving.value = false
                }
            }
        }
    }

    @Synchronized
    fun play(
        scope: CoroutineScope,
        delegate: PlaybackRouterDelegate,
        requests: List<PlayRequest>,
        startIndex: Int
    ) {
        if (requests.isEmpty()) return
        activePlayJob?.cancel()
        prefetchJob?.cancel()
        lastScope = scope
        lastDelegate = delegate
        currentRequests = requests
        val clampedIndex = startIndex.coerceIn(0, requests.size - 1)
        lastIndex = clampedIndex
        val currentRequest = requests[clampedIndex]
        val isOnline = isOnlineRequest(currentRequest)

        if (isOnline) {
            _isResolving.value = true
            playbackStateManager.updateMachineState(PlaybackMachineState.RESOLVING)
            delegate.onStreamResolving(currentRequest)
        }

        // Cancel any background prefetches so 100% of bandwidth/CPU goes to the clicked song!
        sourceResolver.cancelPrefetches()

        activePlayJob = scope.launch {
            try {
                var resolutionError: String? = null
                val currentPath = sourceResolver.resolve(currentRequest) { err ->
                    resolutionError = err
                }
                if (currentPath == null) {
                    if (isOnline) {
                        playbackStateManager.updateMachineState(PlaybackMachineState.FAILED)
                    }
                    withContext(Dispatchers.Main) {
                        delegate.onStreamResolutionFailed(
                            currentRequest,
                            resolutionError ?: "Failed to resolve stream URL"
                        )
                    }
                    return@launch
                }

                if (isOnline) {
                    playbackStateManager.updateMachineState(PlaybackMachineState.PREPARING)
                }

                val currentMediaItem = mediaItemFactory.createMediaItem(
                    songId = currentRequest.songId,
                    path = currentPath,
                    title = currentRequest.title,
                    artist = currentRequest.artist,
                    albumArt = currentRequest.albumArt
                )

                val hasOnlineItems = requests.any { isOnlineRequest(it) } || currentPath.startsWith("http")

                // For online queues, always keep 1 active item in ExoPlayer so MusicController's
                // _songQueue handles Shuffle, Repeat-All, Repeat-One, and Next/Prev without index mismatch.
                val mediaItems = if (hasOnlineItems || requests.size <= 1) {
                    listOf(currentMediaItem)
                } else {
                    requests.mapIndexed { i, r ->
                        if (i == clampedIndex) {
                            currentMediaItem
                        } else {
                            mediaItemFactory.createMediaItem(
                                songId = r.songId,
                                path = r.localUri ?: "",
                                title = r.title,
                                artist = r.artist,
                                albumArt = r.albumArt
                            )
                        }
                    }
                }

                val targetIndex = if (hasOnlineItems || requests.size <= 1) 0 else clampedIndex

                withContext(Dispatchers.Main) {
                    delegate.playMediaItems(mediaItems, targetIndex)
                }
            } finally {
                if (isOnline) {
                    _isResolving.value = false
                }
            }
        }
    }

    suspend fun resolveFreshStreamUrl(videoId: String): String? {
        return sourceResolver.resolveFreshStreamUrl(videoId)
    }

    suspend fun retryCurrentTrack(): Boolean {
        val delegate = lastDelegate ?: return false
        val scope = lastScope ?: return false
        val requests = currentRequests
        val index = lastIndex
        if (requests.isEmpty() || index !in requests.indices) return false

        val currentReq = requests[index]
        val rawUri = currentReq.localUri ?: currentReq.songId
        val videoId = if (rawUri.startsWith("online://")) {
            rawUri.removePrefix("online://")
        } else if (currentReq.songId.startsWith("online://")) {
            currentReq.songId.removePrefix("online://")
        } else if (currentReq.songId.matches(Regex("^[a-zA-Z0-9_-]{11}\$"))) {
            currentReq.songId
        } else {
            currentReq.metadata["videoId"]
        } ?: return false

        val freshUrl = sourceResolver.resolveFreshStreamUrl(videoId)
        if (freshUrl.isNullOrBlank()) return false

        val freshRequest = currentReq.copy(streamUrl = freshUrl, localUri = freshUrl)
        val updatedRequests = requests.toMutableList().apply {
            this[index] = freshRequest
        }
        withContext(Dispatchers.Main) {
            play(scope, delegate, updatedRequests, index)
        }
        return true
    }

    /**
     * Pre-warms the stream URL cache for upcoming tracks (both linear next and shuffle candidates)
     * WITHOUT mutating ExoPlayer's mediaItem list so queue indices, Shuffle, and Repeat stay intact.
     */
    fun onTrackTransition(
        scope: CoroutineScope,
        delegate: PlaybackRouterDelegate,
        queueIndex: Int
    ) {
        lastScope = scope
        lastDelegate = delegate
        if (queueIndex in currentRequests.indices) {
            lastIndex = queueIndex
        }

        prefetchJob?.cancel()
        prefetchJob = scope.launch(Dispatchers.IO) {
            // Wait 3 seconds so current song buffering finishes first at full speed
            kotlinx.coroutines.delay(3000)
            val nextReq = delegate.getNextPlayRequest()
                ?: currentRequests.getOrNull(lastIndex + 1)

            if (nextReq != null) {
                sourceResolver.resolve(nextReq) { /* ignore background prefetch error */ }
            }
        }
    }
}
