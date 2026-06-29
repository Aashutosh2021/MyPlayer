package com.example.myplayer.ui.components.recommendation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myplayer.ui.theme.clayConcave
import com.example.myplayer.ui.theme.claySurface
import com.example.myplayer.ui.theme.OnSurface
import com.example.myplayer.ui.theme.OnSurfaceVariant
import com.example.myplayer.ui.theme.SurfaceLight
import com.example.myplayer.ui.theme.TextMuted
import com.example.myplayer.ui.components.ClayIconButton
import androidx.compose.material.icons.filled.Refresh

@Composable
fun RecommendationEmptyState(
    onRefreshClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clayConcave(borderRadius = 40.dp, backgroundColor = SurfaceLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.LibraryMusic, null, tint = TextMuted, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No recommendations available",
            style = MaterialTheme.typography.titleMedium,
            color = OnSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Continue listening to generate recommendations.",
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        ClayIconButton(onClick = onRefreshClick, size = 48.dp) {
            Icon(Icons.Filled.Refresh, null, tint = OnSurfaceVariant, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun RecommendationLoadingCard() {
    Box(
        modifier = Modifier
            .width(180.dp)
            .height(260.dp)
            .claySurface(
                borderRadius = 28.dp,
                backgroundColor = SurfaceLight
            )
    ) {
        // Skeleton state
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(148.dp)
                    .clayConcave(borderRadius = 20.dp)
            )
            Spacer(Modifier.height(14.dp))
            Box(modifier = Modifier.height(16.dp).fillMaxWidth(0.8f).clayConcave(borderRadius = 8.dp))
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.height(12.dp).fillMaxWidth(0.5f).clayConcave(borderRadius = 6.dp))
        }
    }
}

@Composable
fun RecommendationErrorState(
    onRetryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Couldn't load recommendations",
            style = MaterialTheme.typography.titleMedium,
            color = OnSurface
        )
        Spacer(Modifier.height(16.dp))
        ClayIconButton(onClick = onRetryClick, size = 48.dp) {
            Icon(Icons.Filled.Refresh, null, tint = OnSurfaceVariant, modifier = Modifier.size(24.dp))
        }
    }
}
