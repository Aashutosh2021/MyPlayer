package com.example.myplayer.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 1. CLAY SURFACE (Convex) — Hardware-accelerated (Zero BlurMaskFilter / clipPath overhead)
 */
fun Modifier.claySurface(
    borderRadius: Dp = 16.dp,
    backgroundColor: Color = SurfaceLight,
    outerShadowColor: Color = ClayShadowOuter,
    innerLightColor: Color = ClayShadowInnerLight,
    innerDarkColor: Color = ClayShadowInnerDark,
    outerBlur: Dp = 40.dp,
    outerOffsetY: Dp = 20.dp,
    innerLightOffset: Dp = 4.dp,
    innerDarkOffset: Dp = -6.dp,
    innerLightBlur: Dp = 8.dp,
    innerDarkBlur: Dp = 12.dp,
    interactionSource: InteractionSource? = null
): Modifier = this
    .background(backgroundColor, RoundedCornerShape(borderRadius))
    .drawWithCache {
        val radiusPx = borderRadius.toPx()
        val cornerRadius = CornerRadius(radiusPx, radiusPx)
        val strokeWidth = 1.dp.toPx()
        val halfStroke = strokeWidth / 2f
        val topLeft = Offset(halfStroke, halfStroke)
        val strokeSize = Size(
            (size.width - strokeWidth).coerceAtLeast(0f),
            (size.height - strokeWidth).coerceAtLeast(0f)
        )
        val borderBrush = Brush.verticalGradient(
            colors = listOf(
                innerLightColor.copy(alpha = (innerLightColor.alpha * 0.45f).coerceIn(0.06f, 0.28f)),
                innerDarkColor.copy(alpha = (innerDarkColor.alpha * 0.65f).coerceIn(0.12f, 0.45f))
            )
        )

        onDrawWithContent {
            drawContent()
            drawRoundRect(
                brush = borderBrush,
                topLeft = topLeft,
                size = strokeSize,
                cornerRadius = cornerRadius,
                style = Stroke(width = strokeWidth)
            )
        }
    }
    .clip(RoundedCornerShape(borderRadius))

/**
 * 2. CLAY CONCAVE (Sunken) — Hardware-accelerated (Zero BlurMaskFilter / clipPath overhead)
 */
fun Modifier.clayConcave(
    borderRadius: Dp = 16.dp,
    backgroundColor: Color = SurfaceContainer,
    innerDarkColor: Color = Color(0, 0, 0, 25),
    innerLightColor: Color = Color(255, 255, 255, 204),
    innerDarkOffset: Dp = 4.dp,
    innerLightOffset: Dp = -4.dp,
    innerDarkBlur: Dp = 8.dp,
    innerLightBlur: Dp = 8.dp
): Modifier = this
    .background(backgroundColor, RoundedCornerShape(borderRadius))
    .drawWithCache {
        val radiusPx = borderRadius.toPx()
        val cornerRadius = CornerRadius(radiusPx, radiusPx)
        val strokeWidth = 1.dp.toPx()
        val halfStroke = strokeWidth / 2f
        val topLeft = Offset(halfStroke, halfStroke)
        val strokeSize = Size(
            (size.width - strokeWidth).coerceAtLeast(0f),
            (size.height - strokeWidth).coerceAtLeast(0f)
        )
        val sunkenBrush = Brush.verticalGradient(
            colors = listOf(
                innerDarkColor.copy(alpha = 0.35f),
                innerLightColor.copy(alpha = 0.08f)
            )
        )

        onDrawWithContent {
            drawContent()
            drawRoundRect(
                brush = sunkenBrush,
                topLeft = topLeft,
                size = strokeSize,
                cornerRadius = cornerRadius,
                style = Stroke(width = strokeWidth)
            )
        }
    }
    .clip(RoundedCornerShape(borderRadius))
