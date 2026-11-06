package com.myplayer.app.util

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
import java.io.File

class AudioAlbumArtFetcher(
    private val data: Uri,
    private val options: Options,
    private val context: Context
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val targetWidth = options.size.width.pxOrElse { 512 }.coerceIn(96, 1024)
        val targetHeight = options.size.height.pxOrElse { 512 }.coerceIn(96, 1024)

        // 1. Handle local file:// URIs or raw paths (Downloaded songs & local storage)
        if (data.scheme == "file" || data.scheme.isNullOrBlank()) {
            val rawPath = data.path ?: data.toString()
            val localFile = File(rawPath)
            if (localFile.exists()) {
                val lowerName = localFile.name.lowercase()
                // A. Direct image file (.jpg, .png, .webp)
                if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
                    lowerName.endsWith(".png") || lowerName.endsWith(".webp")
                ) {
                    decodeSampledFile(localFile, targetWidth, targetHeight)?.let { return it }
                }

                // B. Companion image next to audio file (<SongName>.jpg or cover.jpg)
                val parent = localFile.parentFile
                if (parent != null) {
                    val companion = File(parent, "${localFile.nameWithoutExtension}.jpg")
                    if (companion.exists() && companion.length() > 512L) {
                        decodeSampledFile(companion, targetWidth, targetHeight)?.let { return it }
                    }
                    val cover = File(parent, "cover.jpg")
                    if (cover.exists() && cover.length() > 512L) {
                        decodeSampledFile(cover, targetWidth, targetHeight)?.let { return it }
                    }
                }

                // C. Embedded ID3 / MP4 picture inside the local audio file
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(localFile.absolutePath)
                    val picture = retriever.embeddedPicture
                    if (picture != null) {
                        decodeSampledBytes(picture, targetWidth, targetHeight)?.let { return it }
                    }
                } catch (_: Exception) {
                } finally {
                    try { retriever.release() } catch (_: Exception) {}
                }
            }
            return null
        }

        // 2. Handle content:// URIs (MediaStore / SAF custom download folder)
        if (data.scheme == "content") {
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
                } catch (_: Exception) {
                    // Fall back to MediaMetadataRetriever
                }
            }

            val retriever = MediaMetadataRetriever()
            try {
                context.contentResolver.openFileDescriptor(data, "r")?.use { pfd ->
                    retriever.setDataSource(pfd.fileDescriptor)
                    val picture = retriever.embeddedPicture
                    if (picture != null) {
                        decodeSampledBytes(picture, targetWidth, targetHeight)?.let { return it }
                    }
                }
            } catch (_: Exception) {
            } finally {
                try { retriever.release() } catch (_: Exception) {}
            }
        }

        return null
    }

    private fun decodeSampledFile(file: File, targetWidth: Int, targetHeight: Int): FetchResult? {
        return try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, boundsOpts)
            var sampleSize = 1
            val halfW = boundsOpts.outWidth / 2
            val halfH = boundsOpts.outHeight / 2
            while (halfW > 0 && halfH > 0 && (halfW / sampleSize) >= targetWidth && (halfH / sampleSize) >= targetHeight) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
            }
            val bitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOpts) ?: return null
            DrawableResult(
                drawable = BitmapDrawable(context.resources, bitmap),
                isSampled = sampleSize > 1,
                dataSource = DataSource.DISK
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeSampledBytes(bytes: ByteArray, targetWidth: Int, targetHeight: Int): FetchResult? {
        return try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOpts)
            var sampleSize = 1
            val halfW = boundsOpts.outWidth / 2
            val halfH = boundsOpts.outHeight / 2
            while (halfW > 0 && halfH > 0 && (halfW / sampleSize) >= targetWidth && (halfH / sampleSize) >= targetHeight) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
            }
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts) ?: return null
            DrawableResult(
                drawable = BitmapDrawable(context.resources, bitmap),
                isSampled = sampleSize > 1,
                dataSource = DataSource.DISK
            )
        } catch (_: Exception) {
            null
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            val scheme = data.scheme
            if (scheme == "content" || scheme == "file" || scheme.isNullOrBlank()) {
                return AudioAlbumArtFetcher(data, options, context)
            }
            return null
        }
    }
}
