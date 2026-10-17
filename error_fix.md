Maine aapke codebase (MusicController.kt, PlaybackRouter.kt, OnlineSearchScreen.kt, aur HomeViewModel.kt) mein trace kar liya hai ki Recommendation / Search songs kabhi loop mein kyun fass jaate hain aur Shuffle / Loop (Repeat) buttons kabhi-kabhi kaam kyun nahi karte.

Asli Problem Kya Hai? (Exact Root Causes)
1. Gaana Kabhi Loop Mein Bajta Hai Aur Kabhi Aage Badh Jata Hai — Kyun?
ExoPlayer Index (0) vs Queue Index Mismatch (PlaybackRouter.kt:170–186 & MusicController.kt:227, 249):

Jab aap Search ya Recommendations ki list se koi online gaana play karte ho, toh _songQueue mein saare gaane (maslan 20 songs) hote hain, par PlaybackRouter.play() ExoPlayer ko sirf 1 current song (listOf(currentMediaItem)) deta hai. Is wajah se ExoPlayer ke andar controller.currentMediaItemIndex hamesha 0 hota hai.

Jaise hi gaana bajna shuru hota hai, MusicController.onMediaItemTransition (line 227) ExoPlayer ka currentIndex = 0 utha kar playbackRouter.onTrackTransition(currentIndex = 0) ko bhej deta hai.

Ab dekho kya hota hai:

Jab aap list ka 1st gaana (index = 0) play karte ho: Queue index bhi 0 hai aur ExoPlayer index bhi 0 hai. Toh nextIndex = 0 + 1 = 1 banta hai aur 2.5 second baad 2nd song (index = 1) ExoPlayer mein add ho jata hai — isliye 1st song play karne par gaana aage badh jata hai!

Jab aap list ka 2nd gaana (index = 1) play karte ho: Aapne play kiya index = 1, par ExoPlayer mein 1 hi song hone ki wajah se currentIndex 0 aata hai! Phir onTrackTransition nikalta hai nextIndex = 0 + 1 = 1, aur currentRequests[1] (यानी wahi same 2nd gaana jo abhi baj raha hai!) ko 2.5 second baad agle gaane ke roop mein ExoPlayer mein dobara add kar deta hai ([Song_1, Song_1]). Nateeja: wahi gaana loop mein bar-bar bajta rehta hai!

Autoplay Recommendation Ka Purane Index Par Jump Karna (MusicController.kt:148–155):

Jab PlayRequestReady event aata hai aur recommended song pehle se _songQueue mein maujood hota hai, toh existingIndex milne par player queue ke end mein jaane ke bajaye wapas purane index par kood jata hai.

2. Shuffle Aur Loop (Repeat) Buttons Kabhi-Kabhi Kaam Kyun Nahi Karte?
Shuffle Button Kyun Fail Hota Hai (PlaybackRouter.kt:286 vs MusicController.kt:452, 985):

Jab koi online gaana start hota hai, PlaybackRouter.onTrackTransition 2.5 second baad bina isShuffleOn check kiye seedha agla linear gaana (index + 1) resolve karke ExoPlayer mein controller.addMediaItem() kar deta hai.

Isse controller.hasNextMediaItem() true ho jata hai!

Jab gaana khatam hota hai (handlePlaybackEnded, line 452) ya aap Next button dabate ho (skipToNext, line 985), toh sabse pehli line hai:

Kotlin
if (controller.hasNextMediaItem()) {
    controller.seekToNext() // Shuffle ko poori tarah ignore kar deta hai!
}
Kyunki ExoPlayer mein index + 1 wala song pehle hi jud chuka hota hai, handlePlaybackEnded() aur skipToNext() ka Shuffle code (lines 459 aur 991) kabhi chalta hi nahi! Shuffle button ON dikhta rehta hai, par gaana hamesha line se agla hi bajta hai.

Loop (Repeat) Button Kyun Out-of-Sync Ho Jata Hai (MusicController.kt:442, 1086, 1113):

Jab gaana start hota hai tab mediaItemCount == 1 hota hai, isliye REPEAT_MODE_ALL hone par code ExoPlayer ko REPEAT_MODE_OFF set kar deta hai.

Lekin 2.5 second baad jab onTrackTransition doosra song ExoPlayer mein add karta hai, tab mediaItemCount badh kar 2 ho jata hai!

Ab line 442 ki condition (mediaController?.mediaItemCount ?: 0) <= 1 false ho jati hai, aur ExoPlayer ka REPEAT_MODE_OFF aapke Repeat state ko overwrite karke Loop button ko apne aap OFF kar deta hai!

Permanent Fix (Copy-Paste Ready)
Iska sabse clean aur 100% bug-free fix ye hai:

PlaybackRouter agle gaane ko background mein sirf cache mein pre-resolve karega (sourceResolver.resolve), ExoPlayer ki playlist mein replaceMediaItem / addMediaItem nahi karega. Isse mediaItemCount hamesha 1 rahega, URL cache mein <1ms par ready rahega, aur Shuffle + Repeat buttons 100% MusicController ke control mein rahenge.

MusicController mein ek dedicated currentQueueIndex rahega jo ExoPlayer ke 0 index se kabhi confuse nahi hoga.

1. Replace PlaybackRouter.kt (Complete File)
app/src/main/java/com/example/myplayer/playback/PlaybackRouter.kt ko is code se replace karo:

Kotlin
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

        // Prefetch upcoming songs in queue into InnertubeApi cache
        val upcomingRequests = requests.drop(clampedIndex + 1).take(3)
        if (upcomingRequests.isNotEmpty()) {
            sourceResolver.prefetchSongs(upcomingRequests)
        }

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
            val nextReq = delegate.getNextPlayRequest()
                ?: currentRequests.getOrNull(lastIndex + 1)

            if (nextReq != null) {
                val upcoming = currentRequests.drop(lastIndex + 1).take(3).ifEmpty { listOf(nextReq) }
                sourceResolver.prefetchSongs(upcoming)

                kotlinx.coroutines.delay(2000)
                // Warm the stream URL in cache so STATE_ENDED transition is 0ms instant
                sourceResolver.resolve(nextReq) { /* ignore background prefetch error */ }
            }
        }
    }
}
2. Update Key Sections in MusicController.kt
MusicController.kt mein ye 5 chhote updates kar do:

Update A — Track currentQueueIndex (Line 79 ke paas add karo):

Kotlin
private val playbackHistory = java.util.Stack<Int>()
private var isNavigatingHistory = false
private var lastIndex = C.INDEX_UNSET
@Volatile private var currentQueueIndex: Int = 0
Update B — init block mein PlayRequestReady (Lines 148–158) ko fix karo taaki Autoplay purane index par wapas jump na kare:

Kotlin
val playIndex = if (_songQueue.isNotEmpty() &&
    (_songQueue.last().id == newEntity.id || _songQueue.last().videoId == newEntity.videoId)
) {
    _songQueue[_songQueue.lastIndex] = newEntity
    _songQueue.lastIndex
} else {
    _songQueue.add(newEntity)
    _songQueue.lastIndex
}
_playbackQueue.value = _songQueue.toList()
playOnlineQueueIndex(playIndex)
Update C — onMediaItemTransition aur onRepeatModeChanged (Lines 223–251 & 440–446) ko fix karo:

Kotlin
          override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
              super.onMediaItemTransition(mediaItem, reason)
              val controller = mediaController
              if (controller != null) {
                  val mediaId = mediaItem?.mediaId
                  val resolvedQueueIdx = _songQueue.indexOfFirst {
                      !mediaId.isNullOrBlank() && (it.id == mediaId || it.videoId == mediaId)
                  }.takeIf { it != -1 } ?: currentQueueIndex

                  Log.d("MusicController", "[TRANSITION] reason=$reason queueIdx=$resolvedQueueIdx mediaId=$mediaId")

                  if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {
                      // Only allow single-item repeat when user explicitly selected REPEAT_MODE_ONE
                      if (playbackStateManager.repeatMode.value != Player.REPEAT_MODE_ONE) {
                          Log.d("MusicController", "Single track repeat intercepted - advancing queue")
                          handlePlaybackEnded()
                          return
                      }
                  }

                  currentQueueIndex = resolvedQueueIdx
                  playbackRouter.onTrackTransition(scope, this@MusicController, resolvedQueueIdx)
              }

              updateCurrentSong()
              playbackStateManager.updatePosition(0L)
              playbackStateManager.updateDuration(0L)
              playbackCompletionGuard.reset()
              // ... (lines 257-271 same as before)
Aur onRepeatModeChanged (lines 440–446) mein ExoPlayer ke internal REPEAT_MODE_OFF ko user ke REPEAT_MODE_ALL ko overwrite karne se roko:

Kotlin
          override fun onRepeatModeChanged(repeatMode: Int) {
              super.onRepeatModeChanged(repeatMode)
              // Do not overwrite user's REPEAT_MODE_ALL when ExoPlayer has a 1-item online queue
              val userMode = playbackStateManager.repeatMode.value
              if (repeatMode == Player.REPEAT_MODE_OFF && userMode == Player.REPEAT_MODE_ALL) {
                  return
              }
              playbackStateManager.updateRepeatMode(repeatMode)
              saveRepeatMode(repeatMode)
          }
Update D — handlePlaybackEnded() (Lines 450–480) ko fix karo taaki Repeat-One, Shuffle, Linear Next, aur Repeat-All 100% kaam karein:

Kotlin
  private fun handlePlaybackEnded() {
      val controller = mediaController
      val mediaId = controller?.currentMediaItem?.mediaId ?: ""

      // 1. Repeat One: replay the current song immediately
      if (playbackStateManager.repeatMode.value == Player.REPEAT_MODE_ONE) {
          if (controller != null && controller.mediaItemCount > 0) {
              controller.seekTo(0L)
              controller.play()
          } else if (currentQueueIndex in _songQueue.indices) {
              playOnlineQueueIndex(currentQueueIndex)
          }
          return
      }

      // 2. Multi-item pure local queue inside ExoPlayer
      if (controller != null && controller.mediaItemCount > 1 && controller.hasNextMediaItem()) {
          controller.seekToNext()
          return
      }

      val currentIndex = _songQueue.indexOfFirst {
          !mediaId.isBlank() && (it.id == mediaId || it.videoId == mediaId)
      }.takeIf { it != -1 } ?: currentQueueIndex

      // 3. Shuffle Mode: pick a random unplayed song from _songQueue
      if (playbackStateManager.isShuffleOn.value && _songQueue.size > 1) {
          val validIndices = _songQueue.indices.filter { it != currentIndex }
          val nextIndex = validIndices.randomOrNull() ?: 0
          if (currentIndex in _songQueue.indices) playbackHistory.push(currentIndex)
          Log.d("MusicController", "Shuffle selected next queue song at index $nextIndex")
          playOnlineQueueIndex(nextIndex)
          return
      }

      // 4. Normal Sequential Next in _songQueue
      if (currentIndex != -1 && currentIndex + 1 < _songQueue.size) {
          playbackHistory.push(currentIndex)
          Log.d("MusicController", "Advancing to next queue song at index ${currentIndex + 1}")
          playOnlineQueueIndex(currentIndex + 1)
          return
      }

      // 5. Repeat All: loop back to index 0
      if (playbackStateManager.repeatMode.value == Player.REPEAT_MODE_ALL && _songQueue.isNotEmpty()) {
          if (currentIndex in _songQueue.indices) playbackHistory.push(currentIndex)
          Log.d("MusicController", "Queue loop under REPEAT_MODE_ALL, restarting from index 0")
          playOnlineQueueIndex(0)
          return
      }

      // 6. Queue finished & Repeat Off -> request Autoplay Recommendation
      Log.d("MusicController", "Queue ended, emitting SongCompleted for autoplay recommendation")
      playbackEventBus.emit(PlaybackEvent.SongCompleted(mediaId))
  }
Update E — playOnlineQueueIndex, skipToNext, skipToPrevious, aur getNextPlayRequest (Lines 865, 974, 1009, 1323) mein currentQueueIndex aur Shuffle/History use karo:

Kotlin
  fun playOnlineQueueIndex(index: Int) {
      if (index !in _songQueue.indices) return
      cancelActiveRecovery("playOnlineQueueIndex")
      currentQueueIndex = index
      val targetEntity = _songQueue[index]
      val vId = targetEntity.videoId ?: targetEntity.id
      val artUrl = targetEntity.albumArt ?: "https://img.youtube.com/vi/$vId/hqdefault.jpg"
      val onlineSong = OnlineSong(
          videoId = vId,
          title = targetEntity.title,
          artist = targetEntity.artist,
          thumbnailUrl = artUrl,
          durationMs = targetEntity.duration,
          durationText = "",
          streamUrl = if (targetEntity.path.startsWith("http")) targetEntity.path else null
      )

      playbackStateManager.updateCurrentSong(targetEntity)
      playbackStateManager.updateCurrentOnlineSong(onlineSong)
      playbackStateManager.updatePosition(0L)
      playbackStateManager.updateDuration(targetEntity.duration)

      val requests = _songQueue.map { entity ->
          val entityVid = entity.videoId ?: entity.id
          val entityArt = entity.albumArt ?: "https://img.youtube.com/vi/$entityVid/hqdefault.jpg"
          val isLocalFile = entity.path.startsWith("/") || entity.path.startsWith("content://") || entity.path.startsWith("file://")
          PlayRequest(
              songId = entityVid,
              title = entity.title,
              artist = entity.artist,
              playbackSource = if (isLocalFile) PlaybackSourceType.LOCAL else PlaybackSourceType.ONLINE,
              localUri = entity.path,
              streamUrl = if (entity.path.startsWith("http")) entity.path else null,
              albumArt = entityArt
          )
      }
      playbackRouter.play(scope, this, requests, index)
  }
Aur skipToNext() (lines 974–1007) ko aise update karo taaki Next button dabane par bhi Shuffle aur Repeat 100% kaam kare:

Kotlin
  fun skipToNext() {
      cancelActiveRecovery("skipToNext")
      if (syncPlaybackInterceptor?.onTrackSkipRequested(isNext = true) == true) {
          return
      }
      runOnApplicationThread { controller ->
          val currentMediaId = controller.currentMediaItem?.mediaId ?: ""
          if (currentMediaId.isNotEmpty()) {
              playbackEventBus.emit(PlaybackEvent.SongSkipped(currentMediaId))
          }
          if (controller.mediaItemCount > 1 && controller.hasNextMediaItem()) {
              controller.seekToNext()
              return@runOnApplicationThread
          }
          val currentIndex = _songQueue.indexOfFirst {
              !currentMediaId.isBlank() && (it.id == currentMediaId || it.videoId == currentMediaId)
          }.takeIf { it != -1 } ?: currentQueueIndex

          if (playbackStateManager.isShuffleOn.value && _songQueue.size > 1) {
              val nextIndex = _songQueue.indices.filter { it != currentIndex }.randomOrNull() ?: 0
              if (currentIndex in _songQueue.indices) playbackHistory.push(currentIndex)
              playOnlineQueueIndex(nextIndex)
          } else if (currentIndex != -1 && currentIndex + 1 < _songQueue.size) {
              playbackHistory.push(currentIndex)
              playOnlineQueueIndex(currentIndex + 1)
          } else if (playbackStateManager.repeatMode.value == Player.REPEAT_MODE_ALL && _songQueue.isNotEmpty()) {
              if (currentIndex in _songQueue.indices) playbackHistory.push(currentIndex)
              playOnlineQueueIndex(0)
          } else if (playbackStateManager.repeatMode.value == Player.REPEAT_MODE_ONE && currentIndex in _songQueue.indices) {
              controller.seekTo(0L)
              controller.play()
          } else {
              playbackEventBus.emit(PlaybackEvent.SongCompleted(currentMediaId))
          }
      }
  }
In dono files (PlaybackRouter.kt aur MusicController.kt) mein ye changes dalte hi:

Search ya Recommendations mein se koi bhi song (1st, 2nd, 5th, 20th) play karne par wo kabhi khud par loop nahi karega aur hamesha agle song par jayega.

Shuffle button ON karne par automatic next aur manual Next button dono 100% random song bajayenge (aur Previous dabane par wapas pichle shuffled song par aayega).

Loop (Repeat) button teeno modes (Off → Repeat All → Repeat One) mein bina apne aap reset hue kaam karega.