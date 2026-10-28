package com.example.myplayer.ui.mochy

import android.content.Context
import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Audio Lab DSP chain, called by MasterAudioDspProcessor on every PCM buffer:
 *
 *   10-band EQ + preamp  ->  ReplayGain / loudness compressor  ->  spatial (crossfeed | speakers)
 *   ->  Dolby-style surround  ->  Freeverb reverb  ->  mono downmix  ->  soft limiter
 *
 * What changed compared with the first version:
 *  - the 10-band Mochy equalizer is now REALLY applied (it was a no-op callback before);
 *  - no per-sample allocations (the old code returned a Pair per sample) and every coefficient
 *    (exp / pow / trig) is computed once per buffer instead of once per sample;
 *  - ReplayGain/"even out" gain only applies while Loudness is switched on (it used to add
 *    +1.35 dB to everything by default);
 *  - the "exponential volume" waveform pow() was removed: shaping samples is distortion, not a
 *    volume curve (needs a hook on the volume control, see notes);
 *  - state is cleared on seek/flush (no stale reverb tail), reverb tail is cleared when switched off;
 *  - a soft limiter protects against clipping when EQ / loudness / reverb add gain.
 */
object MochyAudioFxDspBridge {
    @Volatile var sampleRate: Int = 48000

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

    // ---- 2. Loudness & ReplayGain ----
    @Volatile var loudnessEnabled: Boolean = false
    @Volatile var loudnessBoostDb: Float = 4.0f
    @Volatile var loudnessCompression: Float = 0.30f
    @Volatile var evenOutSongs: Boolean = true
    @Volatile var replayGainPreampDb: Float = 3.0f

    // ---- 3. Spatial ----
    @Volatile var spatialEnabled: Boolean = false
    @Volatile var spatialIsSpeakersMode: Boolean = false
    @Volatile var headShadowHz: Float = 700f
    @Volatile var crossfeedDb: Float = -4.5f
    @Volatile var speakerAngleDeg: Float = 30f
    @Volatile var spatialAmount: Float = 1.0f

    // ---- 4. Mono & surround ----
    @Volatile var monoEnabled: Boolean = false
    @Volatile var dolbySurroundEnabled: Boolean = false

    // ---- 5. Kept for source compatibility. Not applied to samples (see class doc). ----
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

    // =====================================================================================
    //  Internal DSP state (touched by the audio thread only, except reset() which is also
    //  called from the audio thread through the processor's onFlush)
    // =====================================================================================

    private const val EQ_BANDS = 10
    private val EQ_FREQS = floatArrayOf(31.25f, 62.5f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)
    private const val EQ_Q = 1.41421356

    private val eqB0 = DoubleArray(EQ_BANDS) { 1.0 }
    private val eqB1 = DoubleArray(EQ_BANDS)
    private val eqB2 = DoubleArray(EQ_BANDS)
    private val eqA1 = DoubleArray(EQ_BANDS)
    private val eqA2 = DoubleArray(EQ_BANDS)
    private val eqZ1 = Array(2) { DoubleArray(EQ_BANDS) }
    private val eqZ2 = Array(2) { DoubleArray(EQ_BANDS) }
    private var eqActive = IntArray(0)
    private var eqCachedGains = FloatArray(EQ_BANDS)
    private var eqCachedSr = 0
    private var eqCachedValid = false

    private const val MAX_PREDELAY_SAMPLES = 9600
    private val preDelayBufL = FloatArray(MAX_PREDELAY_SAMPLES)
    private val preDelayBufR = FloatArray(MAX_PREDELAY_SAMPLES)
    private var preDelayWriteIdx = 0

    private val combLengthsL = intArrayOf(1116, 1188, 1277, 1356)
    private val combLengthsR = intArrayOf(1116 + 23, 1188 + 23, 1277 + 23, 1356 + 23)
    private val combBufL = Array(4) { FloatArray(combLengthsL[it]) }
    private val combBufR = Array(4) { FloatArray(combLengthsR[it]) }
    private val combIdxL = IntArray(4)
    private val combIdxR = IntArray(4)
    private val combStoreL = FloatArray(4)
    private val combStoreR = FloatArray(4)

    private val allpassLengthsL = intArrayOf(556, 441)
    private val allpassLengthsR = intArrayOf(556 + 23, 441 + 23)
    private val allpassBufL = Array(2) { FloatArray(allpassLengthsL[it]) }
    private val allpassBufR = Array(2) { FloatArray(allpassLengthsR[it]) }
    private val allpassIdxL = IntArray(2)
    private val allpassIdxR = IntArray(2)

    private var reverbLowCutStateL = 0f
    private var reverbLowCutStateR = 0f
    private var reverbWasOn = false

    private var compEnvelope = 0.001f
    private var compGain = 1f

    private var bs2bLoL = 0f
    private var bs2bLoR = 0f

    private val speakerDelayL = FloatArray(128)
    private val speakerDelayR = FloatArray(128)
    private var speakerDelayIdx = 0
    private var speakerShadowL = 0f
    private var speakerShadowR = 0f

    private val surroundDelayBuf = FloatArray(1024)
    private var surroundIdx = 0

    // =====================================================================================
    //  Public API
    // =====================================================================================

    /** Immediate EQ update (the equalizer screen calls this; the global sync also covers it). */
    fun setEq(enabled: Boolean, gains10: List<Float>, preampDb: Float) {
        eqGainsDb = FloatArray(EQ_BANDS) { gains10.getOrElse(it) { 0f }.coerceIn(-12f, 12f) }
        eqPreampDb = preampDb
        eqEnabled = enabled
    }

    fun syncFromUiState(state: MochyUiStateHolder, loudnessPreset: MochyLoudnessPreset = MochyLoudnessPreset.PUNCHY) {
        setEq(state.eqEnabled, state.eqBands, state.preampDb)
        eqAutoPreamp = state.autoPreamp

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

    fun isActive(): Boolean = eqEnabled || reverbEnabled || loudnessEnabled || spatialEnabled ||
        monoEnabled || dolbySurroundEnabled

    /** Clears every delay line / filter memory (seek, track change, sample-rate change). */
    fun reset() {
        for (c in 0..1) { eqZ1[c].fill(0.0); eqZ2[c].fill(0.0) }
        preDelayBufL.fill(0f); preDelayBufR.fill(0f); preDelayWriteIdx = 0
        for (i in 0 until 4) {
            combBufL[i].fill(0f); combBufR[i].fill(0f)
            combIdxL[i] = 0; combIdxR[i] = 0
            combStoreL[i] = 0f; combStoreR[i] = 0f
        }
        for (i in 0 until 2) {
            allpassBufL[i].fill(0f); allpassBufR[i].fill(0f)
            allpassIdxL[i] = 0; allpassIdxR[i] = 0
        }
        reverbLowCutStateL = 0f; reverbLowCutStateR = 0f
        compEnvelope = 0.001f; compGain = 1f
        bs2bLoL = 0f; bs2bLoR = 0f
        speakerDelayL.fill(0f); speakerDelayR.fill(0f); speakerDelayIdx = 0
        speakerShadowL = 0f; speakerShadowR = 0f
        surroundDelayBuf.fill(0f); surroundIdx = 0
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
    }

    // =====================================================================================
    //  Audio thread
    // =====================================================================================

    fun processBuffer(buf: ByteBuffer, byteOffset: Int, frames: Int, channels: Int, isFloat: Boolean, sr: Int) {
        if (!isActive() || channels !in 1..2 || frames <= 0) return
        if (sr > 8000 && sr != sampleRate) {
            sampleRate = sr
            reset()
            eqCachedValid = false
        }
        val fs = sampleRate.toFloat()

        // ---- parameter snapshot + per-buffer coefficients ----
        val doEq = eqEnabled
        val eqPreLin = if (doEq) prepareEq(fs) else 1f

        if (channels == 1) {
            processMono(buf, byteOffset, frames, isFloat, doEq, eqPreLin)
            return
        }

        val doLoud = loudnessEnabled
        val rgGain = if (doLoud && evenOutSongs && abs(replayGainPreampDb) > 0.05f) {
            10.0f.pow((replayGainPreampDb * 0.45f) / 20.0f)
        } else 1f
        val comp = loudnessCompression.coerceIn(0f, 1f)
        val thresholdDb = -10f - 24f * comp
        val slope = 1f - (1f / (1f + 3f * comp))
        val makeupDb = if (comp > 0f) -thresholdDb * slope * 0.5f else 0f
        val boostDb = loudnessBoostDb

        val doSpatial = spatialEnabled && spatialAmount > 0.01f
        val amt = spatialAmount.coerceIn(0f, 1f)
        val speakers = spatialIsSpeakersMode
        val fc = headShadowHz.coerceIn(300f, 2000f)
        val xLo = exp((-2.0 * PI * fc / fs).toFloat())
        val feedGain = 10.0f.pow(-abs(crossfeedDb).coerceIn(1f, 15f) / 20.0f) * 0.45f
        val bs2bNorm = 1f / (1f + feedGain * 0.6f)
        val angleFactor = (speakerAngleDeg / 60f).coerceIn(0.15f, 1.0f)
        val itdSamples = (angleFactor * (fs * 0.00045f)).toInt().coerceIn(2, 64)
        val crossMix = 0.32f * angleFactor

        val doDolby = dolbySurroundEnabled

        val doReverb = reverbEnabled && reverbMix > 0.01f
        if (!doReverb && reverbWasOn) clearReverb()
        reverbWasOn = doReverb
        val preDelaySamples = ((reverbPreDelayMs / 1000f) * fs).toInt().coerceIn(0, MAX_PREDELAY_SAMPLES - 1)
        val lcCoeff = exp((-2.0 * PI * reverbLowCutHz.coerceIn(20f, 1000f) / fs).toFloat())
        val feedback = 0.70f + reverbRoomSize.coerceIn(0f, 1f) * 0.27f
        val damp1 = reverbDamping.coerceIn(0.02f, 0.95f) * 0.45f
        val damp2 = 1f - damp1
        val width = reverbWidth.coerceIn(0f, 1f)
        val wet1 = reverbWet * (width * 0.5f + 0.5f) * 5.0f   // was 2.4f: wet was ~-20 dB under the dry signal = inaudible
        val wet2 = reverbWet * ((1f - width) * 0.5f) * 5.0f
        val dryLevel = reverbDry.coerceIn(0f, 1f)
        val mixLevel = reverbMix.coerceIn(0f, 1f)

        val doMono = monoEnabled

        val bytesPer = if (isFloat) 4 else 2
        val stride = 2 * bytesPer
        var p = byteOffset

        for (f in 0 until frames) {
            var l: Float
            var r: Float
            if (isFloat) {
                l = buf.getFloat(p)
                r = buf.getFloat(p + 4)
            } else {
                l = buf.getShort(p) / 32768.0f
                r = buf.getShort(p + 2) / 32768.0f
            }

            // 0. EQ + preamp
            if (doEq) {
                l = eqSample(0, l) * eqPreLin
                r = eqSample(1, r) * eqPreLin
            }

            // 1. ReplayGain preamp + loudness compressor
            if (doLoud) {
                l *= rgGain
                r *= rgGain
                val peak = max(abs(l), abs(r))
                compEnvelope += (if (peak > compEnvelope) 0.015f else 0.0008f) * (peak - compEnvelope)
                if ((f and 15) == 0) {
                    val levelDb = 20f * log10(max(compEnvelope, 1e-6f))
                    val reductionDb = if (levelDb > thresholdDb) (thresholdDb - levelDb) * slope else 0f
                    val totalDb = (boostDb + makeupDb + reductionDb).coerceIn(-12f, 18f)
                    val target = 10.0f.pow(totalDb / 20.0f)
                    compGain += 0.25f * (target - compGain)
                }
                l *= compGain
                r *= compGain
            }

            // 2. Spatial
            if (doSpatial) {
                if (!speakers) {
                    bs2bLoL = (1f - xLo) * l + xLo * bs2bLoL
                    bs2bLoR = (1f - xLo) * r + xLo * bs2bLoR
                    val crossL = l + feedGain * bs2bLoR
                    val crossR = r + feedGain * bs2bLoL
                    l += (crossL * bs2bNorm - l) * amt
                    r += (crossR * bs2bNorm - r) * amt
                } else {
                    speakerDelayL[speakerDelayIdx] = l
                    speakerDelayR[speakerDelayIdx] = r
                    val di = (speakerDelayIdx - itdSamples + 128) and 127
                    val farFromLeft = speakerDelayL[di]
                    val farFromRight = speakerDelayR[di]
                    speakerDelayIdx = (speakerDelayIdx + 1) and 127
                    speakerShadowL += 0.28f * (farFromRight - speakerShadowL)
                    speakerShadowR += 0.28f * (farFromLeft - speakerShadowR)
                    val spkL = l * 0.85f + speakerShadowL * crossMix
                    val spkR = r * 0.85f + speakerShadowR * crossMix
                    l += (spkL - l) * amt
                    r += (spkR - r) * amt
                }
            }

            // 3. Surround (mid/side widening with a short Haas delay on the side signal)
            if (doDolby) {
                val mid = (l + r) * 0.5f
                val side = (l - r) * 0.5f
                surroundDelayBuf[surroundIdx] = side
                val delayedSide = surroundDelayBuf[(surroundIdx - 384 + 1024) and 1023]
                surroundIdx = (surroundIdx + 1) and 1023
                val wideSide = side * 1.42f + delayedSide * 0.35f
                l = mid * 1.05f + wideSide
                r = mid * 1.05f - wideSide
            }

            // 4. Freeverb stereo reverb
            if (doReverb) {
                preDelayBufL[preDelayWriteIdx] = l
                preDelayBufR[preDelayWriteIdx] = r
                val ri = (preDelayWriteIdx - preDelaySamples + MAX_PREDELAY_SAMPLES) % MAX_PREDELAY_SAMPLES
                val dL = preDelayBufL[ri]
                val dR = preDelayBufR[ri]
                preDelayWriteIdx = (preDelayWriteIdx + 1) % MAX_PREDELAY_SAMPLES

                reverbLowCutStateL = (1f - lcCoeff) * dL + lcCoeff * reverbLowCutStateL
                reverbLowCutStateR = (1f - lcCoeff) * dR + lcCoeff * reverbLowCutStateR
                val inputMono = ((dL - reverbLowCutStateL) + (dR - reverbLowCutStateR)) * 0.015f

                var outCombL = 0f
                var outCombR = 0f
                for (c in 0 until 4) {
                    val iL = combIdxL[c]
                    val oL = combBufL[c][iL]
                    combStoreL[c] = oL * damp2 + combStoreL[c] * damp1
                    combBufL[c][iL] = inputMono + combStoreL[c] * feedback
                    combIdxL[c] = if (iL + 1 >= combLengthsL[c]) 0 else iL + 1
                    outCombL += oL

                    val iR = combIdxR[c]
                    val oR = combBufR[c][iR]
                    combStoreR[c] = oR * damp2 + combStoreR[c] * damp1
                    combBufR[c][iR] = inputMono + combStoreR[c] * feedback
                    combIdxR[c] = if (iR + 1 >= combLengthsR[c]) 0 else iR + 1
                    outCombR += oR
                }
                for (a in 0 until 2) {
                    val iL = allpassIdxL[a]
                    val oL = allpassBufL[a][iL]
                    val inL = outCombL
                    outCombL = -inL + oL
                    allpassBufL[a][iL] = inL + oL * 0.5f
                    allpassIdxL[a] = if (iL + 1 >= allpassLengthsL[a]) 0 else iL + 1

                    val iR = allpassIdxR[a]
                    val oR = allpassBufR[a][iR]
                    val inR = outCombR
                    outCombR = -inR + oR
                    allpassBufR[a][iR] = inR + oR * 0.5f
                    allpassIdxR[a] = if (iR + 1 >= allpassLengthsR[a]) 0 else iR + 1
                }
                val tankL = outCombL * wet1 + outCombR * wet2
                val tankR = outCombR * wet1 + outCombL * wet2
                l = l * dryLevel + tankL * mixLevel
                r = r * dryLevel + tankR * mixLevel
            }

            // 5. Mono downmix
            if (doMono) {
                val m = (l + r) * 0.5f
                l = m
                r = m
            }

            // 6. Soft limiter + write back
            l = softLimit(l)
            r = softLimit(r)
            if (isFloat) {
                buf.putFloat(p, l)
                buf.putFloat(p + 4, r)
            } else {
                buf.putShort(p, (l * 32767.0f).roundToInt().coerceIn(-32768, 32767).toShort())
                buf.putShort(p + 2, (r * 32767.0f).roundToInt().coerceIn(-32768, 32767).toShort())
            }
            p += stride
        }
    }

    // ---- mono streams: only the equalizer applies ----
    private fun processMono(buf: ByteBuffer, byteOffset: Int, frames: Int, isFloat: Boolean, doEq: Boolean, eqPreLin: Float) {
        if (!doEq) return
        val bytesPer = if (isFloat) 4 else 2
        var p = byteOffset
        for (f in 0 until frames) {
            var x = if (isFloat) buf.getFloat(p) else buf.getShort(p) / 32768.0f
            x = softLimit(eqSample(0, x) * eqPreLin)
            if (isFloat) buf.putFloat(p, x)
            else buf.putShort(p, (x * 32767.0f).roundToInt().coerceIn(-32768, 32767).toShort())
            p += bytesPer
        }
    }

    // ---- equalizer helpers ----
    /** Rebuilds the biquads when gains / sample rate changed. Returns the linear preamp gain. */
    private fun prepareEq(fs: Float): Float {
        val gains = eqGainsDb
        val changed = !eqCachedValid || eqCachedSr != sampleRate || !gains.contentEquals(eqCachedGains)
        if (changed) {
            val active = ArrayList<Int>(EQ_BANDS)
            for (i in 0 until EQ_BANDS) {
                val g = gains[i].toDouble()
                val f0 = EQ_FREQS[i].toDouble()
                if (abs(g) < 0.05 || f0 >= fs * 0.45) continue
                val w0 = 2.0 * PI * f0 / fs
                val cosw = cos(w0)
                val alpha = sin(w0) / (2.0 * EQ_Q)
                val amp = 10.0.pow(g / 40.0)
                val a0 = 1.0 + alpha / amp
                eqB0[i] = (1.0 + alpha * amp) / a0
                eqB1[i] = (-2.0 * cosw) / a0
                eqB2[i] = (1.0 - alpha * amp) / a0
                eqA1[i] = (-2.0 * cosw) / a0
                eqA2[i] = (1.0 - alpha / amp) / a0
                active.add(i)
            }
            eqActive = active.toIntArray()
            eqCachedGains = gains.copyOf()
            eqCachedSr = sampleRate
            eqCachedValid = true
        }
        val maxBoost = gains.maxOrNull()?.coerceAtLeast(0f) ?: 0f
        val preDb = eqPreampDb + if (eqAutoPreamp) -0.8f * maxBoost else 0f
        return 10.0f.pow(preDb / 20.0f)
    }

    private fun eqSample(ch: Int, x0: Float): Float {
        var x = x0.toDouble()
        val z1 = eqZ1[ch]
        val z2 = eqZ2[ch]
        val act = eqActive
        for (k in act.indices) {
            val b = act[k]
            val out = eqB0[b] * x + z1[b]
            var n1 = eqB1[b] * x - eqA1[b] * out + z2[b]
            var n2 = eqB2[b] * x - eqA2[b] * out
            if (abs(n1) < 1.0e-18) n1 = 0.0
            if (abs(n2) < 1.0e-18) n2 = 0.0
            z1[b] = n1
            z2[b] = n2
            x = out
        }
        return x.toFloat()
    }

    private fun clearReverb() {
        preDelayBufL.fill(0f); preDelayBufR.fill(0f); preDelayWriteIdx = 0
        for (i in 0 until 4) {
            combBufL[i].fill(0f); combBufR[i].fill(0f)
            combIdxL[i] = 0; combIdxR[i] = 0
            combStoreL[i] = 0f; combStoreR[i] = 0f
        }
        for (i in 0 until 2) {
            allpassBufL[i].fill(0f); allpassBufR[i].fill(0f)
            allpassIdxL[i] = 0; allpassIdxR[i] = 0
        }
        reverbLowCutStateL = 0f; reverbLowCutStateR = 0f
    }

    private fun softLimit(x: Float): Float {
        if (x != x) return 0f                                   // NaN guard
        val a = abs(x)
        if (a <= 0.9f) return x
        val s = 0.9f + 0.1f * tanh((a - 0.9f) / 0.1f)
        return if (x >= 0f) s else -s
    }
}
