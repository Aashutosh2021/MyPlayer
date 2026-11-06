package com.myplayer.app.playback

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaItemFactory @Inject constructor() {
    @OptIn(UnstableApi::class)
    fun createMediaItem(
        songId: String,
        path: String,
        title: String,
        artist: String,
        albumArt: String?
    ): MediaItem {
        val uri = if (path.startsWith("/")) {
            Uri.fromFile(File(path))
        } else {
            Uri.parse(path)
        }

        val cleanCacheKey = songId
            .removePrefix("online://")
            .removePrefix("spotify:track:")
            .removePrefix("spotify:")
            .trim()
            .takeIf { it.isNotBlank() }

        val resolvedArtUri = when {
            !albumArt.isNullOrBlank() && albumArt.startsWith("/") -> Uri.fromFile(File(albumArt))
            !albumArt.isNullOrBlank() -> Uri.parse(albumArt)
            else -> null
        }

        return MediaItem.Builder()
            .setMediaId(songId)
            .setUri(uri)
            .apply {
                // Stable customCacheKey ensures SimpleCache reuses 9-second chunks across retries
                // and app restarts even when googlevideo.com query tokens change!
                if (cleanCacheKey != null && (path.startsWith("http://") || path.startsWith("https://"))) {
                    setCustomCacheKey("yt_audio_$cleanCacheKey")
                }
            }
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .apply {
                        if (resolvedArtUri != null) {
                            setArtworkUri(resolvedArtUri)
                        }
                    }
                    .build()
            )
            .build()
    }
}
