package com.example.myplayer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_history")
data class RecentHistoryEntity(
    @PrimaryKey val songId: String,
    val playedAt: Long = System.currentTimeMillis()
)
