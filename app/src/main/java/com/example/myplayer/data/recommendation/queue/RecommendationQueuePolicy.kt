package com.example.myplayer.data.recommendation.queue

import javax.inject.Inject
import javax.inject.Singleton

interface RecommendationQueuePolicy {
    fun shouldRefill(currentSize: Int): Boolean
    val targetSize: Int
}

@Singleton
class DefaultQueuePolicy @Inject constructor() : RecommendationQueuePolicy {
    override val targetSize: Int = QueueConstants.DEFAULT_MIN_QUEUE_SIZE

    override fun shouldRefill(currentSize: Int): Boolean {
        return currentSize < targetSize
    }
}
