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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.CachePolicy
import androidx.compose.runtime.remember

import com.example.myplayer.data.artwork.model.ArtworkModel

/**
 * Loads embedded album art or resolves high-resolution online artwork using [ArtworkModel].
 * Offloads loading and caching to Coil and the dedicated Artwork Engine.
 */
@Composable
fun AlbumArtImage(
    uri: String?,
    modifier: Modifier = Modifier,
    title: String? = null,
    artist: String? = null,
    album: String? = null,
    size: Dp = 48.dp,
    shape: Shape = RoundedCornerShape(8.dp),
    iconSize: Dp = 24.dp
) {
    val context = LocalContext.current
    val hasValidRequest = !uri.isNullOrBlank() || (!title.isNullOrBlank() && !artist.isNullOrBlank())

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (hasValidRequest) {
            val requestModel: Any = remember(uri, title, artist, album) {
                if (!title.isNullOrBlank() && !artist.isNullOrBlank()) {
                    ArtworkModel(title = title, artist = artist, album = album, localUri = uri)
                } else {
                    uri ?: ""
                }
            }

            val imageRequest = remember(context, requestModel) {
                ImageRequest.Builder(context)
                    .data(requestModel)
                    .crossfade(true)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
            }
            val errorRequest = remember(context) {
                ImageRequest.Builder(context).data("").build()
            }
            AsyncImage(
                model = imageRequest,
                contentDescription = "Album art",
                modifier = Modifier
                    .size(size)
                    .clip(shape),
                contentScale = ContentScale.Crop,
                error = coil.compose.rememberAsyncImagePainter(
                    model = errorRequest,
                    fallback = coil.compose.rememberAsyncImagePainter(model = null)
                )
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
