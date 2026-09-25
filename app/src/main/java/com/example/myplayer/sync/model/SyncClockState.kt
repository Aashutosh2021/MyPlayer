package com.example.myplayer.sync.model

data class SyncClockState(
    val offsetMs: Long = 0L,
    val roundTripTimeMs: Long = 0L,
    val lastSyncTimestampMs: Long = 0L,
    val sampleCount: Int = 0
)
