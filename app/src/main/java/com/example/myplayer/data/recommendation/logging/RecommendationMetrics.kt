package com.example.myplayer.data.recommendation.logging

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecommendationMetrics @Inject constructor() {
    private val hitCount = AtomicInteger(0)
    private val missCount = AtomicInteger(0)
    private val successCount = AtomicInteger(0)
    private val failureCount = AtomicInteger(0)
    private val totalResponseTimeMs = AtomicLong(0)
    private val fetchCount = AtomicInteger(0)

    fun recordCacheHit() { hitCount.incrementAndGet() }
    
    fun recordCacheMiss() { missCount.incrementAndGet() }
    
    fun recordFetchSuccess(timeTakenMs: Long) {
        successCount.incrementAndGet()
        fetchCount.incrementAndGet()
        totalResponseTimeMs.addAndGet(timeTakenMs)
    }
    
    fun recordFetchFailure() {
        failureCount.incrementAndGet()
        fetchCount.incrementAndGet()
    }

    fun getHitRate(): Double {
        val total = hitCount.get() + missCount.get()
        return if (total == 0) 0.0 else hitCount.get().toDouble() / total.toDouble()
    }

    fun getAverageResponseTime(): Long {
        val success = successCount.get()
        return if (success == 0) 0L else totalResponseTimeMs.get() / success
    }

    fun getSuccessRate(): Double {
        val total = fetchCount.get()
        return if (total == 0) 0.0 else successCount.get().toDouble() / total.toDouble()
    }
}
