package com.example.myplayer.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"],
    indices = [Index(value = ["songId"])]
)
data class PlaylistSongCrossReference(
    val playlistId: Long,
    val songId: String,
    val position: Int
)
