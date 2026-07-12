package com.example.myplayer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val path: String,
    val albumArt: String?,
    val dateAdded: Long,
    val playCount: Int = 0,
    /** YouTube videoId — populated when this local song corresponds to a downloaded/online track.
     *  Null for pure local files scanned from device storage. Added in DB version 3. */
    val videoId: String? = null
)

