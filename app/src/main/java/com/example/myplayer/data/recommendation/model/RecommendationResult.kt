package com.example.myplayer.data.recommendation.model

sealed class RecommendationResult {
    data class Success(val songs: List<RecommendationSong>) : RecommendationResult()
    data class Error(val message: String, val throwable: Throwable? = null) : RecommendationResult()

    fun getSongsOrEmpty(): List<RecommendationSong> {
        return when (this) {
            is Success -> songs
            is Error -> emptyList()
        }
    }
}
