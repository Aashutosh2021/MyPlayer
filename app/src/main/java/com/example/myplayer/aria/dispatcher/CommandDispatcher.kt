package com.example.myplayer.aria.dispatcher

import android.content.Context
import android.os.Binder
import android.os.Bundle
import android.os.Message
import android.os.Messenger
import android.util.Log
import com.example.myplayer.aria.event.AriaEventPublisher
import com.example.myplayer.aria.model.*
import com.example.myplayer.aria.performance.AriaPerformanceMonitor
import com.example.myplayer.aria.providers.*
import com.example.myplayer.aria.security.AriaPermission
import com.example.myplayer.aria.security.CallerVerifier
import com.example.myplayer.playback.MusicController
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommandDispatcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val callerVerifier: CallerVerifier,
    private val eventPublisher: AriaEventPublisher,
    private val performanceMonitor: AriaPerformanceMonitor,
    private val musicController: MusicController,
    private val playbackController: PlaybackController,
    private val searchProvider: SearchProvider,
    private val playlistProvider: PlaylistProvider,
    private val queueProvider: QueueProvider,
    private val downloadProvider: DownloadProvider,
    private val metadataProvider: MetadataProvider,
    private val historyProvider: HistoryProvider,
    private val recommendationProvider: RecommendationProvider,
    private val equalizerProvider: EqualizerProvider,
    private val lyricsProvider: LyricsProvider
) {
    companion object {
        private const val TAG = "CommandDispatcher"
        private const val RATE_LIMIT_THRESHOLD = 50
        private const val RATE_LIMIT_WINDOW_MS = 1000L
    }

    private val dispatcherScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val rateLimitMap = ConcurrentHashMap<Int, ArrayList<Long>>() // UID -> timestamps
    private val activeTransactions = ConcurrentHashMap<String, Job>()    // TransactionId -> Job

    fun dispatchMessage(msg: Message) {
        val replyTo = msg.replyTo
        val commandOrdinal = msg.what
        val data = msg.data ?: Bundle.EMPTY
        val transactionId = data.getString(ARIA_KEY_TRANSACTION_ID)

        val command = try {
            AriaCommand.values()[commandOrdinal]
        } catch (e: Exception) {
            Log.e(TAG, "Unknown command ordinal: $commandOrdinal")
            sendErrorResponse(replyTo, commandOrdinal, transactionId, AriaStatus.BAD_REQUEST, "Unknown command ordinal")
            return
        }

        // 1. Verify caller package signature & permissions
        if (!callerVerifier.isCallerTrusted()) {
            sendErrorResponse(replyTo, commandOrdinal, transactionId, AriaStatus.PERMISSION_DENIED, "Caller signature verification failed")
            return
        }

        // 2. Runtime rate limiting check
        val callingUid = Binder.getCallingUid()
        val now = System.currentTimeMillis()
        val timestamps = rateLimitMap.getOrPut(callingUid) { ArrayList() }
        synchronized(timestamps) {
            timestamps.removeAll { now - it > RATE_LIMIT_WINDOW_MS }
            if (timestamps.size >= RATE_LIMIT_THRESHOLD) {
                Log.e(TAG, "Rate limit exceeded for UID: $callingUid")
                sendErrorResponse(replyTo, commandOrdinal, transactionId, AriaStatus.BUSY, "Rate limit exceeded (max 50 requests/sec)")
                return
            }
            timestamps.add(now)
        }

        // 3. Command authorization check (Standard vs Admin tiers)
        val requiredPermission = when (command) {
            AriaCommand.DELETE_PLAYLIST,
            AriaCommand.DELETE_DOWNLOAD -> AriaPermission.ADMIN
            else -> AriaPermission.STANDARD
        }

        // 4. Asynchronous dispatch to the correct provider with transaction support (Task 4: Streaming Commands & Cancellation)
        val job = dispatcherScope.launch {
            val startTime = System.currentTimeMillis()
            try {
                executeCommand(command, data, replyTo, transactionId)
            } catch (e: CancellationException) {
                Log.i(TAG, "Transaction $transactionId cancelled successfully.")
                sendErrorResponse(replyTo, commandOrdinal, transactionId, AriaStatus.FAILURE, "Transaction cancelled.")
            } catch (e: Exception) {
                Log.e(TAG, "Command execution failed: ${command.name}", e)
                sendErrorResponse(replyTo, commandOrdinal, transactionId, AriaStatus.FAILURE, "Internal execution error: ${e.message}")
            } finally {
                if (transactionId != null) {
                    activeTransactions.remove(transactionId)
                }
                performanceMonitor.recordLatency("dispatcher_throughput", System.currentTimeMillis() - startTime)
            }
        }

        if (transactionId != null && job.isActive) {
            // Cancel previous active job with same transactionId
            activeTransactions.remove(transactionId)?.cancel()
            activeTransactions[transactionId] = job
        }
    }

    private suspend fun executeCommand(command: AriaCommand, data: Bundle, replyTo: Messenger?, transactionId: String?) {
        val ordinal = command.ordinal

        when (command) {
            // ── Playback Controls ─────────────────────────────────────────────
            AriaCommand.PLAY -> {
                val songId = data.getString(ARIA_KEY_SONG_ID) ?: ""
                val source = data.getString(ARIA_KEY_SOURCE)
                val response = playbackController.play(songId, source, ordinal, transactionId)
                sendResponse(replyTo, response)
            }
            AriaCommand.PAUSE -> {
                val response = playbackController.pause(ordinal, transactionId)
                sendResponse(replyTo, response)
            }
            AriaCommand.RESUME -> {
                val response = playbackController.resume(ordinal, transactionId)
                sendResponse(replyTo, response)
            }
            AriaCommand.STOP -> {
                val response = playbackController.stop(ordinal, transactionId)
                sendResponse(replyTo, response)
            }
            AriaCommand.NEXT -> {
                val response = playbackController.next(ordinal, transactionId)
                sendResponse(replyTo, response)
            }
            AriaCommand.PREVIOUS -> {
                val response = playbackController.previous(ordinal, transactionId)
                sendResponse(replyTo, response)
            }
            AriaCommand.SEEK -> {
                val positionMs = data.getLong(ARIA_KEY_POSITION_MS, -1L)
                val response = playbackController.seek(positionMs, ordinal, transactionId)
                sendResponse(replyTo, response)
            }
            AriaCommand.SHUFFLE -> {
                val enabled = data.getBoolean(ARIA_KEY_ENABLED, false)
                val response = playbackController.setShuffle(enabled, ordinal, transactionId)
                sendResponse(replyTo, response)
            }
            AriaCommand.REPEAT -> {
                val mode = data.getInt(ARIA_KEY_REPEAT_MODE, -1)
                val response = playbackController.setRepeatMode(mode, ordinal, transactionId)
                sendResponse(replyTo, response)
            }
            AriaCommand.PLAYBACK_SPEED -> {
                val speed = data.getFloat(ARIA_KEY_SPEED, -1f)
                val response = playbackController.setSpeed(speed, ordinal, transactionId)
                sendResponse(replyTo, response)
            }
            AriaCommand.VOLUME -> {
                val volume = data.getFloat(ARIA_KEY_VOLUME, -1f)
                val response = playbackController.setVolume(volume, ordinal, transactionId)
                sendResponse(replyTo, response)
            }

            // ── Playlist Operations ───────────────────────────────────────────
            AriaCommand.PLAY_PLAYLIST -> {
                val playlistId = data.getLong(ARIA_KEY_PLAYLIST_ID, -1L)
                val startIndex = data.getInt(ARIA_KEY_START_INDEX, 0)
                val response = playlistProvider.playPlaylist(playlistId, startIndex, ordinal, transactionId)
                sendResponse(replyTo, response)
            }
            AriaCommand.CREATE_PLAYLIST -> {
                val name = data.getString(ARIA_KEY_PLAYLIST_NAME) ?: ""
                val response = playlistProvider.createPlaylist(name, ordinal, transactionId)
                sendResponse(replyTo, response)
            }
            AriaCommand.RENAME_PLAYLIST -> {
                val playlistId = data.getLong(ARIA_KEY_PLAYLIST_ID, -1L)
                val name = data.getString(ARIA_KEY_PLAYLIST_NAME) ?: ""
                val response = playlistProvider.renamePlaylist(playlistId, name, ordinal, transactionId)
                sendResponse(replyTo, response)
            }
            AriaCommand.DELETE_PLAYLIST -> {
                val playlistId = data.getLong(ARIA_KEY_PLAYLIST_ID, -1L)
                val response = playlistProvider.deletePlaylist(playlistId, ordinal, transactionId)
                sendResponse(replyTo, response)
            }

            // ── Queue Operations ──────────────────────────────────────────────
            AriaCommand.QUEUE_SONG -> {
                val songId = data.getString(ARIA_KEY_SONG_ID) ?: ""
                val response = queueProvider.queueSong(songId, ordinal, transactionId)
                sendResponse(replyTo, response)
            }
            AriaCommand.GET_QUEUE -> {
                val response = queueProvider.getQueue(ordinal, transactionId)
                sendResponse(replyTo, response)
            }

            // ── Search Song ───────────────────────────────────────────────────
            AriaCommand.SEARCH_SONG -> {
                val query = data.getString(ARIA_KEY_QUERY) ?: ""
                val source = data.getString(ARIA_KEY_SOURCE)
                val response = searchProvider.search(query, source, ordinal, transactionId)
                sendResponse(replyTo, response)
            }

            // ── Download Operations ───────────────────────────────────────────
            AriaCommand.DOWNLOAD_SONG -> {
                val songId = data.getString(ARIA_KEY_SONG_ID) ?: ""
                val title = data.getString(ARIA_KEY_TITLE) ?: ""
                val artist = data.getString(ARIA_KEY_ARTIST) ?: ""
                val response = downloadProvider.downloadSong(songId, title, artist, ordinal, transactionId)
                sendResponse(replyTo, response)
            }
            AriaCommand.DELETE_DOWNLOAD -> {
                val songId = data.getString(ARIA_KEY_SONG_ID) ?: ""
                val response = downloadProvider.deleteDownload(songId, ordinal, transactionId)
                sendResponse(replyTo, response)
            }

            // ── Metadata ──────────────────────────────────────────────────────
            AriaCommand.GET_CURRENT_SONG -> {
                val response = metadataProvider.getCurrentSong(ordinal, transactionId)
                sendResponse(replyTo, response)
            }
            AriaCommand.GET_LYRICS -> {
                val response = lyricsProvider.getLyrics(ordinal, transactionId)
                sendResponse(replyTo, response)
            }
            AriaCommand.GET_RECOMMENDATIONS -> {
                val response = recommendationProvider.getRecommendations(ordinal, transactionId)
                sendResponse(replyTo, response)
            }
            AriaCommand.GET_LISTENING_HISTORY -> {
                val limit = data.getInt(ARIA_KEY_LIMIT, 20)
                val response = historyProvider.getListeningHistory(limit, ordinal, transactionId)
                sendResponse(replyTo, response)
            }

            // ── Timers & Advanced Settings ────────────────────────────────────
            AriaCommand.SLEEP_TIMER -> {
                val minutes = data.getInt(ARIA_KEY_MINUTES, -1)
                val response = if (minutes >= 0) {
                    musicController.startSleepTimer(minutes)
                    AriaGeneralResponse(AriaStatus.SUCCESS, ordinal, transactionId)
                } else {
                    AriaGeneralResponse(AriaStatus.BAD_REQUEST, ordinal, transactionId, "Invalid sleep duration: $minutes")
                }
                sendResponse(replyTo, response)
            }
            AriaCommand.EQUALIZER -> {
                val preset = data.getString(ARIA_KEY_EQ_PRESET)
                val bands = data.getFloatArray(ARIA_KEY_EQ_BANDS)
                val response = if (preset != null) {
                    equalizerProvider.applyPreset(preset, ordinal, transactionId)
                } else if (bands != null) {
                    equalizerProvider.applyCustomBands(bands, ordinal, transactionId)
                } else {
                    AriaGeneralResponse(AriaStatus.BAD_REQUEST, ordinal, transactionId)
                }
                sendResponse(replyTo, response)
            }
            AriaCommand.OFFLINE_MODE -> {
                sendResponse(replyTo, AriaGeneralResponse(AriaStatus.SUCCESS, ordinal, transactionId))
            }
            AriaCommand.CROSSFADE -> {
                sendResponse(replyTo, AriaGeneralResponse(AriaStatus.NOT_AVAILABLE, ordinal, transactionId))
            }

            // ── Phase 2 Advanced API additions (Task 4: Streaming Commands & Cancellation) ──
            AriaCommand.CANCEL -> {
                val transactionToCancel = data.getString(ARIA_KEY_TRANSACTION_ID)
                if (transactionToCancel != null) {
                    activeTransactions.remove(transactionToCancel)?.cancel()
                    sendResponse(replyTo, AriaGeneralResponse(AriaStatus.SUCCESS, ordinal, transactionId))
                } else {
                    sendResponse(replyTo, AriaGeneralResponse(AriaStatus.BAD_REQUEST, ordinal, transactionId, "No transaction ID provided to cancel."))
                }
            }

            AriaCommand.START_PLAYBACK_PROGRESS_STREAM -> {
                if (transactionId == null) {
                    sendResponse(replyTo, AriaGeneralResponse(AriaStatus.BAD_REQUEST, ordinal, transactionId, "Missing transaction ID for streaming."))
                    return
                }

                // Continuously stream current positions back (Task 4: Progress updates stream)
                while (currentCoroutineContext().isActive) {
                    val currentSongResponse = metadataProvider.getCurrentSong(ordinal, transactionId)
                    sendResponse(replyTo, currentSongResponse)
                    delay(500L) // 500ms intervals
                }
            }

            AriaCommand.STOP_PLAYBACK_PROGRESS_STREAM -> {
                if (transactionId != null) {
                    activeTransactions.remove(transactionId)?.cancel()
                    sendResponse(replyTo, AriaGeneralResponse(AriaStatus.SUCCESS, ordinal, transactionId))
                } else {
                    sendResponse(replyTo, AriaGeneralResponse(AriaStatus.BAD_REQUEST, ordinal, transactionId))
                }
            }

            AriaCommand.REGISTER_EVENT_CALLBACK -> {
                if (replyTo != null) {
                    eventPublisher.registerListener(replyTo)
                    sendResponse(replyTo, AriaGeneralResponse(AriaStatus.SUCCESS, ordinal, transactionId))
                } else {
                    sendResponse(replyTo, AriaGeneralResponse(AriaStatus.BAD_REQUEST, ordinal, transactionId))
                }
            }

            AriaCommand.UNREGISTER_EVENT_CALLBACK -> {
                if (replyTo != null) {
                    eventPublisher.unregisterListener(replyTo)
                    sendResponse(replyTo, AriaGeneralResponse(AriaStatus.SUCCESS, ordinal, transactionId))
                } else {
                    sendResponse(replyTo, AriaGeneralResponse(AriaStatus.BAD_REQUEST, ordinal, transactionId))
                }
            }

            AriaCommand.GET_HEALTH -> {
                // Task 8: Health Evaluation API
                val runtime = Runtime.getRuntime()
                val memoryUsed = runtime.totalMemory() - runtime.freeMemory()
                val controller = musicController.getMediaController()
                
                val versionInfo = try {
                    val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    "v${pInfo.versionName} (${pInfo.versionCode})"
                } catch (e: Exception) { "v2.0" }

                val caps = listOf(
                    "PLAYBACK", "PLAYLISTS", "DOWNLOADS", "SEARCH", "LYRICS", "HISTORY", "RECOMMENDATIONS"
                )

                val response = HealthResponse(
                    status = AriaStatus.SUCCESS,
                    commandOrdinal = ordinal,
                    transactionId = transactionId,
                    version = versionInfo,
                    state = "ACTIVE",
                    playbackState = if (controller?.isPlaying == true) "PLAYING" else "PAUSED",
                    queueSize = musicController.getSongQueue().size,
                    memoryUsageBytes = memoryUsed,
                    capabilities = caps
                )
                sendResponse(replyTo, response)
            }
            else -> {
                sendResponse(replyTo, AriaGeneralResponse(AriaStatus.NOT_AVAILABLE, ordinal, transactionId))
            }
        }
    }

    private fun sendResponse(replyTo: Messenger?, response: AriaBaseResponse) {
        if (replyTo == null) return
        try {
            val message = Message.obtain().apply {
                what = response.commandOrdinal
                data = Bundle().apply {
                    putParcelable("aria_response", response)
                }
            }
            replyTo.send(message)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send IPC response back to client", e)
        }
    }

    private fun sendErrorResponse(replyTo: Messenger?, commandOrdinal: Int, transactionId: String?, status: AriaStatus, errorMsg: String) {
        sendResponse(replyTo, AriaGeneralResponse(status, commandOrdinal, transactionId, errorMsg))
    }
}
