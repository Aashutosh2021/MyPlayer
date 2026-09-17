package com.example.aria.connector

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Standard typed event payloads sent from connected apps back to ARIA.
 */
@Parcelize
sealed class EcosystemEvent : Parcelable {

    @Parcelize
    data class TrackChanged(val title: String, val artist: String) : EcosystemEvent()

    @Parcelize
    data class DownloadComplete(val trackId: String, val track: String) : EcosystemEvent()

    @Parcelize
    data class PlaylistUpdated(val playlistName: String) : EcosystemEvent()

    @Parcelize
    data object FavoriteAdded : EcosystemEvent()

    @Parcelize
    data class PlaybackError(val code: Int, val message: String) : EcosystemEvent()

    @Parcelize
    data object SleepTimerDone : EcosystemEvent()

    @Parcelize
    data object PlaybackStopped : EcosystemEvent()

    @Parcelize
    data object PlaybackPaused : EcosystemEvent()
}
