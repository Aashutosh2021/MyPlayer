package com.example.myplayer.data.recommendation.queue

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QueueMetrics @Inject constructor() {
    private val queueRefreshes = AtomicInteger(0)
    private val duplicateFilteredCount = AtomicInteger(0)
    private val rejectedSongsCount = AtomicInteger(0)
    private val totalFilteredCount = AtomicInteger(0)
    
    private val totalFillTimeMs = AtomicLong(0)
    private val totalRecommendationCount = AtomicInteger(0)

    fun recordQueueRefresh(fillTimeMs: Long, recommendationsAdded: Int) {
        queueRefreshes.incrementAndGet()
        totalFillTimeMs.addAndGet(fillTimeMs)
        totalRecommendationCount.addAndGet(recommendationsAdded)
    }

    fun recordDuplicateFiltered(count: Int = 1) {
        duplicateFilteredCount.addAndGet(count)
        totalFilteredCount.addAndGet(count)
    }
    
    fun recordRejected(count: Int = 1) {
        rejectedSongsCount.addAndGet(count)
        totalFilteredCount.addAndGet(count)
    }

    fun getAverageFillTime(): Long {
        val count = queueRefreshes.get()
        return if (count == 0) 0L else totalFillTimeMs.get() / count
    }
    
    fun getAverageRecommendationCount(): Double {
        val count = queueRefreshes.get()
        return if (count == 0) 0.0 else totalRecommendationCount.get().toDouble() / count.toDouble()
    }
    
    fun getQueueRefreshCount(): Int = queueRefreshes.get()
    fun getDuplicateFilteredCount(): Int = duplicateFilteredCount.get()
    fun getRejectedSongsCount(): Int = rejectedSongsCount.get()
    fun getTotalFilteredCount(): Int = totalFilteredCount.get()
}
