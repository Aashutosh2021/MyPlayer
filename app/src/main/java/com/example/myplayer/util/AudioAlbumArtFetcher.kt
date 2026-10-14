package com.example.myplayer.util

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import coil.size.pxOrElse

class AudioAlbumArtFetcher(
    private val data: Uri,
    private val options: Options,
    private val context: Context
) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        val targetWidth = options.size.width.pxOrElse { 256 }.coerceIn(96, 1024)
        val targetHeight = options.size.height.pxOrElse { 256 }.coerceIn(96, 1024)

        // Try Android 10+ ContentResolver thumbnail API first with exact requested size
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val bitmap = context.contentResolver.loadThumbnail(
                    data,
                    Size(targetWidth, targetHeight),
                    null
                )
                return DrawableResult(
                    drawable = BitmapDrawable(context.resources, bitmap),
                    isSampled = targetWidth < 1024,
                    dataSource = DataSource.DISK
                )
            } catch (e: Exception) {
                // Fall back to MediaMetadataRetriever
            }
        }

        val retriever = MediaMetadataRetriever()
        try {
            context.contentResolver.openFileDescriptor(data, "r")?.use { pfd ->
                retriever.setDataSource(pfd.fileDescriptor)
                val picture = retriever.embeddedPicture
                if (picture != null) {
                    val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(picture, 0, picture.size, boundsOpts)

                    var sampleSize = 1
                    val halfW = boundsOpts.outWidth / 2
                    val halfH = boundsOpts.outHeight / 2
                    while ((halfW / sampleSize) >= targetWidth && (halfH / sampleSize) >= targetHeight) {
                        sampleSize *= 2
                    }

                    val decodeOpts = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
                    }
                    val bitmap = BitmapFactory.decodeByteArray(picture, 0, picture.size, decodeOpts)
                    if (bitmap != null) {
                        return DrawableResult(
                            drawable = BitmapDrawable(context.resources, bitmap),
                            isSampled = sampleSize > 1,
                            dataSource = DataSource.DISK
                        )
                    }
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
                return AudioAlbumArtFetcher(data, options, context)
            }
            return null
        }
    }
}

