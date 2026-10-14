package com.example.myplayer.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
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

/**
 * 3. Tactile 3D Spring Press Effect (Zero layout cost — runs on GPU RenderThread)
 */
fun Modifier.bouncy3DClick(
    scaleDown: Float = 0.95f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bouncy3DScale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

/**
 * 4. 3D CoverFlow Carousel Modifier for LazyRow items
 */
fun Modifier.carousel3DItem(
    listState: LazyListState,
    index: Int
): Modifier = this.graphicsLayer {
    val layoutInfo = listState.layoutInfo
    val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    if (itemInfo != null && layoutInfo.viewportEndOffset > 0) {
        val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
        val itemCenter = itemInfo.offset + (itemInfo.size / 2f)
        val normalizedOffset = ((itemCenter - viewportCenter) / viewportCenter.coerceAtLeast(1f)).coerceIn(-1f, 1f)
        val absOffset = kotlin.math.abs(normalizedOffset)

        val scale = 1f - (absOffset * 0.10f)
        scaleX = scale
        scaleY = scale
        rotationY = -normalizedOffset * 16f
        cameraDistance = 12f * density
        alpha = 1f - (absOffset * 0.22f)
    }
}
