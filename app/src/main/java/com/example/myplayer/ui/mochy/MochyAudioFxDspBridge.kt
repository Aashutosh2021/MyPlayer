package com.example.myplayer.ui.mochy

import android.content.Context
import com.example.myplayer.mochyfx.AudioFxService
import com.example.myplayer.mochyfx.audio.LoudnessParams
import com.example.myplayer.mochyfx.audio.ReplayGainMode
import com.example.myplayer.mochyfx.audio.ReplayGainParams
import com.example.myplayer.mochyfx.audio.ReverbParams
import com.example.myplayer.mochyfx.audio.VolumeCurve
import com.example.myplayer.mochyfx.audio.SpatialMode
import com.example.myplayer.mochyfx.audio.SpatialParams
import kotlin.math.pow

/**
 * Audio Lab settings holder. The DSP itself is Mochy's own chain (package mochyfx), running inside
 * the ExoPlayer sink: spatial/surround/mono -> reverb -> loudness, plus the volume curve / ReplayGain.
 * The equalizer is MyPlayer's own (15-band, MasterAudioDspProcessor); setEq() only remembers the UI values.
 * This object turns the Compose UI state into Mochy's parameter objects and hands them to [AudioFxService];
 * the audio processors read those at the start of every buffer, so every slider is live.
 */
object MochyAudioFxDspBridge {
    // ---- 0. Equalizer (10 bands, +-12 dB) ----
    @Volatile var eqEnabled: Boolean = false
    @Volatile private var eqGainsDb: FloatArray = FloatArray(10)
    @Volatile var eqPreampDb: Float = 0f
    @Volatile var eqAutoPreamp: Boolean = true

    // ---- 1. Reverb ----
    @Volatile var reverbEnabled: Boolean = false
    @Volatile var reverbMix: Float = 0.50f
    @Volatile var reverbRoomSize: Float = 0.55f
    @Volatile var reverbDamping: Float = 0.45f
    @Volatile var reverbWet: Float = 0.60f
    @Volatile var reverbDry: Float = 1.00f
    @Volatile var reverbWidth: Float = 1.00f
    @Volatile var reverbPreDelayMs: Float = 12f
    @Volatile var reverbLowCutHz: Float = 180f

    // ---- 2. Loudness ----
    @Volatile var loudnessEnabled: Boolean = false
    @Volatile var loudnessBoostDb: Float = 4.0f
    @Volatile var loudnessCompression: Float = 0.30f
    @Volatile var evenOutSongs: Boolean = true
    @Volatile var replayGainPreampDb: Float = 3.0f

    // ---- 3. Spatial / surround / mono ----
    @Volatile var spatialEnabled: Boolean = false
    @Volatile var spatialIsSpeakersMode: Boolean = false
    @Volatile var headShadowHz: Float = 700f
    @Volatile var crossfeedDb: Float = -4.5f
    @Volatile var speakerAngleDeg: Float = 30f
    @Volatile var spatialAmount: Float = 1.0f
    @Volatile var monoEnabled: Boolean = false
    @Volatile var dolbySurroundEnabled: Boolean = false

    /** Mochy volume curve (power 2), applied by MochyVolumeSink. */
    @Volatile var exponentialVolumeEnabled: Boolean = false

    // ---- Speed / pitch: applied through ExoPlayer PlaybackParameters ----
    private var lastSpeed = 1.0f
    private var lastPitch = 1.0f

    /** Assigning the hook immediately applies the last known speed/pitch (service may start after the UI). */
    var onUpdatePlaybackSpeedPitch: ((speed: Float, pitch: Float) -> Unit)? = null
        set(value) {
            field = value
            value?.invoke(lastSpeed, lastPitch)
        }

    private var audioFx: AudioFxService? = null

    /** Called once when the player is built; whatever the UI already set is pushed right away. */
    fun attach(fx: AudioFxService) {
        audioFx = fx
        pushFx()
    }

    /** MyPlayer's own equalizer owns the EQ now; kept so the Mochy screens still compile. */
    fun setEq(enabled: Boolean, gains10: List<Float>, preampDb: Float) {
        eqGainsDb = FloatArray(10) { gains10.getOrElse(it) { 0f }.coerceIn(-12f, 12f) }
        eqPreampDb = preampDb
        eqEnabled = enabled
    }

    fun syncFromUiState(state: MochyUiStateHolder, loudnessPreset: MochyLoudnessPreset = MochyLoudnessPreset.PUNCHY) {
        eqAutoPreamp = state.autoPreamp
        setEq(state.eqEnabled, state.eqBands, state.preampDb)

        reverbEnabled = state.reverbEnabled
        reverbMix = state.reverbMix
        reverbRoomSize = state.reverbRoomSize
        reverbDamping = state.reverbDamping
        reverbWet = state.reverbWet

        loudnessEnabled = state.loudnessEnabled
        evenOutSongs = state.evenOutSongs
        replayGainPreampDb = state.replayGainPreampDb

        spatialEnabled = state.spatialAudioEnabled
        spatialIsSpeakersMode = (state.spatialMode == "Speakers")
        headShadowHz = state.headShadowHz
        crossfeedDb = state.crossfeedDb
        spatialAmount = state.spatialAmount

        monoEnabled = state.monoAudio
        dolbySurroundEnabled = state.dolbyAtmosSurround
        exponentialVolumeEnabled = state.exponentialVolume
        pushFx()

        val spd = state.playbackSpeed.coerceIn(0.25f, 3.0f)
        val effPitch = if (state.preservePitch) {
            2.0f.pow(state.pitchSemitones / 12.0f)
        } else {
            spd
        }.coerceIn(0.25f, 3.0f)
        if (spd != lastSpeed || effPitch != lastPitch) {
            lastSpeed = spd
            lastPitch = effPitch
            onUpdatePlaybackSpeedPitch?.invoke(spd, effPitch)
        }
    }

    /** Fine-tune sliders that live only inside the Audio FX screen are remembered here. */
    fun saveFineTune(context: Context) {
        context.getSharedPreferences("mochy_ui_prefs", Context.MODE_PRIVATE).edit()
            .putFloat("fx_reverb_dry", reverbDry)
            .putFloat("fx_reverb_width", reverbWidth)
            .putFloat("fx_reverb_predelay", reverbPreDelayMs)
            .putFloat("fx_reverb_lowcut", reverbLowCutHz)
            .putFloat("fx_loud_boost", loudnessBoostDb)
            .putFloat("fx_loud_comp", loudnessCompression)
            .putFloat("fx_speaker_angle", speakerAngleDeg)
            .apply()
    }

    fun loadFineTune(context: Context) {
        val p = context.getSharedPreferences("mochy_ui_prefs", Context.MODE_PRIVATE)
        reverbDry = p.getFloat("fx_reverb_dry", reverbDry)
        reverbWidth = p.getFloat("fx_reverb_width", reverbWidth)
        reverbPreDelayMs = p.getFloat("fx_reverb_predelay", reverbPreDelayMs)
        reverbLowCutHz = p.getFloat("fx_reverb_lowcut", reverbLowCutHz)
        loudnessBoostDb = p.getFloat("fx_loud_boost", loudnessBoostDb)
        loudnessCompression = p.getFloat("fx_loud_comp", loudnessCompression)
        speakerAngleDeg = p.getFloat("fx_speaker_angle", speakerAngleDeg)
        pushFx()
    }

    private fun pushFx() {
        val fx = audioFx ?: return
        fx.reverb = ReverbParams(
            reverbEnabled, reverbMix, reverbRoomSize, reverbDamping, reverbWet,
            reverbDry, reverbWidth, reverbPreDelayMs, reverbLowCutHz,
        )
        fx.loudness = LoudnessParams(loudnessEnabled, loudnessBoostDb, loudnessCompression)
        fx.spatial = SpatialParams(
            enabled = spatialEnabled,
            mode = if (spatialIsSpeakersMode) SpatialMode.Speakers else SpatialMode.Crossfeed,
            cutoffHz = headShadowHz,
            feedDb = kotlin.math.abs(crossfeedDb),
            speakerAngle = speakerAngleDeg,
            amount = spatialAmount,
            surround = dolbySurroundEnabled,
            mono = monoEnabled,
        )
        fx.normalize = evenOutSongs
        fx.replayGain = ReplayGainParams(ReplayGainMode.Track, replayGainPreampDb)
        val curve = VolumeCurve(enabled = exponentialVolumeEnabled)
        if (curve != fx.volumeCurve) fx.volumeCurve = curve
    }

}
