package com.example.myplayer.playback

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SleepTimerManager @Inject constructor() {
    private val _sleepTimerRemainingSeconds = MutableStateFlow<Long>(-1L)
    val sleepTimerRemainingSeconds: StateFlow<Long> = _sleepTimerRemainingSeconds.asStateFlow()

    private var sleepTimerJob: Job? = null

    fun startSleepTimer(scope: CoroutineScope, minutes: Int, onTimerEnd: () -> Unit) {
        sleepTimerJob?.cancel()
        val totalSeconds = minutes * 60L
        _sleepTimerRemainingSeconds.value = totalSeconds
        sleepTimerJob = scope.launch {
            var remaining = totalSeconds
            while (remaining > 0 && isActive) {
                delay(1000L)
                remaining--
                _sleepTimerRemainingSeconds.value = remaining
            }
            if (isActive) {
                onTimerEnd()
                _sleepTimerRemainingSeconds.value = -1L
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerRemainingSeconds.value = -1L
    }
}
