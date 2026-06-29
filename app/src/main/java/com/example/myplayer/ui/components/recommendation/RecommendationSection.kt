package com.example.myplayer.ui.components.recommendation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myplayer.data.recommendation.model.RecommendationSong
import com.example.myplayer.ui.components.PremiumSectionHeader

@Composable
fun RecommendationSection(
    title: String,
    recommendations: List<RecommendationSong>,
    isLoading: Boolean,
    onPlayClick: (RecommendationSong) -> Unit,
    onMoreClick: (RecommendationSong) -> Unit,
    onRefreshClick: () -> Unit
) {
    if (recommendations.isEmpty() && !isLoading) {
        // We only show the section if it has items or is loading.
        // Wait, the prompt says "If no recommendations exist: Display: No recommendations available. Continue listening to generate recommendations. Provide Refresh button."
        Column {
            PremiumSectionHeader(title = title, actionLabel = "Refresh", onAction = onRefreshClick)
            RecommendationEmptyState(onRefreshClick = onRefreshClick)
        }
        return
    }

    Column {
        PremiumSectionHeader(title = title, actionLabel = "Refresh", onAction = onRefreshClick)
        
        if (isLoading) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(3) {
                    RecommendationLoadingCard()
                }
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(recommendations.take(10), key = { it.videoId }) { song ->
                    RecommendedSongCard(
                        song = song,
                        onPlayClick = { onPlayClick(song) },
                        onMoreClick = { onMoreClick(song) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
