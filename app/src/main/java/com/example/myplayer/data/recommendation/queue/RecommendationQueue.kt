package com.example.myplayer.data.recommendation.queue

import com.example.myplayer.data.recommendation.model.RecommendationSong
import java.util.PriorityQueue
import javax.inject.Inject
import javax.inject.Singleton

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class RecommendationQueue @Inject constructor() {

    private val _queueState = MutableStateFlow<List<RecommendationSong>>(emptyList())
    val queueState: StateFlow<List<RecommendationSong>> = _queueState.asStateFlow()

    private var lastUpdated: Long = System.currentTimeMillis()

    // Sort by: recommendationScore (descending), then popularityScore (descending)
    private val queue = PriorityQueue<RecommendationSong> { a, b ->
        val recScoreDiff = b.recommendationScore.compareTo(a.recommendationScore)
        if (recScoreDiff != 0) {
            recScoreDiff
        } else {
            b.popularityScore.compareTo(a.popularityScore)
        }
    }

    @Synchronized
    fun enqueue(songs: List<RecommendationSong>) {
        queue.addAll(songs)
        lastUpdated = System.currentTimeMillis()
        _queueState.value = getSnapshot()
    }

    @Synchronized
    fun enqueue(song: RecommendationSong) {
        queue.add(song)
        lastUpdated = System.currentTimeMillis()
        _queueState.value = getSnapshot()
    }

    @Synchronized
    fun dequeue(): RecommendationSong? {
        val result = queue.poll()
        if (result != null) {
            _queueState.value = getSnapshot()
        }
        return result
    }

    @Synchronized
    fun peek(): RecommendationSong? {
        return queue.peek()
    }
    
    @Synchronized
    fun peekNext(count: Int): List<RecommendationSong> {
        val result = mutableListOf<RecommendationSong>()
        val tempQueue = PriorityQueue(queue)
        for (i in 0 until count) {
            val song = tempQueue.poll() ?: break
            result.add(song)
        }
        return result
    }

    @Synchronized
    fun clear() {
        queue.clear()
        lastUpdated = System.currentTimeMillis()
        _queueState.value = getSnapshot()
    }

    @Synchronized
    fun size(): Int {
        return queue.size
    }

    @Synchronized
    fun isEmpty(): Boolean {
        return queue.isEmpty()
    }

    @Synchronized
    fun contains(videoId: String): Boolean {
        return queue.any { it.videoId == videoId }
    }

    @Synchronized
    fun remove(videoId: String): Boolean {
        val iterator = queue.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().videoId == videoId) {
                iterator.remove()
                _queueState.value = getSnapshot()
                return true
            }
        }
        return false
    }

    @Synchronized
    fun getQueuedIds(): Set<String> {
        return queue.map { it.videoId }.toSet()
    }
    
    @Synchronized
    fun getSnapshot(): List<RecommendationSong> {
        return queue.toList().sortedWith(queue.comparator())
    }
    
    @Synchronized
    fun isExpired(ttlMillis: Long): Boolean {
        return System.currentTimeMillis() - lastUpdated > ttlMillis
    }
}
