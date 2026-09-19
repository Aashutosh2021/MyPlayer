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

    @Synchronized
    fun play(
        scope: CoroutineScope,
        delegate: PlaybackRouterDelegate,
        requests: List<PlayRequest>,
        startIndex: Int
    ) {
        if (requests.isEmpty()) return
        activePlayJob?.cancel()
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

            // Make a list of items for the player queue
            val mediaItems = requests.mapIndexed { i, r ->
                if (i == clampedIndex) {
                    currentMediaItem
                } else {
                    // Placeholder items
                    mediaItemFactory.createMediaItem(
                        songId = r.songId,
                        path = r.localUri ?: "online://${r.songId}",
                        title = r.title,
                        artist = r.artist,
                        albumArt = r.albumArt
                    )
                }
            }.toMutableList()

            // 3. Play items on ExoPlayer via delegate
            withContext(Dispatchers.Main) {
                delegate.playMediaItems(mediaItems, clampedIndex)
            }

            // 4. Pre-resolve only the immediate next track after playback stabilizes (delay 4s)
            // This prevents YouTube rate-limiting/throttling from hammering the entire queue.
            launch(Dispatchers.IO) {
                kotlinx.coroutines.delay(4000)
                val nextIndex = clampedIndex + 1
                if (nextIndex < requests.size) {
                    val req = requests[nextIndex]
                    val resolvedPath = sourceResolver.resolve(req) { /* ignore background errors */ }
                    if (resolvedPath != null && resolvedPath != req.localUri) {
                        withContext(Dispatchers.Main) {
                            if (delegate.getMediaItemCount() == requests.size && nextIndex < delegate.getMediaItemCount()) {
                                val currentItem = delegate.getMediaItemAt(nextIndex)
                                if (currentItem != null) {
                                    val resolvedItem = mediaItemFactory.createMediaItem(
                                        songId = req.songId,
                                        path = resolvedPath,
                                        title = req.title,
                                        artist = req.artist,
                                        albumArt = req.albumArt
                                    )
                                    delegate.replaceMediaItem(nextIndex, resolvedItem)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
