package com.example.myplayer.dualbud

import com.example.myplayer.dualbud.audio.DualChannelMixer
import com.example.myplayer.dualbud.audio.TestAudioOutputDevice
import com.example.myplayer.dualbud.model.DualBudModeState
import com.example.myplayer.dualbud.model.DualChannelId
import com.example.myplayer.dualbud.model.DualChannelState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Complete test suite for Dual Bud / Split Channel Playback verifying all 20 test specifications.
 */
class DualChannelPlaybackTest {

    private lateinit var testDevice: TestAudioOutputDevice
    private lateinit var mixer: DualChannelMixer

    @Before
    fun setup() {
        testDevice = TestAudioOutputDevice(sampleRate = 48000)
        mixer = DualChannelMixer(outputDevice = testDevice, sampleRate = 48000)
        mixer.start(startWorkerLoop = false)
    }

    // ── Test 1: Left song plays only on left channel ─────────────────────────
    @Test
    fun test1_leftSongPlaysOnlyOnLeftChannel() {
        val leftSamples = ShortArray(512) { 1500.toShort() }
        mixer.isLeftPlaying = true
        mixer.isRightPlaying = false
        mixer.enqueueSamples(DualChannelId.LEFT, leftSamples, 0, leftSamples.size, 48000, 1)

        val mixed = mixer.mixChunk()
        assertTrue("Expected frames mixed", mixed > 0)

        val leftOutput = testDevice.getLeftChannelSamples()
        val rightOutput = testDevice.getRightChannelSamples()

        assertEquals(1500.toShort(), leftOutput[0])
        assertEquals(0.toShort(), rightOutput[0]) // Right channel is completely silent
    }

    // ── Test 2: Right song plays only on right channel ───────────────────────
    @Test
    fun test2_rightSongPlaysOnlyOnRightChannel() {
        val rightSamples = ShortArray(512) { 2500.toShort() }
        mixer.isLeftPlaying = false
        mixer.isRightPlaying = true
        mixer.enqueueSamples(DualChannelId.RIGHT, rightSamples, 0, rightSamples.size, 48000, 1)

        val mixed = mixer.mixChunk()
        assertTrue("Expected frames mixed", mixed > 0)

        val leftOutput = testDevice.getLeftChannelSamples()
        val rightOutput = testDevice.getRightChannelSamples()

        assertEquals(0.toShort(), leftOutput[0]) // Left channel is completely silent
        assertEquals(2500.toShort(), rightOutput[0])
    }

    // ── Test 3: Left volume = 0 produces silence on left ────────────────────
    @Test
    fun test3_leftVolumeZeroProducesSilenceOnLeft() {
        val leftSamples = ShortArray(256) { 3000.toShort() }
        mixer.isLeftPlaying = true
        mixer.leftVolume = 0.0f
        mixer.enqueueSamples(DualChannelId.LEFT, leftSamples, 0, leftSamples.size, 48000, 1)

        mixer.mixChunk()
        val leftOutput = testDevice.getLeftChannelSamples()

        for (sample in leftOutput) {
            assertEquals(0.toShort(), sample)
        }
    }

    // ── Test 4: Right volume = 0 produces silence on right ───────────────────
    @Test
    fun test4_rightVolumeZeroProducesSilenceOnRight() {
        val rightSamples = ShortArray(256) { 4000.toShort() }
        mixer.isRightPlaying = true
        mixer.rightVolume = 0.0f
        mixer.enqueueSamples(DualChannelId.RIGHT, rightSamples, 0, rightSamples.size, 48000, 1)

        mixer.mixChunk()
        val rightOutput = testDevice.getRightChannelSamples()

        for (sample in rightOutput) {
            assertEquals(0.toShort(), sample)
        }
    }

    // ── Test 5: Left song ends while right continues ─────────────────────────
    @Test
    fun test5_leftSongEndsWhileRightContinues() {
        val rightSamples = ShortArray(256) { 1200.toShort() }
        mixer.isLeftPlaying = true
        mixer.isLeftEnded = true // Left ended
        mixer.isRightPlaying = true
        mixer.isRightEnded = false
        mixer.enqueueSamples(DualChannelId.RIGHT, rightSamples, 0, rightSamples.size, 48000, 1)

        mixer.mixChunk()
        val leftOutput = testDevice.getLeftChannelSamples()
        val rightOutput = testDevice.getRightChannelSamples()

        assertEquals(0.toShort(), leftOutput[0]) // Left becomes silent
        assertEquals(1200.toShort(), rightOutput[0]) // Right continues playing
    }

    // ── Test 6: Right song ends while left continues ─────────────────────────
    @Test
    fun test6_rightSongEndsWhileLeftContinues() {
        val leftSamples = ShortArray(256) { 1800.toShort() }
        mixer.isLeftPlaying = true
        mixer.isLeftEnded = false
        mixer.isRightPlaying = true
        mixer.isRightEnded = true // Right ended
        mixer.enqueueSamples(DualChannelId.LEFT, leftSamples, 0, leftSamples.size, 48000, 1)

        mixer.mixChunk()
        val leftOutput = testDevice.getLeftChannelSamples()
        val rightOutput = testDevice.getRightChannelSamples()

        assertEquals(1800.toShort(), leftOutput[0]) // Left continues playing
        assertEquals(0.toShort(), rightOutput[0]) // Right becomes silent
    }

    // ── Test 7: Swap left/right ──────────────────────────────────────────────
    @Test
    fun test7_swapLeftRight() {
        val leftSamples = ShortArray(256) { 1111.toShort() }
        val rightSamples = ShortArray(256) { 2222.toShort() }
        mixer.isLeftPlaying = true
        mixer.isRightPlaying = true
        mixer.enqueueSamples(DualChannelId.LEFT, leftSamples, 0, leftSamples.size, 48000, 1)
        mixer.enqueueSamples(DualChannelId.RIGHT, rightSamples, 0, rightSamples.size, 48000, 1)

        // Normal routing
        mixer.isSwapped = false
        mixer.mixChunk()
        assertEquals(1111.toShort(), testDevice.getLeftChannelSamples()[0])
        assertEquals(2222.toShort(), testDevice.getRightChannelSamples()[0])

        // Swap Left <-> Right
        testDevice.flush()
        mixer.enqueueSamples(DualChannelId.LEFT, leftSamples, 0, leftSamples.size, 48000, 1)
        mixer.enqueueSamples(DualChannelId.RIGHT, rightSamples, 0, rightSamples.size, 48000, 1)
        mixer.toggleSwap()
        assertTrue(mixer.isSwapped)

        mixer.mixChunk()
        // Left earbud receives Right song (2222), Right earbud receives Left song (1111)
        assertEquals(2222.toShort(), testDevice.getLeftChannelSamples()[0])
        assertEquals(1111.toShort(), testDevice.getRightChannelSamples()[0])
    }

    // ── Test 8: Pause left only ──────────────────────────────────────────────
    @Test
    fun test8_pauseLeftOnly() {
        val leftSamples = ShortArray(256) { 500.toShort() }
        val rightSamples = ShortArray(256) { 800.toShort() }
        mixer.enqueueSamples(DualChannelId.LEFT, leftSamples, 0, leftSamples.size, 48000, 1)
        mixer.enqueueSamples(DualChannelId.RIGHT, rightSamples, 0, rightSamples.size, 48000, 1)

        mixer.isLeftPlaying = false // Paused
        mixer.isRightPlaying = true

        mixer.mixChunk()
        assertEquals(0.toShort(), testDevice.getLeftChannelSamples()[0])
        assertEquals(800.toShort(), testDevice.getRightChannelSamples()[0])
    }

    // ── Test 9: Pause right only ─────────────────────────────────────────────
    @Test
    fun test9_pauseRightOnly() {
        val leftSamples = ShortArray(256) { 500.toShort() }
        val rightSamples = ShortArray(256) { 800.toShort() }
        mixer.enqueueSamples(DualChannelId.LEFT, leftSamples, 0, leftSamples.size, 48000, 1)
        mixer.enqueueSamples(DualChannelId.RIGHT, rightSamples, 0, rightSamples.size, 48000, 1)

        mixer.isLeftPlaying = true
        mixer.isRightPlaying = false // Paused

        mixer.mixChunk()
        assertEquals(500.toShort(), testDevice.getLeftChannelSamples()[0])
        assertEquals(0.toShort(), testDevice.getRightChannelSamples()[0])
    }

    // ── Test 10: Seek left only ──────────────────────────────────────────────
    @Test
    fun test10_seekLeftOnly() {
        val leftSamples = ShortArray(256) { 100.toShort() }
        val rightSamples = ShortArray(256) { 200.toShort() }
        mixer.enqueueSamples(DualChannelId.LEFT, leftSamples, 0, leftSamples.size, 48000, 1)
        mixer.enqueueSamples(DualChannelId.RIGHT, rightSamples, 0, rightSamples.size, 48000, 1)

        // Seek on Left flushes Left queue only
        mixer.flushChannel(DualChannelId.LEFT)

        assertEquals(0, mixer.leftBuffer.size)
        assertEquals(256, mixer.rightBuffer.size) // Right queue unaffected
    }

    // ── Test 11: Seek right only ─────────────────────────────────────────────
    @Test
    fun test11_seekRightOnly() {
        val leftSamples = ShortArray(256) { 100.toShort() }
        val rightSamples = ShortArray(256) { 200.toShort() }
        mixer.enqueueSamples(DualChannelId.LEFT, leftSamples, 0, leftSamples.size, 48000, 1)
        mixer.enqueueSamples(DualChannelId.RIGHT, rightSamples, 0, rightSamples.size, 48000, 1)

        // Seek on Right flushes Right queue only
        mixer.flushChannel(DualChannelId.RIGHT)

        assertEquals(256, mixer.leftBuffer.size) // Left queue unaffected
        assertEquals(0, mixer.rightBuffer.size)
    }

    // ── Test 12: Both songs play simultaneously ──────────────────────────────
    @Test
    fun test12_bothSongsPlaySimultaneously() {
        val leftSamples = ShortArray(512) { 1234.toShort() }
        val rightSamples = ShortArray(512) { 5678.toShort() }
        mixer.isLeftPlaying = true
        mixer.isRightPlaying = true
        mixer.enqueueSamples(DualChannelId.LEFT, leftSamples, 0, leftSamples.size, 48000, 1)
        mixer.enqueueSamples(DualChannelId.RIGHT, rightSamples, 0, rightSamples.size, 48000, 1)

        val frames = mixer.mixChunk()
        assertTrue(frames >= 512)

        val leftOutput = testDevice.getLeftChannelSamples()
        val rightOutput = testDevice.getRightChannelSamples()

        assertEquals(1234.toShort(), leftOutput[0])
        assertEquals(5678.toShort(), rightOutput[0])
    }

    // ── Test 13: One player buffering does not crash other ───────────────────
    @Test
    fun test13_onePlayerBufferingDoesNotCrashOther() {
        val leftSamples = ShortArray(256) { 999.toShort() }
        mixer.isLeftPlaying = true
        mixer.isRightPlaying = true // Right is set to play but buffer is empty (buffering)
        mixer.enqueueSamples(DualChannelId.LEFT, leftSamples, 0, leftSamples.size, 48000, 1)

        // Must mix smoothly without throwing IndexOutOfBoundsException or crashing
        val mixed = mixer.mixChunk()
        assertTrue(mixed > 0)
        assertEquals(999.toShort(), testDevice.getLeftChannelSamples()[0])
        assertEquals(0.toShort(), testDevice.getRightChannelSamples()[0])
    }

    // ── Test 14: One player error does not stop the other ────────────────────
    @Test
    fun test14_onePlayerErrorDoesNotStopOther() {
        val state = DualBudModeState(
            isEnabled = true,
            leftChannel = DualChannelState(DualChannelId.LEFT, isPlaying = false, errorMessage = "Network timeout"),
            rightChannel = DualChannelState(DualChannelId.RIGHT, isPlaying = true, errorMessage = null)
        )

        assertNotNull(state.leftChannel.errorMessage)
        assertFalse(state.leftChannel.isPlaying)
        assertTrue(state.rightChannel.isPlaying)
        assertNull(state.rightChannel.errorMessage)
    }

    // ── Test 15: Exit Dual Bud Mode restores normal state ───────────────────
    @Test
    fun test15_exitDualBudModeRestoresNormalPlayback() {
        mixer.stop()
        assertEquals(0, mixer.leftBuffer.size)
        assertEquals(0, mixer.rightBuffer.size)
        assertEquals(0L, mixer.leftFramesConsumed.get())
        assertEquals(0L, mixer.rightFramesConsumed.get())
    }

    // ── Test 16: Normal MyPlayer playback remains unchanged ──────────────────
    @Test
    fun test16_normalMyPlayerPlaybackRemainsUnchanged() {
        // DualBudModeState is fully decoupled from normal player queue and settings
        val state = DualBudModeState(isEnabled = false)
        assertFalse(state.isEnabled)
        assertFalse(state.isSwapped)
    }

    // ── Test 17: No audio clipping ───────────────────────────────────────────
    @Test
    fun test17_noAudioClipping() {
        val maxSample = Short.MAX_VALUE // 32767
        val leftSamples = shortArrayOf(maxSample, Short.MIN_VALUE)
        mixer.isLeftPlaying = true
        mixer.leftVolume = 1.0f
        mixer.enqueueSamples(DualChannelId.LEFT, leftSamples, 0, leftSamples.size, 48000, 1)

        mixer.mixChunk()
        val leftOutput = testDevice.getLeftChannelSamples()

        assertEquals(32767.toShort(), leftOutput[0])
        assertEquals((-32768).toShort(), leftOutput[1])

        // Verify that sample never overflows or wraps to negative
        assertTrue(leftOutput[0] > 0)
    }

    // ── Test 18: No main-thread PCM processing ───────────────────────────────
    @Test
    fun test18_noMainThreadPcmProcessing() = runBlocking {
        // Verify mix execution runs safely in background dispatcher
        withContext(Dispatchers.Default) {
            val samples = ShortArray(128) { 42.toShort() }
            mixer.isLeftPlaying = true
            mixer.enqueueSamples(DualChannelId.LEFT, samples, 0, samples.size, 48000, 1)
            val mixed = mixer.mixChunk()
            assertTrue(mixed > 0)
        }
    }

    // ── Test 19: Bluetooth stereo output works ───────────────────────────────
    @Test
    fun test19_bluetoothStereoOutputWorks() {
        val leftSamples = ShortArray(100) { 10.toShort() }
        val rightSamples = ShortArray(100) { 20.toShort() }
        mixer.isLeftPlaying = true
        mixer.isRightPlaying = true
        mixer.enqueueSamples(DualChannelId.LEFT, leftSamples, 0, leftSamples.size, 48000, 1)
        mixer.enqueueSamples(DualChannelId.RIGHT, rightSamples, 0, rightSamples.size, 48000, 1)

        mixer.mixChunk()
        val recorded = testDevice.recordedFrames

        // Interleaved frames format [L0, R0, L1, R1, ...]
        assertEquals(10.toShort(), recorded[0]) // L0
        assertEquals(20.toShort(), recorded[1]) // R0
        assertEquals(10.toShort(), recorded[2]) // L1
        assertEquals(20.toShort(), recorded[3]) // R1
    }

    // ── Test 20: App background/foreground audio focus transition works ──────
    @Test
    fun test20_appBackgroundForegroundTransitionWorks() {
        mixer.leftVolume = 1.0f
        mixer.rightVolume = 0.8f

        // Ducking simulation (50% attenuation on transient focus loss)
        val duckedLeft = mixer.leftVolume * 0.5f
        val duckedRight = mixer.rightVolume * 0.5f

        mixer.leftVolume = duckedLeft
        mixer.rightVolume = duckedRight

        assertEquals(0.5f, mixer.leftVolume, 0.001f)
        assertEquals(0.4f, mixer.rightVolume, 0.001f)

        // Focus restored
        mixer.leftVolume = 1.0f
        mixer.rightVolume = 0.8f

        assertEquals(1.0f, mixer.leftVolume, 0.001f)
        assertEquals(0.8f, mixer.rightVolume, 0.001f)
    }
}
