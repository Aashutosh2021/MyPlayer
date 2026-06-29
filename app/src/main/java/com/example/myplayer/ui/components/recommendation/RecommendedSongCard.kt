package com.example.myplayer.ui.components.recommendation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myplayer.data.recommendation.model.RecommendationSong
import com.example.myplayer.ui.common.AlbumArtImage
import com.example.myplayer.ui.components.ClayIconButton
import com.example.myplayer.ui.theme.clayConcave
import com.example.myplayer.ui.theme.claySurface
import com.example.myplayer.ui.theme.ClayPrimary
import com.example.myplayer.ui.theme.OnPrimary
import com.example.myplayer.ui.theme.OnSurface
import com.example.myplayer.ui.theme.OnSurfaceVariant
import com.example.myplayer.ui.theme.SurfaceLight
import com.example.myplayer.ui.theme.SurfaceContainerLow

@Composable
fun RecommendedSongCard(
    song: RecommendationSong,
    onPlayClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(180.dp)
            .claySurface(
                borderRadius = 28.dp,
                backgroundColor = SurfaceLight
            )
            .clickable(onClick = onPlayClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(148.dp)
                    .clayConcave(borderRadius = 20.dp, backgroundColor = SurfaceContainerLow)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                AlbumArtImage(
                    uri = song.thumbnailUrl,
                    size = 140.dp,
                    shape = RoundedCornerShape(16.dp),
                    iconSize = 48.dp
                )
                
                // Play button overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(36.dp)
                        .claySurface(borderRadius = 18.dp, backgroundColor = ClayPrimary)
                        .clickable(onClick = onPlayClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.PlayArrow, null, tint = OnPrimary, modifier = Modifier.size(20.dp))
                }
            }
            
            Spacer(Modifier.height(14.dp))
            
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            
            Spacer(Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RecommendationReasonChip(reason = song.reason)
                ClayIconButton(onClick = onMoreClick, size = 32.dp) {
                    Icon(Icons.Filled.MoreVert, null, tint = OnSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
