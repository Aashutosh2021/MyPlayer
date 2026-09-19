package com.example.myplayer.dualbud.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Interface representing the stereo PCM audio hardware/sink output.
 */
interface AudioOutputDevice {
    val sampleRate: Int
    val channelCount: Int

    fun open(bufferSizeFrames: Int)
    fun write(audioData: ShortArray, offsetInShorts: Int, sizeInShorts: Int): Int
    fun play()
    fun pause()
    fun flush()
    fun release()
    fun getPlaybackHeadPositionFrames(): Long
}

/**
 * Real Android [AudioTrack] output device for hardware/Bluetooth playback.
 * Outputs standard interleaved stereo 16-bit PCM at 48,000 Hz.
 */
class AndroidAudioTrackOutput(
    override val sampleRate: Int = 48000
) : AudioOutputDevice {

    override val channelCount: Int = 2
    private var audioTrack: AudioTrack? = null

    override fun open(bufferSizeFrames: Int) {
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val targetBufferSize = maxOf(minBufferSize, bufferSizeFrames * 2 * 2) // 2 channels * 2 bytes per short

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
            .build()

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(format)
            .setBufferSizeInBytes(targetBufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    override fun write(audioData: ShortArray, offsetInShorts: Int, sizeInShorts: Int): Int {
        val track = audioTrack ?: return 0
        return track.write(audioData, offsetInShorts, sizeInShorts, AudioTrack.WRITE_BLOCKING)
    }

    override fun play() {
        try {
            audioTrack?.play()
        } catch (e: Exception) {
            // Non-fatal if track not ready
        }
    }

    override fun pause() {
        try {
            audioTrack?.pause()
        } catch (e: Exception) {
            // Non-fatal
        }
    }

    override fun flush() {
        try {
            audioTrack?.flush()
        } catch (e: Exception) {
            // Non-fatal
        }
    }

    override fun release() {
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            // Non-fatal
        } finally {
            audioTrack = null
        }
    }

    override fun getPlaybackHeadPositionFrames(): Long {
        return (audioTrack?.playbackHeadPosition?.toLong() ?: 0L) and 0xFFFFFFFFL
    }
}

/**
 * In-memory [AudioOutputDevice] implementation for testing without Android hardware.
 * Records written stereo frames and enables precise verification of channel isolation,
 * volume attenuation, clipping prevention, and swap routing.
 */
class TestAudioOutputDevice(
    override val sampleRate: Int = 48000
) : AudioOutputDevice {

    override val channelCount: Int = 2
    private var isPlaying = false
    private var headPositionFrames: Long = 0L

    val recordedFrames = CopyOnWriteArrayList<Short>()

    override fun open(bufferSizeFrames: Int) {
        headPositionFrames = 0L
        recordedFrames.clear()
    }

    override fun write(audioData: ShortArray, offsetInShorts: Int, sizeInShorts: Int): Int {
        val count = sizeInShorts
        for (i in offsetInShorts until (offsetInShorts + count)) {
            recordedFrames.add(audioData[i])
        }
        headPositionFrames += (count / 2) // 2 shorts per stereo frame
        return count
    }

    override fun play() {
        isPlaying = true
    }

    override fun pause() {
        isPlaying = false
    }

    override fun flush() {
        recordedFrames.clear()
    }

    override fun release() {
        isPlaying = false
        recordedFrames.clear()
    }

    override fun getPlaybackHeadPositionFrames(): Long {
        return headPositionFrames
    }

    /** Returns all written Left channel samples (even indices: 0, 2, 4, ...). */
    fun getLeftChannelSamples(): ShortArray {
        val list = recordedFrames
        val size = list.size / 2
        val left = ShortArray(size)
        for (i in 0 until size) {
            left[i] = list[i * 2]
        }
        return left
    }

    /** Returns all written Right channel samples (odd indices: 1, 3, 5, ...). */
    fun getRightChannelSamples(): ShortArray {
        val list = recordedFrames
        val size = list.size / 2
        val right = ShortArray(size)
        for (i in 0 until size) {
            right[i] = list[i * 2 + 1]
        }
        return right
    }
}
