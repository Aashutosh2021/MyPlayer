package com.example.myplayer.playback

import androidx.media3.common.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

        // Prefetch subsequent songs in queue
        val upcomingRequests = requests.drop(clampedIndex + 1).take(3)
        if (upcomingRequests.isNotEmpty()) {
            sourceResolver.prefetchSongs(upcomingRequests)
        }

        activePlayJob = scope.launch {
            try {
                var resolutionError: String? = null
                // 1. Resolve current request path immediately
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

                // 2. Build MediaItem for the current song
                val currentMediaItem = mediaItemFactory.createMediaItem(
                    songId = currentRequest.songId,
                    path = currentPath,
                    title = currentRequest.title,
                    artist = currentRequest.artist,
                    albumArt = currentRequest.albumArt
                )

                // CRITICAL: NEVER pass unresolvable "online://" items to ExoPlayer!
                val hasOnlineItems = requests.any {
                    it.playbackSource == PlaybackSourceType.ONLINE ||
                    it.localUri?.startsWith("online://") == true ||
                    it.localUri?.startsWith("http") == true
                } || currentPath.startsWith("http")

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
        } else if (currentReq.songId.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
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

    fun onTrackTransition(
        scope: CoroutineScope,
        delegate: PlaybackRouterDelegate,
        currentIndex: Int
    ) {
        if (currentIndex !in currentRequests.indices) return
        val currentReq = currentRequests[currentIndex]
        lastScope = scope
        lastDelegate = delegate
        lastIndex = currentIndex

        prefetchJob?.cancel()
        prefetchJob = scope.launch(Dispatchers.Main) {
            val currentItem = delegate.getMediaItemAt(currentIndex)
            val currentUri = currentItem?.localConfiguration?.uri?.toString()

            // 1. If current item still has unresolved online:// URI, resolve and replace immediately
            if (currentUri != null && currentUri.startsWith("online://")) {
                _isResolving.value = true
                playbackStateManager.updateMachineState(PlaybackMachineState.RESOLVING)
                delegate.onStreamResolving(currentReq)
                try {
                    val resolvedPath = withContext(Dispatchers.IO) {
                        sourceResolver.resolve(currentReq) { err ->
                            delegate.setCustomError(err)
                        }
                    }
                    if (resolvedPath != null && resolvedPath != currentUri) {
                        playbackStateManager.updateMachineState(PlaybackMachineState.PREPARING)
                        val resolvedItem = mediaItemFactory.createMediaItem(
                            songId = currentReq.songId,
                            path = resolvedPath,
                            title = currentReq.title,
                            artist = currentReq.artist,
                            albumArt = currentReq.albumArt
                        )
                        delegate.replaceMediaItem(currentIndex, resolvedItem)
                    }
                } finally {
                    _isResolving.value = false
                }
            }

            // 2. Pre-resolve next track after playback stabilizes (delay 2.5s)
            val nextIndex = currentIndex + 1
            val nextReq = if (nextIndex in currentRequests.indices) {
                currentRequests[nextIndex]
            } else {
                delegate.getNextPlayRequest()
            }

            if (nextReq != null) {
                // Prefetch upcoming tracks in background immediately
                val upcoming = currentRequests.drop(nextIndex).take(3)
                if (upcoming.isNotEmpty()) {
                    sourceResolver.prefetchSongs(upcoming)
                } else {
                    sourceResolver.prefetchSongs(listOf(nextReq))
                }

                kotlinx.coroutines.delay(2500)
                val nextItem = if (nextIndex in currentRequests.indices) delegate.getMediaItemAt(nextIndex) else null
                val nextUri = nextItem?.localConfiguration?.uri?.toString() ?: nextReq.localUri
                if (nextUri != null && nextUri.startsWith("online://")) {
                    val nextResolved = withContext(Dispatchers.IO) {
                        sourceResolver.resolve(nextReq) { /* ignore background errors */ }
                    }
                    if (nextResolved != null && nextResolved != nextUri && nextItem != null) {
                        if (delegate.getMediaItemCount() == currentRequests.size && nextIndex < delegate.getMediaItemCount()) {
                            val resolvedNextItem = mediaItemFactory.createMediaItem(
                                songId = nextReq.songId,
                                path = nextResolved,
                                title = nextReq.title,
                                artist = nextReq.artist,
                                albumArt = nextReq.albumArt
                            )
                            delegate.replaceMediaItem(nextIndex, resolvedNextItem)
                        }
                    }
                }
            }
        }
    }
}
