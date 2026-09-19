package com.example.myplayer.dualbud.audio

import com.example.myplayer.dualbud.model.DualChannelId
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToInt

/**
 * High-performance thread-safe sample FIFO buffer for normalized 16-bit PCM audio samples.
 */
class PcmSampleBuffer(val capacity: Int = 48000) { // Default ~1 second at 48kHz
    private val buffer = ShortArray(capacity)
    private var head = 0
    private var tail = 0
    private val count = AtomicInteger(0)

    val size: Int get() = count.get()
    val isFull: Boolean get() = count.get() >= capacity
    val isEmpty: Boolean get() = count.get() == 0
    val remainingCapacity: Int get() = capacity - count.get()

    @Synchronized
    fun write(samples: ShortArray, offset: Int, length: Int): Int {
        val toWrite = minOf(length, remainingCapacity)
        if (toWrite <= 0) return 0

        for (i in 0 until toWrite) {
            buffer[tail] = samples[offset + i]
            tail = (tail + 1) % capacity
        }
        count.addAndGet(toWrite)
        return toWrite
    }

    @Synchronized
    fun read(target: ShortArray, offset: Int, length: Int): Int {
        val toRead = minOf(length, count.get())
        if (toRead <= 0) return 0

        for (i in 0 until toRead) {
            target[offset + i] = buffer[head]
            head = (head + 1) % capacity
        }
        count.addAndGet(-toRead)
        return toRead
    }

    @Synchronized
    fun clear() {
        head = 0
        tail = 0
        count.set(0)
    }
}

/**
 * Core stereo PCM mixer engine.
 *
 * Combines decoded PCM from two independent audio players into a single stereo output:
 * - Left Player PCM -> physical LEFT channel
 * - Right Player PCM -> physical RIGHT channel
 *
 * Guarantees zero cross-bleed, independent volume attenuation, clipping protection,
 * sample-rate normalization to 48kHz, and seamless swap routing without restarting playback.
 */
class DualChannelMixer(
    val outputDevice: AudioOutputDevice,
    private val sampleRate: Int = 48000
) {
    companion object {
        const val CHUNK_FRAMES = 1024 // ~21.3ms at 48kHz
        const val MAX_VOLUME = 1.0f
        const val MIN_VOLUME = 0.0f
    }

    // PCM FIFO queues for Left and Right channels
    val leftBuffer = PcmSampleBuffer(capacity = sampleRate) // 1 second capacity
    val rightBuffer = PcmSampleBuffer(capacity = sampleRate)

    // Independent channel volumes (0.0f to 1.0f)
    @Volatile var leftVolume: Float = 1.0f
        set(value) { field = value.coerceIn(MIN_VOLUME, MAX_VOLUME) }

    @Volatile var rightVolume: Float = 1.0f
        set(value) { field = value.coerceIn(MIN_VOLUME, MAX_VOLUME) }

    // Swap routing flag
    @Volatile var isSwapped: Boolean = false

    // Channel active / playing states
    @Volatile var isLeftPlaying: Boolean = false
    @Volatile var isRightPlaying: Boolean = false

    // Channel ended states
    @Volatile var isLeftEnded: Boolean = false
    @Volatile var isRightEnded: Boolean = false

    // Total frames consumed per channel (used for position tracking in AudioSink)
    val leftFramesConsumed = AtomicLong(0L)
    val rightFramesConsumed = AtomicLong(0L)

    // Mixer lifecycle
    private val isRunning = AtomicBoolean(false)
    private var mixerJob: Job? = null
    private val mixerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Starts the audio output device and mixing loop.
     *
     * @param startWorkerLoop Set to false in unit tests to mix chunks deterministically.
     */
    fun start(startWorkerLoop: Boolean = true) {
        if (isRunning.compareAndSet(false, true)) {
            outputDevice.open(CHUNK_FRAMES * 4)
            outputDevice.play()
            if (startWorkerLoop) {
                mixerJob = mixerScope.launch {
                    runMixingLoop()
                }
            }
        }
    }

    /**
     * Stops the mixing loop and releases audio output hardware.
     */
    fun stop() {
        if (isRunning.compareAndSet(true, false)) {
            mixerJob?.cancel()
            mixerJob = null
            outputDevice.pause()
            outputDevice.flush()
            outputDevice.release()
            leftBuffer.clear()
            rightBuffer.clear()
            leftFramesConsumed.set(0L)
            rightFramesConsumed.set(0L)
        }
    }

    /**
     * Enqueues PCM samples for the designated channel.
     * Converts from source sample rate to 48kHz mono if needed.
     *
     * @return Number of input samples accepted.
     */
    fun enqueueSamples(
        channel: DualChannelId,
        inputPcm: ShortArray,
        offset: Int,
        count: Int,
        sourceSampleRate: Int,
        sourceChannels: Int
    ): Int {
        if (!isRunning.get()) return 0

        // 1. Downmix to mono if source is stereo or multi-channel
        val monoSamples = if (sourceChannels == 1) {
            ShortArray(count) { inputPcm[offset + it] }
        } else {
            val frames = count / sourceChannels
            val mono = ShortArray(frames)
            for (f in 0 until frames) {
                var sum = 0
                for (ch in 0 until sourceChannels) {
                    sum += inputPcm[offset + f * sourceChannels + ch]
                }
                mono[f] = (sum / sourceChannels).coerceIn(-32768, 32767).toShort()
            }
            mono
        }

        // 2. Resample to 48kHz if source rate differs
        val normalizedSamples = if (sourceSampleRate == sampleRate) {
            monoSamples
        } else {
            resampleLinear(monoSamples, sourceSampleRate, sampleRate)
        }

        // 3. Write to the respective FIFO buffer
        val targetBuffer = if (channel == DualChannelId.LEFT) leftBuffer else rightBuffer
        val written = targetBuffer.write(normalizedSamples, 0, normalizedSamples.size)

        // Convert written normalized count back to input sample units
        val ratio = (sourceSampleRate.toDouble() / sampleRate.toDouble()) * sourceChannels
        return (written * ratio).roundToInt().coerceAtMost(count)
    }

    /**
     * Processes one mix block synchronously.
     * Useful for deterministic testing and directly called by the worker loop.
     *
     * @return Number of stereo frames written to [outputDevice].
     */
    fun mixChunk(): Int {
        val leftScratch = ShortArray(CHUNK_FRAMES)
        val rightScratch = ShortArray(CHUNK_FRAMES)
        val stereoOutput = ShortArray(CHUNK_FRAMES * 2)

        val leftAvailable = if (isLeftPlaying && !isLeftEnded) {
            leftBuffer.read(leftScratch, 0, CHUNK_FRAMES)
        } else 0

        val rightAvailable = if (isRightPlaying && !isRightEnded) {
            rightBuffer.read(rightScratch, 0, CHUNK_FRAMES)
        } else 0

        // If neither stream has data and neither is playing, yield
        if (leftAvailable == 0 && rightAvailable == 0 && !isLeftPlaying && !isRightPlaying) {
            return 0
        }

        val framesToMix = maxOf(leftAvailable, rightAvailable, 1)

        val lVol = leftVolume
        val rVol = rightVolume
        val swapped = isSwapped

        for (i in 0 until framesToMix) {
            // Left Player PCM sample (silence if buffer empty, paused, or ended)
            val leftRaw = if (i < leftAvailable) leftScratch[i] else 0
            val leftSample = (leftRaw * lVol).roundToInt().coerceIn(-32768, 32767).toShort()

            // Right Player PCM sample (silence if buffer empty, paused, or ended)
            val rightRaw = if (i < rightAvailable) rightScratch[i] else 0
            val rightSample = (rightRaw * rVol).roundToInt().coerceIn(-32768, 32767).toShort()

            // Interleaved stereo PCM output:
            // Normal:  [L = leftPlayer,  R = rightPlayer]
            // Swapped: [L = rightPlayer, R = leftPlayer]
            if (!swapped) {
                stereoOutput[2 * i] = leftSample       // Physical LEFT
                stereoOutput[2 * i + 1] = rightSample  // Physical RIGHT
            } else {
                stereoOutput[2 * i] = rightSample      // Physical LEFT gets Right Player
                stereoOutput[2 * i + 1] = leftSample   // Physical RIGHT gets Left Player
            }
        }

        // Advance consumption counters
        if (leftAvailable > 0) leftFramesConsumed.addAndGet(leftAvailable.toLong())
        if (rightAvailable > 0) rightFramesConsumed.addAndGet(rightAvailable.toLong())

        // Write stereo PCM to the output device
        return outputDevice.write(stereoOutput, 0, framesToMix * 2) / 2
    }

    private suspend fun runMixingLoop() = withContext(Dispatchers.Default) {
        while (isRunning.get() && isActive) {
            val mixedFrames = mixChunk()
            if (mixedFrames == 0) {
                // Buffer underrun or idle: sleep briefly (~10ms) to avoid busy looping
                delay(10)
            }
        }
    }

    /**
     * Resets a specific channel buffer (e.g. upon seek or track change).
     */
    fun flushChannel(channel: DualChannelId) {
        if (channel == DualChannelId.LEFT) {
            leftBuffer.clear()
        } else {
            rightBuffer.clear()
        }
    }

    /**
     * Swaps Left and Right audio routing instantaneously without restarting playback.
     */
    fun toggleSwap() {
        isSwapped = !isSwapped
    }

    /**
     * Linear interpolation resampler for 16-bit PCM mono samples.
     */
    private fun resampleLinear(input: ShortArray, inRate: Int, outRate: Int): ShortArray {
        if (input.isEmpty() || inRate == outRate) return input
        val ratio = inRate.toDouble() / outRate.toDouble()
        val outLength = (input.size / ratio).roundToInt()
        if (outLength <= 0) return ShortArray(0)

        val output = ShortArray(outLength)
        for (i in 0 until outLength) {
            val inIndex = i * ratio
            val index0 = inIndex.toInt()
            val index1 = minOf(index0 + 1, input.size - 1)
            val frac = inIndex - index0
            val s0 = input[index0].toDouble()
            val s1 = input[index1].toDouble()
            val interpolated = s0 + frac * (s1 - s0)
            output[i] = interpolated.roundToInt().coerceIn(-32768, 32767).toShort()
        }
        return output
    }
}
