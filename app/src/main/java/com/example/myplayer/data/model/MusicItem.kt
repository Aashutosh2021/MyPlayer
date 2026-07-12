package com.example.myplayer.data.model

/**
 * Canonical immutable domain model representing a music track.
 *
 * This is the single source of truth for music identity across all modules.
 * Every module (Library, Downloads, Favorites, Playlists, Recommendations)
 * maps its internal model to MusicItem before passing it to shared logic.
 *
 * Introduced in Phase R7 — Data Model Normalization.
 */
data class MusicItem(
    /** Stable identity key. For online/downloaded songs this is the YouTube videoId.
     *  For pure local scanned songs this is the file URI string. */
    val id: String,

    /** YouTube video ID. Null for pure local files that have no online counterpart. */
    val videoId: String?,

    val title: String,
    val artist: String,
    val album: String,

    /** Duration in milliseconds. Unified across all sources. */
    val durationMs: Long,

    /** Unified artwork URI — album art path for local, thumbnail URL for online/downloaded. */
    val artworkUri: String?,

    /** Indicates how this song should be played. */
    val playbackType: PlaybackType,

    /** File URI for LOCAL and DOWNLOADED songs. Null for pure ONLINE songs. */
    val localUri: String?,

    /** Current download state. */
    val downloadStatus: DownloadStatus = DownloadStatus.NOT_DOWNLOADED,

    /** Whether the user has marked this song as a favorite. */
    val isFavorite: Boolean = false,

    /** Arbitrary extra metadata (source, reason, score, etc.) */
    val metadata: Map<String, String> = emptyMap()
)

enum class PlaybackType {
    /** Scanned from a local folder on device storage. */
    LOCAL,
    /** Downloaded from YouTube and stored on device. */
    DOWNLOADED,
    /** Streamed live from YouTube Music — requires network. */
    ONLINE,
    /** Sourced from the autoplay recommendation engine. */
    RECOMMENDATION
}

enum class DownloadStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED
}
