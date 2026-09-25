package com.example.myplayer.sync.clock

import android.os.SystemClock
import android.util.Log
import com.example.myplayer.sync.model.SyncClockState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-precision clock synchronizer utilizing Cristian's / NTP algorithm.
 * Operates on SystemClock.elapsedRealtime() (monotonic clock) to be immune to
 * user wall-clock modifications and time zone adjustments.
 */
@Singleton
class SyncClock @Inject constructor() {

    companion object {
        private const val TAG = "SyncClock"
        private const val MAX_SAMPLES = 5
    }

    private data class Sample(
        val roundTripTimeMs: Long,
        val offsetMs: Long,
        val timestamp: Long
    )

    private val samples = mutableListOf<Sample>()
    private val _clockState = MutableStateFlow(SyncClockState())
    val clockState: StateFlow<SyncClockState> = _clockState.asStateFlow()

    /**
     * Current local monotonic timestamp in milliseconds.
     */
    fun localMonotonicNow(): Long = SystemClock.elapsedRealtime()

    /**
     * Resets clock state when a new session starts.
     */
    fun reset() {
        synchronized(samples) {
            samples.clear()
            _clockState.value = SyncClockState()
        }
    }

    /**
     * Records a Ping-Pong round-trip measurement and updates the estimated offset.
     *
     * @param t1 Slave time when Ping was sent
     * @param t2 Master time when Ping was received
     * @param t3 Master time when Pong was sent
     * @param t4 Slave time when Pong was received
     */
    fun recordSample(t1: Long, t2: Long, t3: Long, t4: Long) {
        val rtt = (t4 - t1) - (t3 - t2)
        if (rtt < 0) {
            Log.w(TAG, "Negative RTT measured ($rtt ms), discarded")
            return
        }

        // offset = MasterTime - SlaveTime
        val offset = ((t2 - t1) + (t3 - t4)) / 2

        synchronized(samples) {
            if (samples.size >= MAX_SAMPLES) {
                samples.removeAt(0)
            }
            samples.add(Sample(roundTripTimeMs = rtt, offsetMs = offset, timestamp = t4))

            // Select best sample (lowest RTT contains least asymmetric network delay)
            val bestSample = samples.minByOrNull { it.roundTripTimeMs } ?: return
            _clockState.value = SyncClockState(
                offsetMs = bestSample.offsetMs,
                roundTripTimeMs = bestSample.roundTripTimeMs,
                lastSyncTimestampMs = bestSample.timestamp,
                sampleCount = samples.size
            )
            Log.d(TAG, "Clock synced: offset=${bestSample.offsetMs}ms RTT=${bestSample.roundTripTimeMs}ms samples=${samples.size}")
        }
    }

    /**
     * Converts a Master monotonic timestamp to the equivalent local Slave monotonic timestamp.
     */
    fun toLocalMonotonic(masterMonotonicTimestamp: Long): Long {
        // masterTime = slaveTime + offset  =>  slaveTime = masterTime - offset
        return masterMonotonicTimestamp - _clockState.value.offsetMs
    }

    /**
     * Converts a local Slave monotonic timestamp to the equivalent Master monotonic timestamp.
     */
    fun toMasterMonotonic(slaveMonotonicTimestamp: Long): Long {
        return slaveMonotonicTimestamp + _clockState.value.offsetMs
    }
}
