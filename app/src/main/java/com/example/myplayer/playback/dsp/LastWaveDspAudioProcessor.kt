package com.example.myplayer.playback.dsp

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.example.myplayer.data.preferences.ClarityPreset
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@OptIn(UnstableApi::class)
@Singleton
class LastWaveDspAudioProcessor @Inject constructor() : BaseAudioProcessor() {

    @Volatile private var clarityPreset: ClarityPreset = ClarityPreset.OFF
    @Volatile private var trueDolbyEnabled: Boolean = false
    @Volatile private var spatialWidth: Float = 0.45f
    @Volatile var isDualBudActive: Boolean = false // DualBud safety flag!

    // Biquad High-Shelf Filter state (Left & Right channels)
    private var x1L = 0f; private var x2L = 0f; private var y1L = 0f; private var y2L = 0f
    private var x1R = 0f; private var x2R = 0f; private var y1R = 0f; private var y2R = 0f
    private var b0 = 1f; private var b1 = 0f; private var b2 = 0f; private var a1 = 0f; private var a2 = 0f

    fun updateDspConfig(preset: ClarityPreset, dolby: Boolean, width: Float) {
        this.clarityPreset = preset
        this.trueDolbyEnabled = dolby
        this.spatialWidth = width.coerceIn(0f, 1f)
        if (inputAudioFormat.sampleRate > 0) {
            calculateHighShelfCoeffs(inputAudioFormat.sampleRate, 3800f, preset.highShelfGainDb)
        }
    }

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount != 2) {
            return AudioFormat.NOT_SET
        }
        calculateHighShelfCoeffs(inputAudioFormat.sampleRate, 3800f, clarityPreset.highShelfGainDb)
        return inputAudioFormat
    }

    private fun calculateHighShelfCoeffs(sampleRate: Int, freq: Float, gainDb: Float) {
        if (gainDb == 0f) {
            b0 = 1f; b1 = 0f; b2 = 0f; a1 = 0f; a2 = 0f
            return
        }
        val a = 10.0.pow(gainDb / 40.0).toFloat()
        val w0 = (2.0 * Math.PI * freq / sampleRate).toFloat()
        val cosW0 = cos(w0)
        val alpha = sin(w0) / 2f * sqrt(2f)

        val a0 = (a + 1f) - (a - 1f) * cosW0 + 2f * sqrt(a) * alpha
        b0 = (a * ((a + 1f) + (a - 1f) * cosW0 + 2f * sqrt(a) * alpha)) / a0
        b1 = (-2f * a * ((a - 1f) + (a + 1f) * cosW0)) / a0
        b2 = (a * ((a + 1f) + (a - 1f) * cosW0 - 2f * sqrt(a) * alpha)) / a0
        a1 = (2f * ((a - 1f) - (a + 1f) * cosW0)) / a0
        a2 = ((a + 1f) - (a - 1f) * cosW0 - 2f * sqrt(a) * alpha) / a0
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val outputBuffer = replaceOutputBuffer(remaining)
        val bypassDsp = (clarityPreset == ClarityPreset.OFF && !trueDolbyEnabled)

        if (bypassDsp) {
            outputBuffer.put(inputBuffer)
            outputBuffer.flip()
            return
        }

        while (inputBuffer.remaining() >= 4) {
            var left = inputBuffer.short / 32768f
            var right = inputBuffer.short / 32768f

            // 1. Apply Clarity High-Shelf Biquad Filter
            if (clarityPreset != ClarityPreset.OFF) {
                val outL = b0 * left + b1 * x1L + b2 * x2L - a1 * y1L - a2 * y2L
                x2L = x1L; x1L = left; y2L = y1L; y1L = outL
                left = outL

                val outR = b0 * right + b1 * x1R + b2 * x2R - a1 * y1R - a2 * y2R
                x2R = x1R; x1R = right; y2R = y1R; y1R = outR
                right = outR
            }

            // 2. Apply True-Dolby Mid-Side Spatial Widening (Bypassed if DualBud Mode is ON)
            if (trueDolbyEnabled && !isDualBudActive) {
                val mid = (left + right) * 0.5f
                val side = (left - right) * 0.5f * (1f + spatialWidth * 0.85f)
                left = mid + side
                right = mid - side
            }

            // Soft-clipping limiter to prevent digital distortion
            val clampedL = (left.coerceIn(-0.99f, 0.99f) * 32767f).toInt().toShort()
            val clampedR = (right.coerceIn(-0.99f, 0.99f) * 32767f).toInt().toShort()

            outputBuffer.putShort(clampedL)
            outputBuffer.putShort(clampedR)
        }
        outputBuffer.flip()
    }
}
