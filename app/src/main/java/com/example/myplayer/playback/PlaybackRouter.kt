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
        val isOnline = isOnlineRequest(request)
        if (isOnline) {
            _isResolving.value = true
            playbackStateManager.updateMachineState(PlaybackMachineState.RESOLVING)
            delegate.onStreamResolving(request)
        }
        activePlayJob = scope.launch {
            val resolvedPath = sourceResolver.resolve(request) { err ->
                delegate.setCustomError(err)
            }
            if (resolvedPath == null) {
                if (isOnline) {
                    _isResolving.value = false
                    playbackStateManager.updateMachineState(PlaybackMachineState.FAILED)
                }
                return@launch
            }

            if (isOnline) {
                _isResolving.value = false
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
        currentRequests = requests
        val clampedIndex = startIndex.coerceIn(0, requests.size - 1)
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
            var resolutionError: String? = null
            // 1. Resolve current request path immediately
            val currentPath = sourceResolver.resolve(currentRequest) { err ->
                resolutionError = err
            }
            if (currentPath == null) {
                if (isOnline) {
                    _isResolving.value = false
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
                _isResolving.value = false
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
        }
    }

    fun onTrackTransition(
        scope: CoroutineScope,
        delegate: PlaybackRouterDelegate,
        currentIndex: Int
    ) {
        if (currentIndex !in currentRequests.indices) return
        val currentReq = currentRequests[currentIndex]

        prefetchJob?.cancel()
        prefetchJob = scope.launch(Dispatchers.Main) {
            val currentItem = delegate.getMediaItemAt(currentIndex)
            val currentUri = currentItem?.localConfiguration?.uri?.toString()

            // 1. If current item still has unresolved online:// URI, resolve and replace immediately
            if (currentUri != null && currentUri.startsWith("online://")) {
                _isResolving.value = true
                playbackStateManager.updateMachineState(PlaybackMachineState.RESOLVING)
                delegate.onStreamResolving(currentReq)
                val resolvedPath = withContext(Dispatchers.IO) {
                    sourceResolver.resolve(currentReq) { err ->
                        delegate.setCustomError(err)
                    }
                }
                _isResolving.value = false
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
