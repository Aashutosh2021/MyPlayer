package com.example.myplayer.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myplayer.data.local.entity.SongEntity
import com.example.myplayer.ui.common.AlbumArtImage
import com.example.myplayer.ui.theme.*

@Composable
fun MiniPlayer(
    song: SongEntity,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPlayerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .claySurface(
                    borderRadius = 20.dp,
                    backgroundColor = ClayPrimary,
                    innerLightColor = Color.White.copy(alpha = 0.3f),
                    innerDarkColor = Color.Black.copy(alpha = 0.2f),
                    outerShadowColor = ClayShadowOuter
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onPlayerClick
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album art (sunken well)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clayConcave(borderRadius = 12.dp, backgroundColor = SurfaceLight)
                    .padding(2.dp)
            ) {
                AlbumArtImage(
                    uri = song.albumArt,
                    size = 44.dp,
                    shape = RoundedCornerShape(10.dp),
                    iconSize = 20.dp
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = OnPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Play/Pause button (raised lozenge inside primary container)
            val playPauseInteraction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .claySurface(
                        borderRadius = 20.dp,
                        backgroundColor = PrimaryContainer,
                        innerLightColor = Color.White.copy(alpha = 0.4f),
                        innerDarkColor = Color.Black.copy(alpha = 0.2f),
                        outerShadowColor = Color.Transparent,
                        interactionSource = playPauseInteraction
                    )
                    .clickable(
                        interactionSource = playPauseInteraction,
                        indication = null,
                        onClick = onPlayPauseClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = OnPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(8.dp))

            // Next button (raised secondary button)
            val nextInteraction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .claySurface(
                        borderRadius = 18.dp,
                        backgroundColor = ClayPrimary,
                        innerLightColor = Color.White.copy(alpha = 0.2f),
                        innerDarkColor = Color.Black.copy(alpha = 0.2f),
                        outerShadowColor = Color.Transparent,
                        interactionSource = nextInteraction
                    )
                    .clickable(
                        interactionSource = nextInteraction,
                        indication = null,
                        onClick = onNextClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.SkipNext,
                    contentDescription = "Next",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
