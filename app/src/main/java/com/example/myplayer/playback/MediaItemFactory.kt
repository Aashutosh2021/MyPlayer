package com.example.myplayer.playback

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaItemFactory @Inject constructor() {
    fun createMediaItem(songId: String, path: String, title: String, artist: String, albumArt: String?): MediaItem {
        val uri = if (path.startsWith("/")) {
            Uri.fromFile(java.io.File(path))
        } else {
            Uri.parse(path)
        }
        return MediaItem.Builder()
            .setMediaId(songId)
            .setUri(uri)
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
