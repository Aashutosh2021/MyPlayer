package com.example.myplayer.data.artwork.provider

import com.example.myplayer.data.artwork.model.ArtworkResult

/**
 * Interface contract for external and local artwork providers.
 */
interface ArtworkProvider {
    /** Unique display/log name of this provider. */
    val name: String

    /** Priority in the fallback chain (lower number = tried earlier). */
    val priority: Int

    /**
     * Resolves high-resolution artwork for the given song/album metadata.
     *
     * @param title The track title.
     * @param artist The artist name.
     * @param album Optional album name.
     * @return [ArtworkResult] containing the image URL if found, or null if not found or rejected.
     */
    suspend fun fetchArtwork(
        title: String,
        artist: String,
        album: String? = null
    ): ArtworkResult?
}
