package com.myplayer.app.ui.common

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import com.myplayer.app.data.artwork.model.ArtworkModel
import java.io.File

@Composable
fun AlbumArtImage(
    uri: String? = null,
    artUri: String? = uri,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    title: String? = null,
    artist: String? = null,
    album: String? = null,
    size: Dp = 48.dp,
    cornerRadius: Dp = 12.dp,
    shape: Shape = RoundedCornerShape(cornerRadius),
    iconSize: Dp = 24.dp
) {
    val effectiveUri = artUri ?: uri
    val context = LocalContext.current
    val density = LocalDensity.current

    val targetPx = remember(size, density) {
        with(density) { (size * 1.75f).roundToPx().coerceIn(120, 900) }
    }

    // Resolve local downloaded artwork file if effectiveUri is a YouTube URL or local file path
    val resolvedUri = remember(effectiveUri, context) {
        if (effectiveUri.isNullOrBlank()) {
            ""
        } else if (effectiveUri.startsWith("/")) {
            Uri.fromFile(File(effectiveUri)).toString()
        } else {
            val ytId = Regex("""/vi/([a-zA-Z0-9_-]{11})/""").find(effectiveUri)?.groupValues?.getOrNull(1)
            if (ytId != null) {
                val localFile = File(File(context.filesDir, "artwork"), "$ytId.jpg")
                if (localFile.exists() && localFile.length() > 512L) {
                    Uri.fromFile(localFile).toString()
                } else {
                    effectiveUri
                }
            } else {
                effectiveUri
            }
        }
    }

    // Keep consistent URL so Coil disk cache hits both in list view and NowPlaying screen offline!
    val requestModel: Any? = remember(resolvedUri, title, artist, album) {
        if (resolvedUri.isNotBlank() && (resolvedUri.startsWith("file://") || resolvedUri.startsWith("content://"))) {
            Uri.parse(resolvedUri)
        } else if (!title.isNullOrBlank() || !artist.isNullOrBlank()) {
            ArtworkModel(title = title, artist = artist, album = album, localUri = resolvedUri.ifBlank { null })
        } else if (resolvedUri.isNotBlank()) {
            resolvedUri
        } else {
            null
        }
    }

    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val iconTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(surfaceColor),
        contentAlignment = Alignment.Center
    ) {
        if (requestModel != null) {
            val isLarge = size >= 120.dp
            val stableCacheKey = remember(resolvedUri, title, artist) {
                resolvedUri.ifBlank { "${artist.orEmpty()}_${title.orEmpty()}" }
            }
            val request = remember(context, requestModel, targetPx, isLarge, stableCacheKey) {
                ImageRequest.Builder(context)
                    .data(requestModel)
                    .size(targetPx, targetPx)
                    .precision(Precision.INEXACT)
                    .allowHardware(true)
                    .crossfade(if (isLarge) 180 else 0)
                    .diskCacheKey(stableCacheKey)
                    .memoryCacheKey("${stableCacheKey}_$targetPx")
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
            }
            val fallbackPainter = rememberVectorPainter(Icons.Default.MusicNote)

            AsyncImage(
                model = request,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                error = fallbackPainter,
                fallback = fallbackPainter,
                modifier = Modifier.matchParentSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = iconTint
            )
        }
    }
}
