package com.example.myplayer.ui.common

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Image

/**
 * Loads embedded album art from an audio file URI (SAF content URI).
 * Uses MediaMetadataRetriever to extract the embedded picture bytes on IO dispatcher.
 * Falls back to a placeholder icon if no art is found.
 */
@Composable
fun AlbumArtImage(
    uri: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    shape: Shape = RoundedCornerShape(8.dp),
    iconSize: Dp = 24.dp
) {
    val context = LocalContext.current
    val isHttp = uri?.startsWith("http") == true

    // Only attempt to extract metadata if it's not an HTTP URL
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = uri) {
        value = null
        if (uri.isNullOrBlank() || isHttp) return@produceState
        value = withContext(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, Uri.parse(uri))
                val bytes = retriever.embeddedPicture
                retriever.release()
                if (bytes != null && bytes.isNotEmpty()) {
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (isHttp && !uri.isNullOrBlank()) {
            coil.compose.AsyncImage(
                model = uri,
                contentDescription = "Album art",
                modifier = Modifier
                    .size(size)
                    .clip(shape),
                contentScale = ContentScale.Crop
            )
        } else if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Album art",
                modifier = Modifier
                    .size(size)
                    .clip(shape),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.size(iconSize)
            )
        }
    }
}
