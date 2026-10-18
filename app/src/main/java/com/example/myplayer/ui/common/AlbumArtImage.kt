package com.example.myplayer.ui.common

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
import com.example.myplayer.data.artwork.model.ArtworkModel

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

    // Downsample list thumbnails to exact pixel size (saves 90% RAM & GPU bandwidth)
    val targetPx = remember(size, density) {
        with(density) { (size * 1.75f).roundToPx().coerceIn(120, 900) }
    }

    // For small thumbnails (<100dp), convert YouTube w800-h800 URLs to lightweight w226-h226
    val optimizedUrl = remember(effectiveUri, size) {
        if (effectiveUri.isNullOrBlank()) {
            ""
        } else if (size < 100.dp) {
            effectiveUri
                .replace("=w800-h800-l90-rj", "=w226-h226-l90-rj")
                .replace("w800-h800", "w226-h226")
        } else {
            effectiveUri
        }
    }

    // Support both direct URLs and ArtworkModel for songs needing artwork resolution
    val requestModel: Any? = remember(optimizedUrl, effectiveUri, title, artist, album) {
        if (optimizedUrl.isNotBlank()) {
            optimizedUrl
        } else if (!title.isNullOrBlank() && !artist.isNullOrBlank()) {
            ArtworkModel(title = title, artist = artist, album = album, localUri = effectiveUri)
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
            val request = remember(context, requestModel, targetPx, isLarge) {
                ImageRequest.Builder(context)
                    .data(requestModel)
                    .size(targetPx, targetPx)
                    .precision(Precision.INEXACT)
                    .allowHardware(true)
                    .crossfade(if (isLarge) 180 else 0) // Never crossfade inside fast-scrolling lists!
                    .memoryCacheKey("${requestModel}_$targetPx")
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
