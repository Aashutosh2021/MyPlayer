package com.example.myplayer.aria.performance

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AriaPerformanceMonitor @Inject constructor() {
    private val statsMap = ConcurrentHashMap<String, PerformanceStats>()

    class PerformanceStats {
        private val totalDurationMs = AtomicLong(0)
        private val sampleCount = AtomicLong(0)

        fun record(durationMs: Long) {
            totalDurationMs.addAndGet(durationMs)
            sampleCount.incrementAndGet()
        }

        fun getAverageMs(): Double {
            val count = sampleCount.get()
            return if (count == 0L) 0.0 else totalDurationMs.get().toDouble() / count
        }

        fun getCount(): Long = sampleCount.get()
    }

    fun recordLatency(category: String, durationMs: Long) {
        statsMap.getOrPut(category) { PerformanceStats() }.record(durationMs)
    }

    fun getAverageLatency(category: String): Double {
        return statsMap[category]?.getAverageMs() ?: 0.0
    }

    fun getSampleCount(category: String): Long {
        return statsMap[category]?.getCount() ?: 0L
    }

    fun clearStats() {
        statsMap.clear()
    }
}
