package com.example.myplayer.data.artwork.model

/**
 * Represents a high-quality resolved artwork item.
 *
 * @param url The direct HTTP/HTTPS URL or content URI to the high-resolution artwork image.
 * @param provider The name of the source provider that resolved this artwork (e.g. "Deezer", "iTunes", "YouTubeMusic", "Local").
 * @param width The nominal pixel width of the image (e.g. 1000).
 * @param height The nominal pixel height of the image (e.g. 1000).
 */
data class ArtworkResult(
    val url: String,
    val provider: String,
    val width: Int = 0,
    val height: Int = 0
)
