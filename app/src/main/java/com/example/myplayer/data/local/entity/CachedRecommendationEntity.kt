package com.example.myplayer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_recommendations")
data class CachedRecommendationEntity(
    @PrimaryKey
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val durationMs: Long,
    val source: String,
    /** The seed videoId this recommendation belongs to (or "cold_start") */
    val seedId: String,
    val cachedAt: Long = System.currentTimeMillis()
)
