package com.example.myplayer.dualbud

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.myplayer.data.local.entity.SongEntity
import com.example.myplayer.dualbud.audio.AndroidAudioTrackOutput
import com.example.myplayer.dualbud.audio.AudioOutputDevice
import com.example.myplayer.dualbud.audio.ChannelAudioSink
import com.example.myplayer.dualbud.audio.DualChannelMixer
import com.example.myplayer.dualbud.model.DualBudModeState
import com.example.myplayer.dualbud.model.DualChannelId
import com.example.myplayer.dualbud.model.DualChannelState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrator for Dual Bud / Split Channel Playback.
 *
 * Manages two independent ExoPlayer instances (Left and Right), their dedicated
 * [ChannelAudioSink] implementations, real-time PCM mixing via [DualChannelMixer],
 * unified audio focus, volume scaling, and seamless channel swapping.
 */
@UnstableApi
@Singleton
class DualChannelPlaybackManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val cacheDataSourceFactory: androidx.media3.datasource.cache.CacheDataSource.Factory? = null,
    outputDevice: AudioOutputDevice = AndroidAudioTrackOutput()
) {
    companion object {
        private const val TAG = "DualChannelPlayback"
        private const val POSITION_POLL_INTERVAL_MS = 250L
    }

    val mixer: DualChannelMixer = DualChannelMixer(outputDevice)

    private val _state = MutableStateFlow(DualBudModeState())
    val state: StateFlow<DualBudModeState> = _state.asStateFlow()

    private var leftPlayer: ExoPlayer? = null
    private var rightPlayer: ExoPlayer? = null

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var positionPollJob: Job? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    // Audio focus listener for unified dual playback session
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d(TAG, "Audio focus lost permanently. Pausing both channels.")
                pause(DualChannelId.LEFT)
                pause(DualChannelId.RIGHT)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.d(TAG, "Audio focus lost transiently. Pausing both channels.")
                pause(DualChannelId.LEFT)
                pause(DualChannelId.RIGHT)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d(TAG, "Audio focus duck. Ducking both channels by 50%.")
                mixer.leftVolume = (_state.value.leftChannel.volume * 0.5f)
                mixer.rightVolume = (_state.value.rightChannel.volume * 0.5f)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(TAG, "Audio focus gained. Restoring volumes.")
                mixer.leftVolume = _state.value.leftChannel.volume
                mixer.rightVolume = _state.value.rightChannel.volume
            }
        }
    }

    /**
     * Enables Dual Bud Mode, initializing the two independent ExoPlayers and starting the mixer.
     */
    fun enableDualBudMode() {
        if (_state.value.isEnabled) return

        requestAudioFocus()
        mixer.start()

        leftPlayer = createChannelPlayer(DualChannelId.LEFT)
        rightPlayer = createChannelPlayer(DualChannelId.RIGHT)

        _state.update { it.copy(isEnabled = true) }
        startPositionPolling()
    }

    /**
     * Exits Dual Bud Mode, releasing both players and stopping the mixer.
     */
    fun disableDualBudMode() {
        if (!_state.value.isEnabled) return

        stopPositionPolling()

        leftPlayer?.stop()
        leftPlayer?.release()
        leftPlayer = null

        rightPlayer?.stop()
        rightPlayer?.release()
        rightPlayer = null

        mixer.stop()
        abandonAudioFocus()

        _state.update {
            DualBudModeState(
                isEnabled = false,
                isSwapped = false,
                leftChannel = DualChannelState(DualChannelId.LEFT),
                rightChannel = DualChannelState(DualChannelId.RIGHT)
            )
        }
    }

    /**
     * Loads a song into the designated channel player.
     * Keeps the other channel completely unaffected.
     */
    fun loadSong(channel: DualChannelId, song: SongEntity, autoPlay: Boolean = true) {
        if (!_state.value.isEnabled) {
            enableDualBudMode()
        }

        val player = getPlayer(channel) ?: return
        val uri = Uri.parse(song.path)
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMediaId(song.id)
            .build()

        player.setMediaItem(mediaItem)
        player.prepare()
        if (autoPlay) {
            player.play()
        }

        updateChannelState(channel) {
            it.copy(
                song = song,
                isLoading = true,
                isEnded = false,
                errorMessage = null,
                durationMs = song.duration
            )
        }
    }

    /**
     * Toggles play/pause for a specific channel player.
     */
    fun togglePlayPause(channel: DualChannelId) {
        val player = getPlayer(channel) ?: return
        if (player.isPlaying) {
            pause(channel)
        } else {
            play(channel)
        }
    }

    fun play(channel: DualChannelId) {
        val player = getPlayer(channel) ?: return
        if (!hasAudioFocus) requestAudioFocus()
        player.play()
        updateChannelState(channel) { it.copy(isPlaying = true) }
    }

    fun pause(channel: DualChannelId) {
        val player = getPlayer(channel) ?: return
        player.pause()
        updateChannelState(channel) { it.copy(isPlaying = false) }
    }

    /**
     * Seeks on a specific channel player.
     * The other channel's playback timing remains completely untouched.
     */
    fun seekTo(channel: DualChannelId, positionMs: Long) {
        val player = getPlayer(channel) ?: return
        player.seekTo(positionMs)
        updateChannelState(channel) { it.copy(positionMs = positionMs) }
    }

    /**
     * Sets independent volume (0.0f to 1.0f) for a channel inside the PCM mixer.
     */
    fun setVolume(channel: DualChannelId, volume: Float) {
        val safeVol = volume.coerceIn(0.0f, 1.0f)
        if (channel == DualChannelId.LEFT) {
            mixer.leftVolume = safeVol
        } else {
            mixer.rightVolume = safeVol
        }
        updateChannelState(channel) { it.copy(volume = safeVol) }
    }

    /**
     * Swaps physical Left and Right audio routing instantaneously without restarting playback.
     */
    fun swapChannels() {
        mixer.toggleSwap()
        _state.update { it.copy(isSwapped = mixer.isSwapped) }
    }

    private fun getPlayer(channel: DualChannelId): ExoPlayer? {
        return if (channel == DualChannelId.LEFT) leftPlayer else rightPlayer
    }

    private fun createChannelPlayer(channel: DualChannelId): ExoPlayer {
        val renderersFactory = object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return ChannelAudioSink(channel, mixer)
            }
        }

        val mediaSourceFactory = if (cacheDataSourceFactory != null) {
            DefaultMediaSourceFactory(cacheDataSourceFactory)
        } else {
            val httpFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15_000)
                .setReadTimeoutMs(15_000)
            val defaultDsFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, httpFactory)
            DefaultMediaSourceFactory(defaultDsFactory)
        }

        val player = ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        player.addListener(createPlayerListener(channel))
        return player
    }

    private fun createPlayerListener(channel: DualChannelId): Player.Listener {
        return object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        updateChannelState(channel) { it.copy(isLoading = true) }
                    }
                    Player.STATE_READY -> {
                        val duration = getPlayer(channel)?.duration ?: 0L
                        updateChannelState(channel) {
                            it.copy(
                                isLoading = false,
                                durationMs = if (duration > 0) duration else it.durationMs
                            )
                        }
                    }
                    Player.STATE_ENDED -> {
                        updateChannelState(channel) {
                            it.copy(isPlaying = false, isEnded = true, isLoading = false)
                        }
                    }
                    Player.STATE_IDLE -> {
                        // Player idle
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateChannelState(channel) { it.copy(isPlaying = isPlaying) }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "Error on channel $channel: ${error.message}", error)
                updateChannelState(channel) {
                    it.copy(
                        isPlaying = false,
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Playback error"
                    )
                }
                // If both channels have failed or are stopped, safely handle
                checkDualFailure()
            }
        }
    }

    private fun checkDualFailure() {
        val leftErr = _state.value.leftChannel.errorMessage != null
        val rightErr = _state.value.rightChannel.errorMessage != null
        if (leftErr && rightErr) {
            Log.w(TAG, "Both dual-channel players failed. Halting dual mode safely.")
            disableDualBudMode()
        }
    }

    private fun updateChannelState(channel: DualChannelId, transform: (DualChannelState) -> DualChannelState) {
        _state.update { current ->
            if (channel == DualChannelId.LEFT) {
                current.copy(leftChannel = transform(current.leftChannel))
            } else {
                current.copy(rightChannel = transform(current.rightChannel))
            }
        }
    }

    private fun startPositionPolling() {
        positionPollJob?.cancel()
        positionPollJob = managerScope.launch {
            while (isActive) {
                val leftPos = leftPlayer?.currentPosition ?: 0L
                val rightPos = rightPlayer?.currentPosition ?: 0L

                _state.update { current ->
                    current.copy(
                        leftChannel = current.leftChannel.copy(positionMs = leftPos),
                        rightChannel = current.rightChannel.copy(positionMs = rightPos)
                    )
                }
                delay(POSITION_POLL_INTERVAL_MS)
            }
        }
    }

    private fun stopPositionPolling() {
        positionPollJob?.cancel()
        positionPollJob = null
    }

    private fun requestAudioFocus(): Boolean {
        if (hasAudioFocus) return true
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setOnAudioFocusChangeListener(audioFocusChangeListener, mainHandler)
                .build()
            audioFocusRequest = req
            audioManager.requestAudioFocus(req)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        hasAudioFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        return hasAudioFocus
    }

    private fun abandonAudioFocus() {
        if (!hasAudioFocus) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
        hasAudioFocus = false
    }
}
