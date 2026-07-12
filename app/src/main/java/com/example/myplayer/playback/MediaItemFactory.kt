package com.example.myplayer.playback

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaItemFactory @Inject constructor() {
    fun createMediaItem(songId: String, path: String, title: String, artist: String, albumArt: String?): MediaItem {
        return MediaItem.Builder()
            .setMediaId(songId)
            .setUri(Uri.parse(path))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .apply {
                        if (!albumArt.isNullOrBlank()) {
                            setArtworkUri(Uri.parse(albumArt))
                        }
                    }
                    .build()
            )
            .build()
    }
}
