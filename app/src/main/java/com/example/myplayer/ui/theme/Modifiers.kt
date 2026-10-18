package com.example.myplayer.ui.theme

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import kotlinx.coroutines.delay
import kotlin.math.abs
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    backgroundColor: Color = Surface,
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

/**
 * 5. SLIDE + POP-UP ENTRANCE ANIMATION (Scroll karte waqt cards pop-up hokar aate hain)
 */
fun Modifier.slidePopIn(
    index: Int = 0
): Modifier = composed {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val staggerDelay = (index % 6) * 35L
        if (staggerDelay > 0L) delay(staggerDelay)
        isVisible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.86f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "popScale"
    )
    val translationY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 60f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "popSlideY"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(220),
        label = "popAlpha"
    )

    this.graphicsLayer {
        this.scaleX = scale
        this.scaleY = scale
        this.translationY = translationY
        this.alpha = alpha
    }
}

/**
 * 6. HORIZONTAL CAROUSEL SLIDE POP-UP (LazyRow slide karte waqt center card pop-up hota hai)
 */
fun Modifier.carouselSlidePop(
    listState: LazyListState,
    index: Int
): Modifier = this.graphicsLayer {
    val layoutInfo = listState.layoutInfo
    val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    if (itemInfo != null && layoutInfo.viewportEndOffset > 0) {
        val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
        val itemCenter = itemInfo.offset + (itemInfo.size / 2f)
        val normalized = ((itemCenter - viewportCenter) / viewportCenter.coerceAtLeast(1f)).coerceIn(-1f, 1f)
        val absNorm = abs(normalized)

        // Center card pops up (1.0f), side cards shrink (0.88f) and tilt in 3D
        val scale = 1f - (absNorm * 0.12f)
        scaleX = scale
        scaleY = scale
        translationY = absNorm * 18f
        rotationY = -normalized * 14f
        cameraDistance = 14f * density
        alpha = 1f - (absNorm * 0.2f)
    }
}

/**
 * 7. BOUNCE CLICK (Touch feedback with zero state-holding leaks & GPU RenderThread scaling)
 */
fun Modifier.bounceClick(
    scaleDown: Float = 0.94f,
    onClick: () -> Unit = {}
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bounceScale"
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
 * 8. SHIMMER EFFECT (Zero allocation per frame — operates on graphicsLayer alpha)
 */
fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )
    // Use graphicsLayer alpha instead of reallocating Modifier.background every frame!
    this
        .graphicsLayer { this.alpha = alpha }
        .background(Color.Gray.copy(alpha = 0.35f))
}

/**
 * 9. BOUNCY POP CLICK (Aliases to optimized bounceClick for backward compatibility)
 */
fun Modifier.bouncyPopClick(
    scaleDown: Float = 0.93f,
    onClick: () -> Unit
): Modifier = bounceClick(scaleDown = scaleDown, onClick = onClick)


