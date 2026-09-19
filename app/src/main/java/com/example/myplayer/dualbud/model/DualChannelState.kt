package com.example.myplayer.dualbud.model

import com.example.myplayer.data.local.entity.SongEntity

/**
 * Identifier for the earbud channel.
 */
enum class DualChannelId {
    LEFT,
    RIGHT
}

/**
 * State representing one independent earbud channel.
 */
data class DualChannelState(
    val channelId: DualChannelId,
    val song: SongEntity? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val isEnded: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val volume: Float = 1.0f,
    val errorMessage: String? = null
)

/**
 * Overall state of the Dual Bud / Split Channel Playback subsystem.
 */
data class DualBudModeState(
    val isEnabled: Boolean = false,
    val isSwapped: Boolean = false,
    val leftChannel: DualChannelState = DualChannelState(DualChannelId.LEFT),
    val rightChannel: DualChannelState = DualChannelState(DualChannelId.RIGHT)
) {
    /** The song actively routed to the physical LEFT earbud (respects [isSwapped]). */
    val physicalLeftSong: SongEntity?
        get() = if (isSwapped) rightChannel.song else leftChannel.song

    /** The song actively routed to the physical RIGHT earbud (respects [isSwapped]). */
    val physicalRightSong: SongEntity?
        get() = if (isSwapped) leftChannel.song else rightChannel.song
}
