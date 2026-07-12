package com.example.myplayer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_lyrics")
data class CachedLyricsEntity(
    @PrimaryKey val songId: String,
    val plainLyrics: String?,
    val syncedLyrics: String?,
    val trackName: String,
    val artistName: String,
    val cachedAt: Long = System.currentTimeMillis()
)
