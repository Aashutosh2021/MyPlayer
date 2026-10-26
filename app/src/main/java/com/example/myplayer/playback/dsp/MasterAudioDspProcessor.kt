package com.example.myplayer.playback.dsp

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.example.myplayer.data.preferences.AudioPlaybackState
import com.example.myplayer.data.preferences.Eq15Preset
import com.example.myplayer.data.preferences.LoudnessMode
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(UnstableApi::class)
@Singleton
class MasterAudioDspProcessor @Inject constructor() : BaseAudioProcessor() {

    companion object {
        init {
            try {
                System.loadLibrary("lastwave_audio_engine")
            } catch (t: Throwable) {
                t.printStackTrace()
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

    fun updateState(newState: AudioPlaybackState, trackLufs: Float = -14.0f, isSpatial: Boolean = false) {
        this.currentState = newState
        this.currentTrackLufs = trackLufs
        this.isSpatialStream = isSpatial
        if (inputAudioFormat.sampleRate > 0) {
            pushConfigToNative(inputAudioFormat.sampleRate)
        }
    }

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        // Support BOTH Float32 and Int16 PCM, Mono and Stereo (1..2 channels)
        val isSupportedEncoding = inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT ||
                inputAudioFormat.encoding == C.ENCODING_PCM_16BIT
        if (!isSupportedEncoding || inputAudioFormat.channelCount !in 1..2) {
            return AudioFormat.NOT_SET
        }
        pushConfigToNative(inputAudioFormat.sampleRate)
        return inputAudioFormat
    }

    private fun pushConfigToNative(sampleRate: Int) {
        val gainsArray = FloatArray(15) { i -> currentState.eqBandGains.getOrElse(i) { 0f } }

        // Studio Master Clarity is active when explicitly enabled OR when Studio Master preset is active
        val isStudioPreset = currentState.eqPreset == Eq15Preset.STUDIO_MASTER
        val effectiveClarity = (currentState.studioMasterClarity || isStudioPreset) &&
                !(currentState.claritySpatialBypass && isSpatialStream)

        val loudnessDb = when (currentState.loudnessMode) {
            LoudnessMode.OFF -> 0f
            LoudnessMode.TRACK -> (-14.0f - currentTrackLufs).coerceIn(-6f, 6f)
            LoudnessMode.ALBUM -> (-14.0f - (currentTrackLufs - 1.0f)).coerceIn(-6f, 5f)
        }

        try {
            nativeConfigure(
                sampleRate = if (sampleRate > 0) sampleRate else 48000,
                bitPerfect = currentState.bitPerfectMode || currentState.usbExclusiveOutput,
                eqEnabled = currentState.eqEnabled,
                eqGains = gainsArray,
                studioClarity = effectiveClarity,
                clarityPresetOrd = currentState.clarityOutputPreset.ordinal,
                loudnessGainDb = loudnessDb
            )
        } catch (t: Throwable) {
            // Safe fallback if native lib is unavailable
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val outputBuffer = replaceOutputBuffer(remaining).order(ByteOrder.nativeOrder())

        // Bit-Perfect Mode or USB Exclusive: 100% direct pass-through
        if (currentState.bitPerfectMode || currentState.usbExclusiveOutput) {
            outputBuffer.put(inputBuffer)
            outputBuffer.flip()
            return
        }

        // Copy input into output buffer
        val positionBefore = outputBuffer.position()
        outputBuffer.put(inputBuffer)

        val channelCount = inputAudioFormat.channelCount
        val bytesPerFrame = inputAudioFormat.bytesPerFrame
        val frameCount = remaining / bytesPerFrame

        try {
            if (inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT) {
                nativeProcessPcmFloat(
                    directBuffer = outputBuffer,
                    byteOffset = positionBefore,
                    frameCount = frameCount,
                    channelCount = channelCount
                )
            } else if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT) {
                nativeProcessPcm16(
                    directBuffer = outputBuffer,
                    byteOffset = positionBefore,
                    frameCount = frameCount,
                    channelCount = channelCount
                )
            }
        } catch (t: Throwable) {
            // Keep output intact on exception
        }

        outputBuffer.position(positionBefore + remaining)
        outputBuffer.flip()
    }
}
