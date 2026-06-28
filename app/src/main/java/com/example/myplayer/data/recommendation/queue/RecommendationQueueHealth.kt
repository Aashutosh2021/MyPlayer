package com.example.myplayer.data.recommendation.queue

import com.example.myplayer.data.recommendation.logging.RecommendationLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecommendationQueueHealth @Inject constructor(
    private val logger: RecommendationLogger
) {
    private var emptyQueueCount = 0
    private var refillCount = 0
    private var totalQueueSizeRecorded = 0
    private var sizeRecordingsCount = 0
    private var expiredEntriesCount = 0
    private var duplicateEntriesDetected = 0

    @Synchronized
    fun recordEmptyQueue() {
        emptyQueueCount++
        emitHealthReport()
    }

    @Synchronized
    fun recordRefill() {
        refillCount++
    }

    @Synchronized
    fun recordQueueSize(size: Int) {
        totalQueueSizeRecorded += size
        sizeRecordingsCount++
    }

    @Synchronized
    fun recordExpiredEntries(count: Int) {
        expiredEntriesCount += count
    }

    @Synchronized
    fun recordDuplicateEntry() {
        duplicateEntriesDetected++
    }

    @Synchronized
    fun getAverageQueueSize(): Double {
        if (sizeRecordingsCount == 0) return 0.0
        return totalQueueSizeRecorded.toDouble() / sizeRecordingsCount
    }

    private fun emitHealthReport() {
        logger.logEvent("QueueHealthReport", mapOf(
            "emptyQueueCount" to emptyQueueCount,
            "refillCount" to refillCount,
            "averageQueueSize" to getAverageQueueSize(),
            "expiredEntriesCount" to expiredEntriesCount,
            "duplicateEntriesDetected" to duplicateEntriesDetected
        ))
    }
}
