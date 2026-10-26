package com.example.myplayer.playback

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.example.myplayer.data.preferences.AudioPlaybackState
import com.example.myplayer.data.preferences.AudioPlaybackStore
import com.example.myplayer.data.preferences.AudioSettingsRepository
import com.example.myplayer.playback.dsp.LastWaveDspAudioProcessor
import com.example.myplayer.playback.dsp.MasterAudioDspProcessor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MusicService : MediaSessionService() {

    @Inject
    lateinit var player: ExoPlayer

    @Inject
    lateinit var playbackStateManager: PlaybackStateManager

    @Inject
    lateinit var playbackEventBus: PlaybackEventBus

    @Inject
    lateinit var audioSettingsRepository: AudioSettingsRepository

    @Inject
    lateinit var lastWaveDspProcessor: LastWaveDspAudioProcessor

    @Inject
    lateinit var audioPlaybackStore: AudioPlaybackStore

    @Inject
    lateinit var audioFeatureManagers: AudioFeatureManagers

    @Inject
    lateinit var masterAudioDspProcessor: MasterAudioDspProcessor

    @Volatile private var currentAudioPlaybackState = AudioPlaybackState()

    private var loudnessEnhancer: android.media.audiofx.LoudnessEnhancer? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var mediaSession: MediaSession? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    override fun onCreate() {
        super.onCreate()

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MyPlayer:MusicServiceWakeLock"
        ).apply { setReferenceCounted(false) }

        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiLockMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            WifiManager.WIFI_MODE_FULL_LOW_LATENCY
        } else {
            @Suppress("DEPRECATION")
            WifiManager.WIFI_MODE_FULL_HIGH_PERF
        }
        wifiLock = wifiManager.createWifiLock(wifiLockMode, "MyPlayer:MusicServiceWifiLock").apply {
            setReferenceCounted(false)
        }

        val forwardingPlayer = object : androidx.media3.common.ForwardingPlayer(player) {
            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .build()
            }

            override fun isCommandAvailable(command: Int): Boolean {
                return if (command == Player.COMMAND_SEEK_TO_NEXT ||
                    command == Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM ||
                    command == Player.COMMAND_SEEK_TO_PREVIOUS ||
                    command == Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
                ) {
                    true
                } else {
                    super.isCommandAvailable(command)
                }
            }

            override fun seekToNext() {
                if (player.mediaItemCount > 1 && player.hasNextMediaItem()) {
                    super.seekToNext()
                } else {
                    playbackEventBus.emit(PlaybackEvent.SkipToNextRequested)
                }
            }

            override fun seekToNextMediaItem() {
                seekToNext()
            }

            override fun seekToPrevious() {
                if (player.mediaItemCount > 1 && player.hasPreviousMediaItem()) {
                    super.seekToPrevious()
                } else {
                    playbackEventBus.emit(PlaybackEvent.SkipToPreviousRequested)
                }
            }

            override fun seekToPreviousMediaItem() {
                seekToPrevious()
            }
        }

        val callback = object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val connectionResult = super.onConnect(session, controller)
                val sessionCommands = connectionResult.availableSessionCommands
                val playerCommands = connectionResult.availablePlayerCommands.buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .build()
                return MediaSession.ConnectionResult.accept(sessionCommands, playerCommands)
            }

            override fun onPlaybackResumption(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo
            ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                return Futures.immediateFuture(
                    MediaSession.MediaItemsWithStartPosition(
                        emptyList(),
                        C.INDEX_UNSET,
                        C.TIME_UNSET
                    )
                )
            }
        }

        mediaSession = MediaSession.Builder(this, forwardingPlayer)
            .setCallback(callback)
            .build()

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                // We can broadcast song changes or update recent history here
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                updateServiceLocks(isPlaying || player.playWhenReady)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                val active = player.playWhenReady &&
                    (playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_READY)
                if (active) {
                    acquireLocks()
                }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                super.onRepeatModeChanged(repeatMode)
                getSharedPreferences("playback_preferences", Context.MODE_PRIVATE)
                    .edit().putInt("playback_repeat_mode", repeatMode).apply()
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                super.onShuffleModeEnabledChanged(shuffleModeEnabled)
                getSharedPreferences("playback_preferences", Context.MODE_PRIVATE)
                    .edit().putBoolean("playback_shuffle_mode", shuffleModeEnabled).apply()
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                super.onAudioSessionIdChanged(audioSessionId)
                if (currentAudioPlaybackState.systemAudioEffects) {
                    audioFeatureManagers.toggleSystemAudioEffects(audioSessionId, true)
                }
                serviceScope.launch {
                    val config = audioSettingsRepository.configFlow.firstOrNull()
                    if (config != null) {
                        updateLoudnessEnhancer(config.loudnessNormalize, config.loudnessGainMb)
                    }
                }
            }
        })

        // Master Audio & Playback Feature Integration:
        // 1. Crossfade engine monitor
        audioFeatureManagers.attachCrossfadeMonitor(
            scope = serviceScope,
            exoPlayer = player,
            isEnabled = { currentAudioPlaybackState.crossfadeEnabled },
            crossfadeDurationSec = currentAudioPlaybackState.crossfadeSeconds
        )

        // 2. Observe Master Audio & Playback Preferences (15-Band EQ, Studio Master Clarity, -14 LUFS, Bit-Perfect)
        serviceScope.launch {
            audioPlaybackStore.stateFlow.collect { state ->
                currentAudioPlaybackState = state
                masterAudioDspProcessor.updateState(state)
                audioFeatureManagers.toggleSystemAudioEffects(player.audioSessionId, state.systemAudioEffects)
            }
        }

        // 3. Observe Legacy/Simple Audio Settings (Skip Silence, Clarity Presets, True-Dolby Spatial)
        serviceScope.launch {
            audioSettingsRepository.configFlow.collect { config ->
                try {
                    player.skipSilenceEnabled = config.skipSilence
                } catch (e: Exception) {
                    android.util.Log.w("MusicService", "Failed to set skipSilenceEnabled", e)
                }

                lastWaveDspProcessor.updateDspConfig(
                    preset = config.clarityPreset,
                    dolby = config.trueDolbySpatial,
                    width = config.spatialWidth
                )

                updateLoudnessEnhancer(config.loudnessNormalize, config.loudnessGainMb)
            }
        }
    }

    private fun updateLoudnessEnhancer(enabled: Boolean, gainMb: Int) {
        val sessionId = player.audioSessionId
        if (sessionId != C.AUDIO_SESSION_ID_UNSET && sessionId != 0) {
            try {
                if (loudnessEnhancer == null) {
                    loudnessEnhancer = android.media.audiofx.LoudnessEnhancer(sessionId)
                }
                loudnessEnhancer?.setTargetGain(if (enabled) gainMb else 0)
                loudnessEnhancer?.enabled = enabled
            } catch (e: Exception) {
                android.util.Log.w("MusicService", "Failed to update LoudnessEnhancer", e)
            }
        }
    }

    private fun acquireLocks() {
        if (wakeLock?.isHeld == false) {
            // 4-hour safety timeout so locks never leak indefinitely
            wakeLock?.acquire(4 * 60 * 60 * 1000L)
        }
        if (wifiLock?.isHeld == false) {
            wifiLock?.acquire()
        }
    }

    private fun updateServiceLocks(keepActive: Boolean) {
        if (keepActive) {
            acquireLocks()
        } else if (
            playbackStateManager.playbackMachineState.value != PlaybackMachineState.RESOLVING &&
            playbackStateManager.playbackMachineState.value != PlaybackMachineState.PREPARING &&
            playbackStateManager.playbackMachineState.value != PlaybackMachineState.RECOVERING
        ) {
            releaseLocks()
        }
    }

    private fun releaseLocks() {
        if (wakeLock?.isHeld == true) wakeLock?.release()
        if (wifiLock?.isHeld == true) wifiLock?.release()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    override fun onUpdateNotification(
        session: MediaSession,
        startInForegroundRequired: Boolean
    ) {
        val state = playbackStateManager.playbackMachineState.value
        val isResolvingOrRecovering = state == PlaybackMachineState.RESOLVING ||
            state == PlaybackMachineState.PREPARING ||
            state == PlaybackMachineState.RECOVERING
        val keepForeground = startInForegroundRequired || player.playWhenReady || isResolvingOrRecovering
        if (keepForeground) {
            acquireLocks()
        }
        try {
            super.onUpdateNotification(session, keepForeground)
        } catch (_: Exception) {
            // Suppress ForegroundServiceStartNotAllowedException on Android 12+ background transitions
        }
    }

    override fun onDestroy() {
        releaseLocks()
        try {
            loudnessEnhancer?.release()
            loudnessEnhancer = null
        } catch (_: Exception) {}
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // If the user swipes away the app from recent apps, we should stop the service if it's paused.
        if (!player.playWhenReady || player.playbackState == Player.STATE_ENDED) {
            releaseLocks()
            stopSelf()
        }
    }
}
