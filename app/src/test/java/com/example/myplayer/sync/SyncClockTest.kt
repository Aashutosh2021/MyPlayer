package com.example.myplayer.sync

import com.example.myplayer.sync.clock.SyncClock
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SyncClockTest {

    private lateinit var syncClock: SyncClock

    @Before
    fun setUp() {
        syncClock = SyncClock()
    }

    @Test
    fun testClockOffsetCalculation() {
        // Slave sends Ping at t1=1000
        // Master receives at t2=1050 (offset +40ms, travel time 10ms)
        // Master sends Pong at t3=1060
        // Slave receives at t4=1120
        // RTT = (1120 - 1000) - (1060 - 1050) = 120 - 10 = 110ms
        // Offset = ((1050 - 1000) + (1060 - 1120)) / 2 = (50 - 60) / 2 = -5ms
        syncClock.recordSample(t1 = 1000L, t2 = 1050L, t3 = 1060L, t4 = 1120L)

        val state = syncClock.clockState.value
        assertEquals(110L, state.roundTripTimeMs)
        assertEquals(-5L, state.offsetMs)
        assertEquals(1, state.sampleCount)

        // Test conversion: masterTime = 2000 => slaveTime = 2000 - (-5) = 2005
        assertEquals(2005L, syncClock.toLocalMonotonic(2000L))
        assertEquals(1995L, syncClock.toMasterMonotonic(2000L))
    }

    @Test
    fun testSelectsLowestRttSample() {
        // Sample 1: RTT = 100ms, offset = 0ms
        // (1100 - 1000) - 0 = 100ms
        syncClock.recordSample(t1 = 1000L, t2 = 1050L, t3 = 1050L, t4 = 1100L)
        assertEquals(100L, syncClock.clockState.value.roundTripTimeMs)

        // Sample 2 (jittery spike): RTT = 300ms
        // (2300 - 2000) - 0 = 300ms
        syncClock.recordSample(t1 = 2000L, t2 = 2170L, t3 = 2170L, t4 = 2300L)
        // Should STILL retain Sample 1 because 100ms < 300ms
        assertEquals(100L, syncClock.clockState.value.roundTripTimeMs)

        // Sample 3 (better link): RTT = 20ms, offset = 45ms
        // (3020 - 3000) - 0 = 20ms
        // offset = ((3055 - 3000) + (3055 - 3020)) / 2 = (55 + 35) / 2 = 45ms
        syncClock.recordSample(t1 = 3000L, t2 = 3055L, t3 = 3055L, t4 = 3020L)
        // Should now update to Sample 3 with RTT = 20ms
        assertEquals(20L, syncClock.clockState.value.roundTripTimeMs)
        assertEquals(45L, syncClock.clockState.value.offsetMs)
    }

    @Test
    fun testResetClearsSamples() {
        syncClock.recordSample(t1 = 1000L, t2 = 1050L, t3 = 1050L, t4 = 1100L)
        assertEquals(1, syncClock.clockState.value.sampleCount)

        syncClock.reset()
        assertEquals(0, syncClock.clockState.value.sampleCount)
        assertEquals(0L, syncClock.clockState.value.offsetMs)
    }
}
