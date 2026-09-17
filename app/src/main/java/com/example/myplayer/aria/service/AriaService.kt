package com.example.myplayer.aria.service

import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.aria.connector.AppCommand
import com.example.aria.connector.AppResponse
import com.example.aria.connector.Capability
import com.example.aria.connector.sdk.MusicConnectorService
import com.example.myplayer.aria.model.*
import com.example.myplayer.aria.providers.*
import com.example.myplayer.aria.recovery.AriaStateRecoveryManager
import com.example.myplayer.aria.security.CallerVerifier
import com.example.myplayer.data.repository.MusicRepository
import com.example.myplayer.playback.MusicController
import com.example.myplayer.playback.PlaybackEvent
import com.example.myplayer.playback.PlaybackEventBus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import java.util.concurrent.ConcurrentSkipListSet
import javax.inject.Inject

@AndroidEntryPoint
class AriaService : MusicConnectorService() {
    companion object {
        private const val TAG = "AriaService"
    }

    @Inject
    lateinit var callerVerifier: CallerVerifier

    @Inject
    lateinit var stateRecoveryManager: AriaStateRecoveryManager

    @Inject
    lateinit var musicController: MusicController

    @Inject
    lateinit var musicRepository: MusicRepository

    @Inject
    lateinit var playbackEventBus: PlaybackEventBus

    @Inject
    lateinit var playbackController: PlaybackController

    @Inject
    lateinit var searchProvider: SearchProvider

    @Inject
    lateinit var playlistProvider: PlaylistProvider

    @Inject
    lateinit var queueProvider: QueueProvider

    @Inject
    lateinit var downloadProvider: DownloadProvider

    @Inject
    lateinit var metadataProvider: MetadataProvider

    @Inject
    lateinit var historyProvider: HistoryProvider

    @Inject
    lateinit var recommendationProvider: RecommendationProvider

    @Inject
    lateinit var equalizerProvider: EqualizerProvider

    @Inject
    lateinit var lyricsProvider: LyricsProvider

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Tracks seen WorkInfo task states to prevent duplicate events
    private val seenDownloadStarts = ConcurrentSkipListSet<String>()
    private val seenDownloadCompletes = ConcurrentSkipListSet<String>()
    private val seenDownloadFailures = ConcurrentSkipListSet<String>()

    override fun appName(): String = "MyPlayer"

    override fun supportedCapabilities(): Set<Capability> = setOf(
        Capability.PLAY, Capability.PAUSE, Capability.RESUME, Capability.STOP,
        Capability.NEXT, Capability.PREVIOUS, Capability.SEEK, Capability.SET_VOLUME,
        Capability.SET_REPEAT, Capability.SET_SHUFFLE, Capability.GET_CURRENT_TRACK,
        Capability.GET_QUEUE, Capability.GET_HISTORY, Capability.SEARCH,
        Capability.ADD_TO_QUEUE, Capability.CREATE_PLAYLIST, Capability.DOWNLOAD,
        Capability.GET_LYRICS, Capability.GET_RECOMMENDATIONS, Capability.SLEEP_TIMER,
        Capability.SET_EQUALIZER
    )

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AriaService creating...")

        // Restore playback state asynchronously (Task 9: State Recovery)
        serviceScope.launch {
            stateRecoveryManager.restoreState(musicController)
            startStateObservers()
        }
    }

    private fun startStateObservers() {
        // 1. Observe Playback Event Bus for song-started and ended triggers
        serviceScope.launch {
            playbackEventBus.events.collect { event ->
                when (event) {
                    is PlaybackEvent.SongStarted -> {
                        val song = musicRepository.getSongById(event.songId)
                        if (song != null) {
                            notifyTrackChanged(song.title, song.artist)
                        } else {
                            notifyTrackChanged(event.songId, "")
                        }
                    }
                    is PlaybackEvent.SongCompleted -> {
                        notifyPlaybackStopped()
                    }
                    else -> {}
                }
            }
        }

        // 2. Observe playing state changes for song paused notifications
        serviceScope.launch {
            musicController.isPlaying
                .collect { isPlaying ->
                    if (!isPlaying) {
                        notifyPlaybackPaused()
                    }
                }
        }

        // 3. Observe WorkManager for download lifecycle broadcasts
        serviceScope.launch {
            try {
                WorkManager.getInstance(this@AriaService)
                    .getWorkInfosByTagFlow("download")
                    .collectLatest { workInfos ->
                        for (info in workInfos) {
                            val videoIdTag = info.tags.firstOrNull { it.startsWith("videoId_") }
                            val videoId = videoIdTag?.substringAfter("videoId_") ?: continue

                            val idStr = info.id.toString()
                            when (info.state) {
                                WorkInfo.State.SUCCEEDED -> {
                                    if (seenDownloadCompletes.add(idStr)) {
                                        val song = musicRepository.getSongById(videoId)
                                        notifyDownloadComplete(song?.title ?: videoId)
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error tracking WorkManager downloads", e)
            }
        }

        // 4. Auto-persist playback state for future recovery on process death (Task 9)
        serviceScope.launch {
            combine(
                musicController.currentSong,
                musicController.repeatMode,
                musicController.isShuffleOn
            ) { currentSong, repeatMode, shuffleOn ->
                val queueIds = musicController.getSongQueue().map { it.id }
                stateRecoveryManager.saveState(
                    queueIds = queueIds,
                    currentSongId = currentSong?.id,
                    positionMs = musicController.getCurrentPosition(),
                    repeatMode = repeatMode,
                    shuffleMode = shuffleOn
                )
            }.collect {}
        }

        // 5. Ticker to continuously persist playback positions
        serviceScope.launch {
            while (isActive) {
                delay(5000L) // every 5 seconds
                val currentSong = musicController.currentSong.value
                val queueIds = musicController.getSongQueue().map { it.id }
                stateRecoveryManager.saveState(
                    queueIds = queueIds,
                    currentSongId = currentSong?.id,
                    positionMs = musicController.getCurrentPosition(),
                    repeatMode = musicController.repeatMode.value,
                    shuffleMode = musicController.isShuffleOn.value
                )
            }
        }
    }

    override fun onBind(intent: Intent): IBinder {
        Log.d(TAG, "AriaService onBind called")
        if (!callerVerifier.isCallerTrusted()) {
            Log.e(TAG, "onBind aborted: untrusted client signature.")
            throw SecurityException("Untrusted client signature.")
        }
        return super.onBind(intent)
    }

    override fun onDestroy() {
        Log.d(TAG, "AriaService destroying...")
        serviceScope.cancel()
        super.onDestroy()
    }

    // ── MusicConnectorService Implementations ───────────────────────────────

    override suspend fun onPlay(
        query: String,
        queryType: AppCommand.Music.Play.QueryType,
        mood: String?
    ): AppResponse = withContext(Dispatchers.Main) {
        val source = if (query.startsWith("online://") || query.startsWith("http")) "online" else "local"
        val response = playbackController.play(query, source, AriaCommand.PLAY.ordinal, null)
        response.toAppResponse()
    }

    override suspend fun onPause(): AppResponse = withContext(Dispatchers.Main) {
        playbackController.pause(AriaCommand.PAUSE.ordinal, null).toAppResponse()
    }

    override suspend fun onResume(): AppResponse = withContext(Dispatchers.Main) {
        playbackController.resume(AriaCommand.RESUME.ordinal, null).toAppResponse()
    }

    override suspend fun onNext(): AppResponse = withContext(Dispatchers.Main) {
        playbackController.next(AriaCommand.NEXT.ordinal, null).toAppResponse()
    }

    override suspend fun onPrevious(): AppResponse = withContext(Dispatchers.Main) {
        playbackController.previous(AriaCommand.PREVIOUS.ordinal, null).toAppResponse()
    }

    override suspend fun onStop(): AppResponse = withContext(Dispatchers.Main) {
        playbackController.stop(AriaCommand.STOP.ordinal, null).toAppResponse()
    }

    override suspend fun onSeek(positionMs: Long): AppResponse = withContext(Dispatchers.Main) {
        playbackController.seek(positionMs, AriaCommand.SEEK.ordinal, null).toAppResponse()
    }

    override suspend fun onSetVolume(percent: Int): AppResponse = withContext(Dispatchers.Main) {
        playbackController.setVolume(percent.toFloat() / 100f, AriaCommand.VOLUME.ordinal, null).toAppResponse()
    }

    override suspend fun onSetRepeat(mode: AppCommand.Music.SetRepeat.RepeatMode): AppResponse = withContext(Dispatchers.Main) {
        val legacyMode = when (mode) {
            AppCommand.Music.SetRepeat.RepeatMode.OFF -> 0
            AppCommand.Music.SetRepeat.RepeatMode.ONE -> 1
            AppCommand.Music.SetRepeat.RepeatMode.ALL -> 2
        }
        playbackController.setRepeatMode(legacyMode, AriaCommand.REPEAT.ordinal, null).toAppResponse()
    }

    override suspend fun onSetShuffle(enabled: Boolean): AppResponse = withContext(Dispatchers.Main) {
        playbackController.setShuffle(enabled, AriaCommand.SHUFFLE.ordinal, null).toAppResponse()
    }

    override suspend fun onGetCurrentTrack(): AppResponse = withContext(Dispatchers.Main) {
        metadataProvider.getCurrentSong(AriaCommand.GET_CURRENT_SONG.ordinal, null).toAppResponse()
    }

    override suspend fun onGetQueue(): AppResponse = withContext(Dispatchers.Main) {
        queueProvider.getQueue(AriaCommand.GET_QUEUE.ordinal, null).toAppResponse()
    }

    override suspend fun onGetHistory(): AppResponse = withContext(Dispatchers.Main) {
        historyProvider.getListeningHistory(20, AriaCommand.GET_LISTENING_HISTORY.ordinal, null).toAppResponse()
    }

    override suspend fun onSearch(query: String): AppResponse = withContext(Dispatchers.Main) {
        searchProvider.search(query, null, AriaCommand.SEARCH_SONG.ordinal, null).toAppResponse()
    }

    override suspend fun onAddToQueue(query: String): AppResponse = withContext(Dispatchers.Main) {
        queueProvider.queueSong(query, AriaCommand.QUEUE_SONG.ordinal, null).toAppResponse()
    }

    override suspend fun onCreatePlaylist(name: String): AppResponse = withContext(Dispatchers.Main) {
        playlistProvider.createPlaylist(name, AriaCommand.CREATE_PLAYLIST.ordinal, null).toAppResponse()
    }

    override suspend fun onDownload(trackId: String): AppResponse = withContext(Dispatchers.Main) {
        downloadProvider.downloadSong(trackId, "", "", AriaCommand.DOWNLOAD_SONG.ordinal, null).toAppResponse()
    }

    override suspend fun onGetLyrics(): AppResponse = withContext(Dispatchers.Main) {
        lyricsProvider.getLyrics(AriaCommand.GET_LYRICS.ordinal, null).toAppResponse()
    }

    override suspend fun onGetRecommendations(mood: String?, context: String?): AppResponse = withContext(Dispatchers.Main) {
        recommendationProvider.getRecommendations(AriaCommand.GET_RECOMMENDATIONS.ordinal, null).toAppResponse()
    }

    override suspend fun onSetSleepTimer(minutes: Int): AppResponse = withContext(Dispatchers.Main) {
        if (minutes >= 0) {
            musicController.startSleepTimer(minutes)
            AppResponse.Success(mapOf("message" to "Sleep timer set for $minutes minutes"))
        } else {
            AppResponse.Error(AppResponse.Error.ErrorCode.COMMAND_FAILED, "Invalid sleep duration: $minutes")
        }
    }

    override suspend fun onSetEqualizer(preset: String): AppResponse = withContext(Dispatchers.Main) {
        equalizerProvider.applyPreset(preset, AriaCommand.EQUALIZER.ordinal, null).toAppResponse()
    }

    // ── Helper to convert local AriaBaseResponse to type-safe AppResponse ──

    private fun AriaBaseResponse.toAppResponse(): AppResponse {
        return when (this.status) {
            AriaStatus.SUCCESS -> {
                val dataMap = mutableMapOf<String, String>()
                when (this) {
                    is TrackResponse -> {
                        this.song?.let {
                            dataMap["id"] = it.id
                            dataMap["title"] = it.title
                            dataMap["artist"] = it.artist
                            dataMap["album"] = it.album
                            dataMap["durationMs"] = it.durationMs.toString()
                            dataMap["path"] = it.path ?: ""
                            dataMap["albumArtUrl"] = it.albumArtUrl ?: ""
                            dataMap["isOnline"] = it.isOnline.toString()
                        }
                        dataMap["positionMs"] = this.positionMs.toString()
                        dataMap["durationMs"] = this.durationMs.toString()
                        dataMap["isPlaying"] = this.isPlaying.toString()
                    }
                    is PlaylistResponse -> {
                        dataMap["playlistId"] = this.playlistId.toString()
                        dataMap["playlistName"] = this.playlistName
                        dataMap["songs"] = this.songs.joinToString(",") { it.id }
                    }
                    is QueueResponse -> {
                        this.queue?.let { q ->
                            val currentSongId = if (q.currentIndex in q.songs.indices) q.songs[q.currentIndex].id else ""
                            dataMap["currentSongId"] = currentSongId
                            dataMap["songs"] = q.songs.joinToString(",") { it.id }
                        }
                    }
                    is HistoryResponse -> {
                        dataMap["songs"] = this.songs.joinToString(",") { it.id }
                    }
                    is LyricsResponse -> {
                        dataMap["songId"] = this.songId
                        dataMap["lyrics"] = this.lyrics ?: ""
                    }
                    is RecommendationResponse -> {
                        dataMap["songs"] = this.songs.joinToString(",") { it.id }
                    }
                    is AriaGeneralResponse -> {
                        this.message?.let { dataMap["message"] = it }
                    }
                    is HealthResponse -> {
                        dataMap["version"] = this.version
                        dataMap["state"] = this.state
                        dataMap["playbackState"] = this.playbackState
                        dataMap["queueSize"] = this.queueSize.toString()
                        dataMap["memoryUsageBytes"] = this.memoryUsageBytes.toString()
                        dataMap["capabilities"] = this.capabilities.joinToString(",")
                    }
                }
                AppResponse.Success(dataMap)
            }
            AriaStatus.PERMISSION_DENIED -> AppResponse.Error(AppResponse.Error.ErrorCode.PERMISSION_DENIED, "Permission denied")
            AriaStatus.NOT_AVAILABLE -> AppResponse.Error(AppResponse.Error.ErrorCode.COMMAND_FAILED, "Feature not available")
            else -> AppResponse.Error(AppResponse.Error.ErrorCode.COMMAND_FAILED, "Command failed")
        }
    }
}
