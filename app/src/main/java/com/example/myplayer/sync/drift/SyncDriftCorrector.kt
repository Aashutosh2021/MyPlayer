package com.example.myplayer.sync.drift

import android.os.SystemClock
import android.util.Log
import com.example.myplayer.playback.MusicController
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Calculates playback position drift between Master and Slave and dynamically applies
 * imperceptible speed shifts (0.96x - 1.04x) or controlled micro-seeks to maintain sync.
 */
@Singleton
class SyncDriftCorrector @Inject constructor() {

    companion object {
        private const val TAG = "SyncDriftCorrector"

        // Below 60ms: imperceptible, normal audio buffer jitter — ignore
        const val DRIFT_IGNORE_THRESHOLD_MS = 60L

        // 60ms - 350ms: moderate drift — adjust playback speed gently
        const val DRIFT_MODERATE_THRESHOLD_MS = 350L

        // Above 350ms: severe drift (e.g. initial buffer delay or seek) — controlled seek
        const val DRIFT_SEEK_THRESHOLD_MS = 350L

        // Minimum delay between forced seeks to prevent audio stutter
        private const val MIN_SEEK_COOLDOWN_MS = 3000L

        const val SPEED_SLOWER = 0.96f
        const val SPEED_FASTER = 1.04f
        const val SPEED_NORMAL = 1.0f
    }

    private var lastSeekTimestamp = 0L
    private var currentAppliedSpeed = SPEED_NORMAL

    fun reset(musicController: MusicController) {
        if (currentAppliedSpeed != SPEED_NORMAL) {
            musicController.setPlaybackSpeed(SPEED_NORMAL)
            currentAppliedSpeed = SPEED_NORMAL
        }
        lastSeekTimestamp = 0L
    }

    /**
     * Evaluates drift and applies correction.
     *
     * @return Measured drift in milliseconds (positive means Slave is ahead, negative means Slave is behind).
     */
    fun correctDrift(
        slavePosMs: Long,
        expectedMasterPosMs: Long,
        musicController: MusicController
    ): Long {
        val driftMs = slavePosMs - expectedMasterPosMs
        val absDrift = abs(driftMs)
        val now = SystemClock.elapsedRealtime()

        when {
            absDrift <= DRIFT_IGNORE_THRESHOLD_MS -> {
                // In sync! Restore normal speed if altered
                if (currentAppliedSpeed != SPEED_NORMAL) {
                    musicController.setPlaybackSpeed(SPEED_NORMAL)
                    currentAppliedSpeed = SPEED_NORMAL
                    Log.d(TAG, "Drift resolved ($driftMs ms) -> restored 1.0x speed")
                }
            }

            absDrift in (DRIFT_IGNORE_THRESHOLD_MS + 1)..DRIFT_MODERATE_THRESHOLD_MS -> {
                // Moderate drift: subtle speed adjustment
                val targetSpeed = if (driftMs > 0) SPEED_SLOWER else SPEED_FASTER
                if (currentAppliedSpeed != targetSpeed) {
                    musicController.setPlaybackSpeed(targetSpeed)
                    currentAppliedSpeed = targetSpeed
                    Log.d(TAG, "Adjusted speed to ${targetSpeed}x for drift: $driftMs ms")
                }
            }

            else -> {
                // Severe drift (> 350ms): discrete seek with cooldown
                if (now - lastSeekTimestamp >= MIN_SEEK_COOLDOWN_MS) {
                    Log.w(TAG, "Severe drift ($driftMs ms) -> seeking to $expectedMasterPosMs ms")
                    musicController.seekTo(expectedMasterPosMs)
                    lastSeekTimestamp = now
                    if (currentAppliedSpeed != SPEED_NORMAL) {
                        musicController.setPlaybackSpeed(SPEED_NORMAL)
                        currentAppliedSpeed = SPEED_NORMAL
                    }
                }
            }
        }

        return driftMs
    }
}
