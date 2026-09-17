package com.example.myplayer.data.artwork.model

/**
 * Request model passed to Coil or ArtworkRepository to resolve high-quality artwork.
 *
 * @param title The song title.
 * @param artist The song artist name.
 * @param album Optional album name.
 * @param localUri Optional fallback URI (e.g. content:// or low-res thumbnail).
 */
data class ArtworkModel(
    val title: String?,
    val artist: String?,
    val album: String? = null,
    val localUri: String? = null
)
