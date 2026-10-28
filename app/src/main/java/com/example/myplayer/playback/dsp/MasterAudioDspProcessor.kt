package com.example.myplayer.playback.dsp

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.example.myplayer.data.preferences.AudioPlaybackState
import com.example.myplayer.data.preferences.Eq15Preset
import com.example.myplayer.data.preferences.LoudnessMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Live health of the master DSP. Android release builds strip android.util.Log
 * (see proguard-rules.pro), so this flow is the only reliable way to see whether
 * the native engine is really running or the Kotlin fallback took over.
 */
data class DspStatus(
    val nativeLoaded: Boolean = false,
    val usingKotlinFallback: Boolean = true,
    val lastError: String? = null,
    val sampleRate: Int = 0,
    val eqEnabled: Boolean = false,
    val activeBands: Int = 0,
    val clarityActive: Boolean = false,
    /** Studio Master Clarity chain exists only in the native engine. */
    val clarityNeedsNative: Boolean = false
)

@OptIn(UnstableApi::class)
@Singleton
class MasterAudioDspProcessor @Inject constructor() : BaseAudioProcessor() {

    companion object {
        @Volatile private var libLoaded = false
        @Volatile private var libError: String? = null

        init {
            try {
                System.loadLibrary("lastwave_audio_engine")
                libLoaded = true
            } catch (t: Throwable) {
                libLoaded = false
                libError = "${t.javaClass.simpleName}: ${t.message}"
            }
        }

        val BAND_FREQUENCIES = floatArrayOf(
            25f, 40f, 63f, 100f, 160f, 250f, 400f, 630f,
            1000f, 1600f, 2500f, 4000f, 6300f, 10000f, 16000f
        )

        val BAND_LABELS = listOf(
            "25\nSUB", "40\nSUB", "63\nBASS", "100\nBASS", "160\nLOW-MID",
            "250\nLOW-MID", "400\nMID", "630\nMID", "1k\nUPPER-MID", "1.6k\nPRESENCE",
            "2.5k\nPRESENCE", "4k\nCLARITY", "6.3k\nTREBLE", "10k\nAIR", "16k\nSPARKLE"
        )

        private const val EQ_Q = 1.41421356
        private const val SOFT_KNEE = 0.841395141
        private const val CEILING = 0.944060876
    }

    private external fun nativeConfigure(
        sampleRate: Int,
        bitPerfect: Boolean,
        eqEnabled: Boolean,
        eqGains: FloatArray,
        studioClarity: Boolean,
        clarityPresetOrd: Int,
        loudnessGainDb: Float
    )

    private external fun nativeProcessPcmFloat(
        directBuffer: ByteBuffer,
        byteOffset: Int,
        frameCount: Int,
        channelCount: Int
    )

    private external fun nativeProcessPcm16(
        directBuffer: ByteBuffer,
        byteOffset: Int,
        frameCount: Int,
        channelCount: Int
    )

    @Volatile private var currentState = AudioPlaybackState()
    @Volatile private var currentTrackLufs = -14.0f
    @Volatile private var isSpatialStream = false
    @Volatile private var nativeReady = libLoaded
    @Volatile private var nativeError: String? = libError
    @Volatile private var lastSampleRate = 48000

    private val eqLock = Any()
    private val kotlinEq = KotlinEq()

    private val _status = MutableStateFlow(
        DspStatus(nativeLoaded = libLoaded, usingKotlinFallback = !libLoaded, lastError = libError)
    )
    val status: StateFlow<DspStatus> = _status.asStateFlow()

    fun updateState(newState: AudioPlaybackState, trackLufs: Float = currentTrackLufs, isSpatial: Boolean = isSpatialStream) {
        this.currentState = newState
        this.currentTrackLufs = trackLufs
        this.isSpatialStream = isSpatial
        // Always push, even before the first track: the sample rate falls back to the last known one.
        val sr = if (inputAudioFormat.sampleRate > 0) inputAudioFormat.sampleRate else lastSampleRate
        pushConfig(sr)
    }

    fun setTrackFormat(format: androidx.media3.common.Format?) {
        val mime = format?.sampleMimeType?.lowercase().orEmpty()
        val isSpatial = mime.contains("eac3") || mime.contains("ec-3") ||
            mime.contains("ac-3") || mime.contains("ac3") || mime.contains("ac4") ||
            mime.contains("mha1") || mime.contains("mhm1")
        val tags = com.example.myplayer.playback.LoudnessNormalizer.parseFromFormat(format)
        val trackGain = tags.trackGainDb ?: if (isSpatial) 6.0f else 0.0f
        val trackLufs = -14.0f - trackGain
        this.currentTrackLufs = trackLufs
        this.isSpatialStream = isSpatial
        val sr = if (inputAudioFormat.sampleRate > 0) inputAudioFormat.sampleRate else lastSampleRate
        pushConfig(sr)
    }

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        // Support BOTH Float32 and Int16 PCM, Mono and Stereo (1..2 channels)
        val isSupportedEncoding = inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT ||
                inputAudioFormat.encoding == C.ENCODING_PCM_16BIT
        if (!isSupportedEncoding || inputAudioFormat.channelCount !in 1..2) {
            return AudioFormat.NOT_SET
        }
        pushConfig(inputAudioFormat.sampleRate)
        return inputAudioFormat
    }

    private fun pushConfig(sampleRate: Int) {
        val sr = if (sampleRate > 0) sampleRate else 48000
        lastSampleRate = sr
        val st = currentState

        val gains = FloatArray(15) { i -> st.eqBandGains.getOrElse(i) { 0f } }

        // Studio Master Clarity is active when explicitly enabled OR when Studio Master preset is active
        val isStudioPreset = st.eqPreset == Eq15Preset.STUDIO_MASTER
        val effectiveClarity = (st.studioMasterClarity || isStudioPreset) &&
                !(st.claritySpatialBypass && isSpatialStream)

        val loudnessDb = when (st.loudnessMode) {
            LoudnessMode.OFF -> 0f
            LoudnessMode.TRACK -> (-14.0f - currentTrackLufs).coerceIn(-6f, 6f)
            LoudnessMode.ALBUM -> (-14.0f - (currentTrackLufs - 1.0f)).coerceIn(-6f, 5f)
        }
        val bitPerfect = st.bitPerfectMode || st.usbExclusiveOutput

        // 1) Kotlin EQ is ALWAYS configured, so failover to it is instant and glitch-free.
        //    Same +1 dB pre-limiter boost as the native engine when EQ is on.
        val preampDb = loudnessDb + if (st.eqEnabled) 1.0f else 0.0f
        synchronized(eqLock) { kotlinEq.configure(sr, gains, st.eqEnabled, preampDb) }

        // 2) Native engine (preferred: also carries the Studio Clarity chain).
        if (nativeReady) {
            try {
                nativeConfigure(
                    sampleRate = sr,
                    bitPerfect = bitPerfect,
                    eqEnabled = st.eqEnabled,
                    eqGains = gains,
                    studioClarity = effectiveClarity,
                    clarityPresetOrd = st.clarityOutputPreset.ordinal,
                    loudnessGainDb = loudnessDb
                )
            } catch (t: Throwable) {
                nativeReady = false
                nativeError = "${t.javaClass.simpleName}: ${t.message}"
            }
        }

        _status.value = DspStatus(
            nativeLoaded = libLoaded,
            usingKotlinFallback = !nativeReady,
            lastError = nativeError,
            sampleRate = sr,
            eqEnabled = st.eqEnabled,
            activeBands = if (st.eqEnabled) gains.count { abs(it) >= 0.005f } else 0,
            clarityActive = effectiveClarity,
            clarityNeedsNative = effectiveClarity && !nativeReady
        )
    }

    /** Seek / track change: drop reverb tails and filter memory of the Audio Lab chain. */
    override fun onFlush() {
        com.example.myplayer.ui.mochy.MochyAudioFxDspBridge.reset()
    }

    override fun onReset() {
        com.example.myplayer.ui.mochy.MochyAudioFxDspBridge.reset()
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val outputBuffer = replaceOutputBuffer(remaining).order(ByteOrder.nativeOrder())
        val st = currentState

        // Bit-Perfect Mode or USB Exclusive: 100% direct pass-through
        if (st.bitPerfectMode || st.usbExclusiveOutput) {
            outputBuffer.put(inputBuffer)
            outputBuffer.flip()
            return
        }

        val positionBefore = outputBuffer.position()
        outputBuffer.put(inputBuffer)

        val channelCount = inputAudioFormat.channelCount
        val frameCount = remaining / inputAudioFormat.bytesPerFrame
        val isFloat = inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT

        var processed = false
        if (nativeReady) {
            try {
                if (isFloat) {
                    nativeProcessPcmFloat(outputBuffer, positionBefore, frameCount, channelCount)
                } else {
                    nativeProcessPcm16(outputBuffer, positionBefore, frameCount, channelCount)
                }
                processed = true
            } catch (t: Throwable) {
                // JNI symbol missing / library broken: switch to Kotlin permanently and say so.
                nativeReady = false
                nativeError = "${t.javaClass.simpleName}: ${t.message}"
                _status.value = _status.value.copy(
                    usingKotlinFallback = true,
                    lastError = nativeError,
                    clarityNeedsNative = _status.value.clarityActive
                )
            }
        }
        if (!processed) {
            synchronized(eqLock) {
                kotlinEq.process(outputBuffer, positionBefore, frameCount, channelCount, isFloat)
            }
        }

        com.example.myplayer.ui.mochy.MochyAudioFxDspBridge.processBuffer(
            outputBuffer,
            positionBefore,
            frameCount,
            channelCount,
            isFloat,
            inputAudioFormat.sampleRate
        )

        outputBuffer.position(positionBefore + remaining)
        outputBuffer.flip()
    }

    /** 15-band peaking EQ (Q = sqrt(2)) + preamp + soft limiter. Same maths as the native engine. */
    private class KotlinEq {
        private val n = 15
        private val b0 = DoubleArray(n) { 1.0 }
        private val b1 = DoubleArray(n)
        private val b2 = DoubleArray(n)
        private val a1 = DoubleArray(n)
        private val a2 = DoubleArray(n)
        private val z1 = Array(2) { DoubleArray(n) }
        private val z2 = Array(2) { DoubleArray(n) }
        private var activeIdx = IntArray(0)
        private var preamp = 1.0
        private var sampleRate = 48000.0

        fun configure(sr: Int, gainsDb: FloatArray, eqEnabled: Boolean, preampDb: Float) {
            if (sr.toDouble() != sampleRate) {
                for (c in 0..1) { z1[c].fill(0.0); z2[c].fill(0.0) }
            }
            sampleRate = sr.toDouble()
            preamp = 10.0.pow(preampDb / 20.0)

            val previouslyActive = BooleanArray(n)
            for (i in activeIdx) previouslyActive[i] = true

            val active = ArrayList<Int>(n)
            for (i in 0 until n) {
                val g = if (eqEnabled) gainsDb[i].toDouble() else 0.0
                val f = BAND_FREQUENCIES[i].toDouble()
                if (abs(g) < 0.005 || f >= sampleRate * 0.45) continue

                val omega = 2.0 * PI * f / sampleRate
                val cosw = cos(omega)
                val alpha = sin(omega) / (2.0 * EQ_Q)
                val amp = 10.0.pow(g / 40.0)
                val a0 = 1.0 + alpha / amp
                b0[i] = (1.0 + alpha * amp) / a0
                b1[i] = (-2.0 * cosw) / a0
                b2[i] = (1.0 - alpha * amp) / a0
                a1[i] = (-2.0 * cosw) / a0
                a2[i] = (1.0 - alpha / amp) / a0
                if (!previouslyActive[i]) {
                    z1[0][i] = 0.0; z2[0][i] = 0.0
                    z1[1][i] = 0.0; z2[1][i] = 0.0
                }
                active.add(i)
            }
            activeIdx = active.toIntArray()
        }

        fun process(buf: ByteBuffer, byteOffset: Int, frames: Int, channels: Int, isFloat: Boolean) {
            val idxs = activeIdx
            if (idxs.isEmpty() && preamp == 1.0) return

            val bytesPer = if (isFloat) 4 else 2
            val ch = if (channels >= 2) 2 else 1
            var p = byteOffset
            for (f in 0 until frames) {
                for (c in 0 until ch) {
                    val at = p + c * bytesPer
                    var x = if (isFloat) buf.getFloat(at).toDouble() else buf.getShort(at) / 32768.0
                    x *= preamp
                    val zc1 = z1[c]
                    val zc2 = z2[c]
                    for (k in idxs.indices) {
                        val b = idxs[k]
                        val out = b0[b] * x + zc1[b]
                        var n1 = b1[b] * x - a1[b] * out + zc2[b]
                        var n2 = b2[b] * x - a2[b] * out
                        if (abs(n1) < 1.0e-18) n1 = 0.0
                        if (abs(n2) < 1.0e-18) n2 = 0.0
                        zc1[b] = n1
                        zc2[b] = n2
                        x = out
                    }
                    x = softLimit(x)
                    if (isFloat) {
                        buf.putFloat(at, x.toFloat())
                    } else {
                        buf.putShort(at, (x * 32767.0).roundToInt().coerceIn(-32768, 32767).toShort())
                    }
                }
                p += channels * bytesPer
            }
        }

        private fun softLimit(x: Double): Double {
            val a = abs(x)
            if (a <= SOFT_KNEE) return x
            val range = CEILING - SOFT_KNEE
            val s = SOFT_KNEE + range * tanh((a - SOFT_KNEE) / range)
            return if (x >= 0.0) s else -s
        }
    }
}
