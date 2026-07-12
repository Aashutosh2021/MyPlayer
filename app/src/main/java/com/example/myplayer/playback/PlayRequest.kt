package com.example.myplayer.playback

enum class PlaybackSourceType {
    LOCAL,
    DOWNLOADED,
    ONLINE,
    PLAYLIST,
    RECOMMENDATION,
    QUEUE,
    FAVORITES,
    RECENT
}

data class PlayRequest(
    val songId: String,
    val title: String,
    val artist: String,
    val playbackSource: PlaybackSourceType,
    val localUri: String? = null,
    val streamUrl: String? = null,
    val playlistId: Long? = null,
    val queueId: String? = null,
    val albumArt: String? = null,
    val metadata: Map<String, String> = emptyMap()
)
