package com.example.myplayer.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options

class AudioAlbumArtFetcher(
    private val data: Uri,
    private val options: Options,
    private val context: Context
) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        // Try Android 10+ ContentResolver thumbnail API first
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            try {
                val bitmap = context.contentResolver.loadThumbnail(data, android.util.Size(512, 512), null)
                return DrawableResult(
                    drawable = android.graphics.drawable.BitmapDrawable(context.resources, bitmap),
                    isSampled = false,
                    dataSource = DataSource.DISK
                )
            } catch (e: Exception) {
                // Ignore and fall back to MediaMetadataRetriever
            }
        }

        // Fallback to MediaMetadataRetriever
        val retriever = MediaMetadataRetriever()
        try {
            context.contentResolver.openFileDescriptor(data, "r")?.use { pfd ->
                retriever.setDataSource(pfd.fileDescriptor)
                val picture = retriever.embeddedPicture
                if (picture != null) {
                    val bitmap = android.graphics.BitmapFactory.decodeByteArray(picture, 0, picture.size)
                    return DrawableResult(
                        drawable = android.graphics.drawable.BitmapDrawable(context.resources, bitmap),
                        isSampled = false,
                        dataSource = DataSource.DISK
                    )
                }
            }
        } catch (e: Exception) {
            // Ignore
        } finally {
            try { retriever.release() } catch (e: Exception) {}
        }
        return null
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (data.scheme == "content") {
                val uriString = data.toString().lowercase()
                if (uriString.contains(".mp3") || uriString.contains(".m4a") || 
                    uriString.contains(".flac") || uriString.contains(".wav") || 
                    uriString.contains(".ogg")) {
                    return AudioAlbumArtFetcher(data, options, context)
                }
            }
            return null
        }
    }
}
