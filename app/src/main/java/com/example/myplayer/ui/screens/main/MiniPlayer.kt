package com.example.myplayer.ui.screens.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
    onPreviousClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var swipeOffsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(
        targetValue = swipeOffsetX,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "miniSwipe"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 14.dp, shape = RoundedCornerShape(26.dp), spotColor = Color(0x33000000))
                .clip(RoundedCornerShape(26.dp))
                .background(CharcoalPrimary)
                .graphicsLayer {
                    translationX = animatedOffsetX * 0.35f
                    rotationZ = (animatedOffsetX / 60f).coerceIn(-3f, 3f)
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            swipeOffsetX = (swipeOffsetX + dragAmount).coerceIn(-220f, 220f)
                        },
                        onDragEnd = {
                            if (swipeOffsetX < -100f) {
                                onNextClick()
                            } else if (swipeOffsetX > 100f) {
                                onPreviousClick()
                            }
                            swipeOffsetX = 0f
                        },
                        onDragCancel = { swipeOffsetX = 0f }
                    )
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onPlayerClick
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Squircle Album Art
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF262C38)),
                contentAlignment = Alignment.Center
            ) {
                AlbumArtImage(
                    uri = song.albumArt,
                    title = song.title,
                    artist = song.artist,
                    size = 46.dp,
                    shape = RoundedCornerShape(14.dp),
                    iconSize = 22.dp
                )
            }

            Spacer(Modifier.width(14.dp))

            // Title and Artist
            AnimatedContent(
                targetState = song.title to song.artist,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    (slideInHorizontally { it / 2 } + fadeIn(tween(200)))
                        .togetherWith(slideOutHorizontally { -it / 2 } + fadeOut(tween(150)))
                },
                label = "miniSongTitleAnim"
            ) { (title, artist) ->
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = PureWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = LightSlateMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            // Solid White Play/Pause Button with Dark Icon (matches design image 1)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(PureWhite)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onPlayPauseClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = CharcoalPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
