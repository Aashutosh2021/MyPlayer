package com.example.myplayer.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "played_songs_history",
    indices = [Index(value = ["playedAt"])]
)
data class PlayedSongHistoryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
    val durationMs: Long = 0L,
    val isOnline: Boolean = true,
    val playedAt: Long = System.currentTimeMillis()
)
