package com.example.myplayer.data.recommendation.state

data class RecommendationUserState(
    val hasPlaybackHistory: Boolean,
    val hasOnlinePlaybackHistory: Boolean,
    val hasLocalPlaybackHistory: Boolean,
    val hasRecommendationHistory: Boolean = false,
    val totalPlaysCount: Int = 0
) {
    val isColdStart: Boolean get() = !hasPlaybackHistory
}
