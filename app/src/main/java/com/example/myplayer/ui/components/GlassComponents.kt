package com.example.myplayer.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.myplayer.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// Clay Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ClayCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val baseModifier = modifier
        .claySurface(
            borderRadius = cornerRadius,
            interactionSource = if (onClick != null) interactionSource else null
        )
        .then(
            if (onClick != null) Modifier.clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ) else Modifier
        )

    Box(modifier = baseModifier, content = content)
}

// ─────────────────────────────────────────────────────────────────────────────
// Clay Button (Primary Action)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ClayButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .claySurface(
                borderRadius = cornerRadius,
                backgroundColor = ClayPrimary,
                innerLightColor = Color.White.copy(alpha = 0.5f),
                innerDarkColor = Color.Black.copy(alpha = 0.2f),
                interactionSource = interactionSource
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = OnPrimary
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Clay Icon Button (Circle)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ClayIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(size)
            .claySurface(
                borderRadius = size / 2,
                backgroundColor = SurfaceLight,
                interactionSource = interactionSource
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Section Header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PremiumSectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            ),
            color = OnSurface
        )
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelMedium,
                color = ClayPrimary,
                modifier = Modifier.clickable(onClick = onAction)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Clay Progress Bar (Concave track, convex progress)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ClayProgressBar(
    progress: Float,           // 0f..1f
    modifier: Modifier = Modifier,
    height: Dp = 12.dp
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .height(height)
            .clayConcave(borderRadius = height / 2)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(clampedProgress)
                .claySurface(
                    borderRadius = height / 2,
                    backgroundColor = ClayPrimary,
                    innerLightColor = Color.White.copy(alpha = 0.4f),
                    innerDarkColor = Color.Black.copy(alpha = 0.2f),
                    outerOffsetY = 2.dp,
                    outerBlur = 8.dp,
                    outerShadowColor = ClayPrimary.copy(alpha = 0.3f)
                )
        )
    }
}
