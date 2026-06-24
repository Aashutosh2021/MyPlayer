package com.example.myplayer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloaded_songs")
data class DownloadedSongEntity(
    @PrimaryKey val id: String,          // YouTube videoId
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val durationMs: Long,
    val localPath: String,               // Absolute path to local file
    val fileSizeBytes: Long = 0,
    val downloadedAt: Long = System.currentTimeMillis()
)
