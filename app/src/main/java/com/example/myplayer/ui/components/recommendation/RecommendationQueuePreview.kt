package com.example.myplayer.ui.components.recommendation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.animation.animateContentSize
import com.example.myplayer.data.recommendation.model.RecommendationSong
import com.example.myplayer.ui.common.AlbumArtImage
import com.example.myplayer.ui.theme.clayConcave
import com.example.myplayer.ui.theme.claySurface
import com.example.myplayer.ui.theme.ClayPrimary
import com.example.myplayer.ui.theme.OnSurface
import com.example.myplayer.ui.theme.OnSurfaceVariant
import com.example.myplayer.ui.theme.SurfaceLight

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed

@Composable
fun RecommendationQueuePreview(
    recommendations: List<RecommendationSong>,
    modifier: Modifier = Modifier,
    onSongClick: (RecommendationSong) -> Unit = {}
) {
    if (recommendations.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Up next: No recommendations yet",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .claySurface(borderRadius = 24.dp, backgroundColor = SurfaceLight)
            .padding(16.dp)
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Up Next (Autoplay)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = OnSurface
            )
            Text(
                text = "${recommendations.size} songs",
                style = MaterialTheme.typography.labelMedium,
                color = OnSurfaceVariant
            )
        }
        
        Spacer(Modifier.height(16.dp))
        
        LazyColumn(
            modifier = Modifier.heightIn(max = 340.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            userScrollEnabled = false
        ) {
            itemsIndexed(recommendations.take(5), key = { _, song -> song.videoId }) { index, song ->
                QueuePreviewItem(
                    song = song,
                    position = index + 1,
                    onClick = { onSongClick(song) },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

@Composable
private fun QueuePreviewItem(
    song: RecommendationSong,
    position: Int,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = position.toString(),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = ClayPrimary,
            modifier = Modifier.width(24.dp)
        )
        
        Box(
            modifier = Modifier
                .size(44.dp)
                .clayConcave(borderRadius = 12.dp)
                .padding(2.dp)
        ) {
            AlbumArtImage(
                uri = song.thumbnailUrl,
                size = 40.dp,
                shape = RoundedCornerShape(10.dp),
                iconSize = 20.dp
            )
        }
        
        Spacer(Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${song.artist} • ${song.reason}",
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
