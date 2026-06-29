package com.example.myplayer.ui.components.recommendation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myplayer.ui.theme.claySurface
import com.example.myplayer.ui.theme.ClayPrimary
import com.example.myplayer.ui.theme.OnPrimary
import com.example.myplayer.ui.theme.SurfaceLight

@Composable
fun RecommendationReasonChip(reason: String) {
    if (reason.isBlank()) return
    Text(
        text = reason,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
        color = ClayPrimary,
        modifier = Modifier
            .claySurface(borderRadius = 12.dp, backgroundColor = SurfaceLight)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
