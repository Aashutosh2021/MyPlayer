package com.example.myplayer.ui.components.recommendation


import com.example.myplayer.ui.theme.*
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
@Composable
fun RecommendationReasonChip(reason: String) {
    if (reason.isBlank()) return
    Text(
        text = reason,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
        color = ClayPrimary,
        modifier = Modifier
            .claySurface(borderRadius = 12.dp, backgroundColor = MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}


