package com.example.myplayer.dualbud.audio

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.Clock
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.analytics.PlayerId
import androidx.media3.exoplayer.audio.AudioSink
import com.example.myplayer.dualbud.model.DualChannelId
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Custom Media3 [AudioSink] for a single channel (Left or Right) of the Dual Bud subsystem.
 * Intercepts decoded PCM audio buffers from ExoPlayer's audio renderer and deposits them into
 * the shared [DualChannelMixer], providing backpressure and accurate position tracking.
 */
@UnstableApi
class ChannelAudioSink(
    val channelId: DualChannelId,
    val mixer: DualChannelMixer
) : AudioSink {

    private var listener: AudioSink.Listener? = null
    private var sourceSampleRate = 48000
    private var sourceChannelCount = 2
    private var isPlaying = false
    private var isEnded = false
    private var startPositionUs = 0L
    private var playbackParameters = PlaybackParameters.DEFAULT

    override fun setListener(listener: AudioSink.Listener) {
        this.listener = listener
    }

    override fun supportsFormat(format: Format): Boolean {
        return MimeTypes.AUDIO_RAW == format.sampleMimeType &&
                (format.pcmEncoding == C.ENCODING_PCM_16BIT || format.pcmEncoding == Format.NO_VALUE)
    }

    override fun getFormatSupport(format: Format): Int {
        return if (supportsFormat(format)) {
            AudioSink.SINK_FORMAT_SUPPORTED_DIRECTLY
        } else {
            AudioSink.SINK_FORMAT_UNSUPPORTED
        }
    }

    override fun getCurrentPositionUs(sourceEnded: Boolean): Long {
        if (isEnded || sourceEnded) {
            return startPositionUs + (getFramesConsumed() * 1_000_000L / 48000L)
        }
        val consumed = getFramesConsumed()
        return startPositionUs + (consumed * 1_000_000L / 48000L)
    }

    private fun getFramesConsumed(): Long {
        return if (channelId == DualChannelId.LEFT) {
            mixer.leftFramesConsumed.get()
        } else {
            mixer.rightFramesConsumed.get()
        }
    }

    override fun configure(
        inputFormat: Format,
        specifiedBufferSize: Int,
        outputChannels: IntArray?
    ) {
        sourceSampleRate = if (inputFormat.sampleRate != Format.NO_VALUE) inputFormat.sampleRate else 48000
        sourceChannelCount = if (inputFormat.channelCount != Format.NO_VALUE) inputFormat.channelCount else 2
    }

    override fun play() {
        isPlaying = true
        isEnded = false
        if (channelId == DualChannelId.LEFT) {
            mixer.isLeftPlaying = true
            mixer.isLeftEnded = false
        } else {
            mixer.isRightPlaying = true
            mixer.isRightEnded = false
        }
    }

    override fun handleBuffer(
        buffer: ByteBuffer,
        presentationTimeUs: Long,
        encodedAccessUnitCount: Int
    ): Boolean {
        if (!buffer.hasRemaining()) return true

        // Capture starting playback position on first buffer
        if (getFramesConsumed() == 0L && startPositionUs == 0L) {
            startPositionUs = presentationTimeUs
        }

        val targetBuffer = if (channelId == DualChannelId.LEFT) mixer.leftBuffer else mixer.rightBuffer
        // Check backpressure: if queue has less than 2048 samples of space, wait
        if (targetBuffer.remainingCapacity < DualChannelMixer.CHUNK_FRAMES * 2) {
            return false // Backpressure: ExoPlayer will retry on next render tick
        }

        buffer.order(ByteOrder.LITTLE_ENDIAN)
        val shortBuffer = buffer.asShortBuffer()
        val totalShorts = shortBuffer.remaining()
        if (totalShorts <= 0) {
            buffer.position(buffer.limit())
            return true
        }

        val tempShorts = ShortArray(minOf(totalShorts, DualChannelMixer.CHUNK_FRAMES * sourceChannelCount))
        shortBuffer.get(tempShorts)
        buffer.position(buffer.position() + tempShorts.size * 2)

        mixer.enqueueSamples(
            channel = channelId,
            inputPcm = tempShorts,
            offset = 0,
            count = tempShorts.size,
            sourceSampleRate = sourceSampleRate,
            sourceChannels = sourceChannelCount
        )

        return !buffer.hasRemaining()
    }

    override fun playToEndOfStream() {
        isEnded = true
        if (channelId == DualChannelId.LEFT) {
            mixer.isLeftEnded = true
        } else {
            mixer.isRightEnded = true
        }
    }

    override fun isEnded(): Boolean {
        val targetBuffer = if (channelId == DualChannelId.LEFT) mixer.leftBuffer else mixer.rightBuffer
        return isEnded && targetBuffer.isEmpty
    }

    override fun hasPendingData(): Boolean {
        val targetBuffer = if (channelId == DualChannelId.LEFT) mixer.leftBuffer else mixer.rightBuffer
        return !targetBuffer.isEmpty
    }

    override fun setPlaybackParameters(playbackParameters: PlaybackParameters) {
        this.playbackParameters = playbackParameters
    }

    override fun getPlaybackParameters(): PlaybackParameters = playbackParameters

    override fun setAudioAttributes(audioAttributes: AudioAttributes) {
        // Handled by DualChannelMixer's AudioOutputDevice
    }

    override fun getAudioAttributes(): AudioAttributes? = null

    override fun setAudioSessionId(audioSessionId: Int) {}

    override fun setAuxEffectInfo(auxEffectInfo: androidx.media3.common.AuxEffectInfo) {}

    override fun enableTunnelingV21() {}

    override fun disableTunneling() {}

    override fun setVolume(volume: Float) {
        // Individual channel volume is controlled directly in DualChannelMixer
    }

    override fun pause() {
        isPlaying = false
        if (channelId == DualChannelId.LEFT) {
            mixer.isLeftPlaying = false
        } else {
            mixer.isRightPlaying = false
        }
    }

    override fun flush() {
        mixer.flushChannel(channelId)
        isEnded = false
        startPositionUs = 0L
        if (channelId == DualChannelId.LEFT) {
            mixer.isLeftEnded = false
            mixer.leftFramesConsumed.set(0L)
        } else {
            mixer.isRightEnded = false
            mixer.rightFramesConsumed.set(0L)
        }
    }

    override fun reset() {
        flush()
        isPlaying = false
    }

    override fun release() {
        reset()
    }

    override fun handleDiscontinuity() {}

    override fun setSkipSilenceEnabled(skipSilenceEnabled: Boolean) {}

    override fun getSkipSilenceEnabled(): Boolean = false

    override fun setPlayerId(playerId: PlayerId?) {}

    override fun setClock(clock: Clock) {}

    override fun setOutputStreamOffsetUs(outputStreamOffsetUs: Long) {}
}
