package com.example.myplayer.playback

import androidx.media3.common.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import javax.inject.Inject
import javax.inject.Singleton

interface PlaybackRouterDelegate {
    fun playMediaItems(mediaItems: List<MediaItem>, startIndex: Int)
    fun prepareMediaItems(mediaItems: List<MediaItem>, startIndex: Int, startPositionMs: Long = 0L, onPrepared: () -> Unit = {}) {}
    fun replaceMediaItem(index: Int, mediaItem: MediaItem)
    fun setCustomError(message: String)
    fun stopPlayback()
    fun getMediaItemAt(index: Int): MediaItem?
    fun getMediaItemCount(): Int
}

@Singleton
class PlaybackRouter @Inject constructor(
    private val sourceResolver: PlaybackSourceResolver,
    private val mediaItemFactory: MediaItemFactory
) {
    val currentAudioQuality: kotlinx.coroutines.flow.StateFlow<AudioQualityInfo> = sourceResolver.currentAudioQuality
    private var activePlayJob: Job? = null
    private var prefetchJob: Job? = null
    private var currentRequests: List<PlayRequest> = emptyList()

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
        activePlayJob = scope.launch {
            val resolvedPath = sourceResolver.resolve(request) { err ->
                delegate.setCustomError(err)
            } ?: return@launch

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

        activePlayJob = scope.launch {
            // 1. Resolve current request path immediately
            val currentPath = sourceResolver.resolve(currentRequest) { err ->
                delegate.setCustomError(err)
            }
            if (currentPath == null) {
                withContext(Dispatchers.Main) {
                    delegate.stopPlayback()
                }
                return@launch
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
                val resolvedPath = withContext(Dispatchers.IO) {
                    sourceResolver.resolve(currentReq) { err ->
                        delegate.setCustomError(err)
                    }
                }
                if (resolvedPath != null && resolvedPath != currentUri) {
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
            if (nextIndex in currentRequests.indices) {
                kotlinx.coroutines.delay(2500)
                val nextReq = currentRequests[nextIndex]
                val nextItem = delegate.getMediaItemAt(nextIndex)
                val nextUri = nextItem?.localConfiguration?.uri?.toString()
                if (nextUri != null && nextUri.startsWith("online://")) {
                    val nextResolved = withContext(Dispatchers.IO) {
                        sourceResolver.resolve(nextReq) { /* ignore background errors */ }
                    }
                    if (nextResolved != null && nextResolved != nextUri) {
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
