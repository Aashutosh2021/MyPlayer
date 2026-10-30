package com.myplayer.app.myplayerfx.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.sqrt

/**
 * What the Audio Lab pictures listen to: the playback output folded to mono into a ring,
 * only filled while a page is looking (wanted) so playback pays nothing otherwise.
 */
object MyPlayerAudioTap {

    private const val RingSize = 1 shl 15
    private const val Mask = RingSize - 1

    /** audio is processed ahead of the speaker, so pictures read this far behind to line up with what is heard */
    private const val PlaybackLagSeconds = 0.2

    private val ring = FloatArray(RingSize)

    @Volatile private var writeIndex = 0L
    @Volatile var sampleRate = 44_100
        private set
    @Volatile private var lastWriteNanos = 0L

    @Volatile private var listeners = 0

    /** newest loudness-reduction the loudness engine applied (db, <= 0), for the FX meter */
    @Volatile var loudnessReductionDb = 0f

    fun listen() { synchronized(this) { listeners++ } }
    fun stopListening() { synchronized(this) { listeners = max(0, listeners - 1) } }

    val wanted: Boolean get() = listeners > 0

    /** whether music went through recently: pictures settle into their idle motion when not */
    val isLive: Boolean get() = System.nanoTime() - lastWriteNanos < 400_000_000L

    /** audio thread: folds a pcm16 interleaved buffer to mono into the ring, without moving the original buffer */
    fun feed(input: ByteBuffer, channels: Int, rate: Int) {
        if (!wanted || channels == 0 || rate <= 0) return
        lastWriteNanos = System.nanoTime()
        sampleRate = rate
        val dup = input.duplicate().order(ByteOrder.nativeOrder())
        val frames = dup.remaining() / (2 * channels)
        val inv = 1f / channels
        val scale = 1f / 32768f
        var w = writeIndex
        repeat(frames) {
            var sum = 0f
            for (c in 0 until channels) sum += dup.short * scale
            ring[(w and Mask.toLong()).toInt()] = sum * inv
            w++
        }
        writeIndex = w
    }

    /** copies the newest count samples into into, oldest first */
    fun read(into: FloatArray, count: Int = into.size): Boolean {
        if (!isLive) return false
        val lag = (sampleRate * PlaybackLagSeconds).toLong()
        val end = writeIndex - lag
        if (end < count) return false
        var r = end - count
        for (i in 0 until count) {
            into[i] = ring[(r and Mask.toLong()).toInt()]
            r++
        }
        return true
    }

    /** rms of the newest samples heard (scratch.size of them), 0..1 */
    fun level(scratch: FloatArray): Float {
        if (!read(scratch)) return 0f
        var sum = 0.0
        for (v in scratch) sum += v * v
        return sqrt(sum / scratch.size).toFloat().coerceIn(0f, 1f)
    }
}
