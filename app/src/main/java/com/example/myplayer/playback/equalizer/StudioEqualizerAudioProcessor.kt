package com.example.myplayer.playback.equalizer

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

data class EqualizerState(
    val enabled: Boolean = true,
    val presetName: String = "Normal",
    val bassLevel: Float = 0f,        // -1.0f (-12dB) to +1.0f (+12dB)
    val trebleLevel: Float = 0f,      // -1.0f (-12dB) to +1.0f (+12dB)
    val volumeGain: Float = 1.0f,      // 0.0f (0%) to 2.0f (200%)
    val vocalLevel: Float = 1.0f,      // 0.0f (No Vocals / Karaoke) to 1.5f (Vocal Boost)
    val instrumentalLevel: Float = 1.0f // 0.0f (Vocals Only) to 1.5f (Instrumental Boost)
)

@UnstableApi
@Singleton
class StudioEqualizerAudioProcessor @Inject constructor() : BaseAudioProcessor() {

    private val _state = MutableStateFlow(EqualizerState())
    val state: StateFlow<EqualizerState> = _state.asStateFlow()

    @Volatile private var isEqEnabled = true
    @Volatile private var bassDb = 0.0
    @Volatile private var trebleDb = 0.0
    @Volatile private var preampGain = 1.0f
    @Volatile private var vocalGain = 1.0f
    @Volatile private var instrumentalGain = 1.0f
    @Volatile private var coeffsDirty = true

    // Biquad Low-Shelf (Bass @ 110Hz) coefficients & state per channel
    private var b0L = 1.0; private var b1L = 0.0; private var b2L = 0.0
    private var a1L = 0.0; private var a2L = 0.0
    private var lx1L = 0.0; private var lx2L = 0.0; private var ly1L = 0.0; private var ly2L = 0.0
    private var lx1R = 0.0; private var lx2R = 0.0; private var ly1R = 0.0; private var ly2R = 0.0

    // Biquad High-Shelf (Treble @ 4200Hz) coefficients & state per channel
    private var b0H = 1.0; private var b1H = 0.0; private var b2H = 0.0
    private var a1H = 0.0; private var a2H = 0.0
    private var hx1L = 0.0; private var hx2L = 0.0; private var hy1L = 0.0; private var hy2L = 0.0
    private var hx1R = 0.0; private var hx2R = 0.0; private var hy1R = 0.0; private var hy2R = 0.0

    // Vocal Bandpass (220Hz - 3800Hz) 1-pole crossover states on Mid channel
    private var lowPassState = 0.0
    private var highPassState = 0.0

    fun setEnabled(enabled: Boolean) {
        isEqEnabled = enabled
        _state.value = _state.value.copy(enabled = enabled)
    }

    fun setBass(level: Float) {
        val clamped = level.coerceIn(-1f, 1f)
        bassDb = clamped * 12.0
        coeffsDirty = true
        _state.value = _state.value.copy(bassLevel = clamped, presetName = "Custom")
    }

    fun setTreble(level: Float) {
        val clamped = level.coerceIn(-1f, 1f)
        trebleDb = clamped * 12.0
        coeffsDirty = true
        _state.value = _state.value.copy(trebleLevel = clamped, presetName = "Custom")
    }

    fun setVolumeGain(gain: Float) {
        val clamped = gain.coerceIn(0f, 2f)
        preampGain = clamped
        _state.value = _state.value.copy(volumeGain = clamped)
    }

    fun setVocalLevel(level: Float) {
        val clamped = level.coerceIn(0f, 1.5f)
        vocalGain = clamped
        _state.value = _state.value.copy(vocalLevel = clamped, presetName = "Custom")
    }

    fun setInstrumentalLevel(level: Float) {
        val clamped = level.coerceIn(0f, 1.5f)
        instrumentalGain = clamped
        _state.value = _state.value.copy(instrumentalLevel = clamped, presetName = "Custom")
    }

    fun applyPreset(name: String) {
        val next = when (name) {
            "Only Vocals" -> EqualizerState(
                enabled = true,
                presetName = name,
                bassLevel = -0.5f,
                trebleLevel = 0.1f,
                volumeGain = 1.15f,
                vocalLevel = 1.35f,
                instrumentalLevel = 0.0f
            )
            "Only Instrumental" -> EqualizerState(
                enabled = true,
                presetName = name,
                bassLevel = 0.25f,
                trebleLevel = 0.15f,
                volumeGain = 1.15f,
                vocalLevel = 0.0f,
                instrumentalLevel = 1.25f
            )
            "Bass Boost" -> EqualizerState(
                enabled = true,
                presetName = name,
                bassLevel = 0.85f,
                trebleLevel = 0.20f,
                volumeGain = 1.05f,
                vocalLevel = 1.0f,
                instrumentalLevel = 1.0f
            )
            "Lo-Fi Chill" -> EqualizerState(
                enabled = true,
                presetName = name,
                bassLevel = 0.35f,
                trebleLevel = -0.65f,
                volumeGain = 0.95f,
                vocalLevel = 0.90f,
                instrumentalLevel = 1.05f
            )
            else -> EqualizerState(presetName = "Normal")
        }

        isEqEnabled = next.enabled
        bassDb = next.bassLevel * 12.0
        trebleDb = next.trebleLevel * 12.0
        preampGain = next.volumeGain
        vocalGain = next.vocalLevel
        instrumentalGain = next.instrumentalLevel
        coeffsDirty = true
        _state.value = next
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        if (inputAudioFormat.channelCount != 2 && inputAudioFormat.channelCount != 1) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        coeffsDirty = true
        return inputAudioFormat
    }

    private fun updateBiquadCoefficients(sampleRate: Double) {
        // 1. Low-Shelf Filter at 110 Hz (Bass)
        val aLow = 10.0.pow(bassDb / 40.0)
        val w0Low = 2.0 * Math.PI * 110.0 / sampleRate
        val cosLow = cos(w0Low)
        val alphaLow = sin(w0Low) / 2.0 * sqrt(2.0)
        val twoSqrtALowAlpha = 2.0 * sqrt(aLow) * alphaLow

        val a0L = (aLow + 1.0) + (aLow - 1.0) * cosLow + twoSqrtALowAlpha
        b0L = (aLow * ((aLow + 1.0) - (aLow - 1.0) * cosLow + twoSqrtALowAlpha)) / a0L
        b1L = (2.0 * aLow * ((aLow - 1.0) - (aLow + 1.0) * cosLow)) / a0L
        b2L = (aLow * ((aLow + 1.0) - (aLow - 1.0) * cosLow - twoSqrtALowAlpha)) / a0L
        a1L = (-2.0 * ((aLow - 1.0) + (aLow + 1.0) * cosLow)) / a0L
        a2L = ((aLow + 1.0) + (aLow - 1.0) * cosLow - twoSqrtALowAlpha) / a0L

        // 2. High-Shelf Filter at 4200 Hz (Treble)
        val aHigh = 10.0.pow(trebleDb / 40.0)
        val w0High = 2.0 * Math.PI * 4200.0 / sampleRate
        val cosHigh = cos(w0High)
        val alphaHigh = sin(w0High) / 2.0 * sqrt(2.0)
        val twoSqrtAHighAlpha = 2.0 * sqrt(aHigh) * alphaHigh

        val a0H = (aHigh + 1.0) - (aHigh - 1.0) * cosHigh + twoSqrtAHighAlpha
        b0H = (aHigh * ((aHigh + 1.0) + (aHigh - 1.0) * cosHigh + twoSqrtAHighAlpha)) / a0H
        b1H = (-2.0 * aHigh * ((aHigh - 1.0) + (aHigh + 1.0) * cosHigh)) / a0H
        b2H = (aHigh * ((aHigh + 1.0) + (aHigh - 1.0) * cosHigh - twoSqrtAHighAlpha)) / a0H
        a1H = (2.0 * ((aHigh - 1.0) - (aHigh + 1.0) * cosHigh)) / a0H
        a2H = ((aHigh + 1.0) - (aHigh - 1.0) * cosHigh - twoSqrtAHighAlpha) / a0H

        coeffsDirty = false
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val sampleRate = inputAudioFormat.sampleRate.toDouble().coerceAtLeast(8000.0)
        if (coeffsDirty) {
            updateBiquadCoefficients(sampleRate)
        }

        val channels = inputAudioFormat.channelCount
        val outputBuffer = replaceOutputBuffer(remaining)
        val vGain = vocalGain.toDouble()
        val iGain = instrumentalGain.toDouble()
        val pGain = preampGain.toDouble()

        // Pass-through optimization when all parameters are nominal/default
        if (!isEqEnabled || (vGain == 1.0 && iGain == 1.0 && bassDb == 0.0 && trebleDb == 0.0 && pGain == 1.0)) {
            outputBuffer.put(inputBuffer)
            outputBuffer.flip()
            return
        }

        // Crossover alphas for isolating center vocal band (200 Hz to 3800 Hz) from center sub-bass
        val lpAlpha = (2.0 * Math.PI * 200.0 / (sampleRate + 2.0 * Math.PI * 200.0)).coerceIn(0.005, 0.5)
        val hpAlpha = (2.0 * Math.PI * 3800.0 / (sampleRate + 2.0 * Math.PI * 3800.0)).coerceIn(0.05, 0.85)

        if (channels == 2) {
            while (inputBuffer.remaining() >= 4) {
                var l = inputBuffer.short / 32768.0
                var r = inputBuffer.short / 32768.0

                // 1. Mid/Side Vocal & Instrumental Separation
                if (vGain != 1.0 || iGain != 1.0) {
                    val mid = (l + r) * 0.5
                    val side = (l - r) * 0.5

                    // Split center Mid channel into Sub-Bass (<200Hz), Vocal Band (200-3800Hz), and High Air (>3800Hz)
                    lowPassState += lpAlpha * (mid - lowPassState)
                    val subBassMid = lowPassState
                    val midAboveBass = mid - subBassMid

                    highPassState += hpAlpha * (midAboveBass - highPassState)
                    val vocalMid = highPassState
                    val highInstMid = midAboveBass - vocalMid

                    // Reconstruct Mid and Side using independent Vocal & Instrumental gains
                    val newMid = (vocalMid * vGain) + ((subBassMid + highInstMid) * iGain)
                    val newSide = side * iGain

                    l = newMid + newSide
                    r = newMid - newSide
                }

                // 2. Bass Biquad Low-Shelf Filter
                val lBass = b0L * l + b1L * lx1L + b2L * lx2L - a1L * ly1L - a2L * ly2L
                lx2L = lx1L; lx1L = l; ly2L = ly1L; ly1L = lBass

                val rBass = b0L * r + b1L * lx1R + b2L * lx2R - a1L * ly1R - a2L * ly2R
                lx2R = lx1R; lx1R = r; ly2R = ly1R; ly1R = rBass

                // 3. Treble Biquad High-Shelf Filter
                val lOut = b0H * lBass + b1H * hx1L + b2H * hx2L - a1H * hy1L - a2H * hy2L
                hx2L = hx1L; hx1L = lBass; hy2L = hy1L; hy1L = lOut

                val rOut = b0H * rBass + b1H * hx1R + b2H * hx2R - a1H * hy1R - a2H * hy2R
                hx2R = hx1R; hx1R = rBass; hy2R = hy1R; hy1R = rOut

                // 4. Preamp Volume Gain + Soft-Clip Limiter (prevents harsh distortion)
                val finalL = softClip(lOut * pGain)
                val finalR = softClip(rOut * pGain)

                outputBuffer.putShort((finalL * 32767.0).toInt().coerceIn(-32768, 32767).toShort())
                outputBuffer.putShort((finalR * 32767.0).toInt().coerceIn(-32768, 32767).toShort())
            }
        } else {
            while (inputBuffer.remaining() >= 2) {
                val m = (inputBuffer.short / 32768.0) * pGain
                val out = (softClip(m) * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
                outputBuffer.putShort(out)
            }
        }
        inputBuffer.position(inputBuffer.limit())
        outputBuffer.flip()
    }

    private fun softClip(x: Double): Double {
        return if (x in -0.85..0.85) x else tanh(x)
    }

    override fun onFlush() {
        lx1L = 0.0; lx2L = 0.0; ly1L = 0.0; ly2L = 0.0
        lx1R = 0.0; lx2R = 0.0; ly1R = 0.0; ly2R = 0.0
        hx1L = 0.0; hx2L = 0.0; hy1L = 0.0; hy2L = 0.0
        hx1R = 0.0; hx2R = 0.0; hy1R = 0.0; hy2R = 0.0
        lowPassState = 0.0; highPassState = 0.0
    }

    override fun onReset() {
        onFlush()
    }
}
