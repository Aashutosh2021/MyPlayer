package com.example.myplayer.ui.screens.recommendation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myplayer.data.recommendation.RecommendationCoordinator
import com.example.myplayer.data.recommendation.model.RecommendationSong
import com.example.myplayer.data.recommendation.queue.RecommendationQueueManager
import com.example.myplayer.playback.MusicController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecommendationViewModel @Inject constructor(
    private val coordinator: RecommendationCoordinator,
    private val musicController: MusicController
) : ViewModel() {

    val recommendations: StateFlow<List<RecommendationSong>> = coordinator.queueState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun playNow(song: RecommendationSong) {
        viewModelScope.launch {
            val onlineSong = coordinator.playRecommendation(song)
            if (onlineSong != null) {
                musicController.playOnlineSong(onlineSong)
            }
        }
    }

    fun hideRecommendation(song: RecommendationSong) {
        coordinator.rejectRecommendation(song.videoId)
    }

    fun refreshRecommendations() {
        _isLoading.value = true
        coordinator.refreshQueue()
        viewModelScope.launch {
            delay(1000) // fake loading UX
            _isLoading.value = false
        }
    }
}
