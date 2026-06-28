package com.example.myplayer.playback

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackCompletionGuard @Inject constructor() {
    private var lastCompletedMediaId: String? = null

    /**
     * Checks if the completion event for the given media ID has already been handled.
     * Returns true if it was already handled, false if it is a new completion event.
     */
    @Synchronized
    fun isAlreadyHandled(mediaId: String?): Boolean {
        if (mediaId == null) return false // Cannot guard null mediaId, assume new
        
        if (lastCompletedMediaId == mediaId) {
            return true
        }
        
        lastCompletedMediaId = mediaId
        return false
    }

    /**
     * Resets the guard, typically when a new song starts playing.
     */
    @Synchronized
    fun reset() {
        lastCompletedMediaId = null
    }
}
