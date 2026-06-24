package com.example.myplayer.ui.theme

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 1. CLAY SURFACE (Convex)
 * Soft outer shadow + Inner top-left highlight + Inner bottom-right shadow.
 * If [interactionSource] is provided, it will invert to concave when pressed.
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
) = composed {
    val isPressed = interactionSource?.collectIsPressedAsState()?.value == true

    if (isPressed) {
        // Switch to pressed (concave) look
        Modifier.clayConcave(
            borderRadius = borderRadius,
            backgroundColor = backgroundColor,
            innerDarkColor = innerDarkColor.copy(alpha = innerDarkColor.alpha * 2),
            innerLightColor = innerLightColor,
            innerDarkOffset = 4.dp,
            innerLightOffset = -4.dp,
            innerDarkBlur = 8.dp,
            innerLightBlur = 8.dp
        )
    } else {
        // Normal convex look
        this.drawBehind {
            val radiusPx = borderRadius.toPx()
            
            // 1. Draw outer shadow
            val outerPaint = Paint().apply {
                asFrameworkPaint().apply {
                    color = outerShadowColor.toArgb()
                    maskFilter = BlurMaskFilter(outerBlur.toPx(), BlurMaskFilter.Blur.NORMAL)
                }
            }
            drawIntoCanvas { canvas ->
                canvas.save()
                canvas.translate(0f, outerOffsetY.toPx())
                canvas.drawRoundRect(0f, 0f, size.width, size.height, radiusPx, radiusPx, outerPaint)
                canvas.restore()
            }
            
            // Base shape (background drawn by background modifier later, or draw it here)
            // But we can let Modifier.background handle it.
        }
        .background(backgroundColor, RoundedCornerShape(borderRadius))
        .drawBehind {
            val radiusPx = borderRadius.toPx()
            val rectPath = Path().apply {
                addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(radiusPx)))
            }
            val inversePath = Path().apply {
                addRect(Rect(-200f, -200f, size.width + 200f, size.height + 200f))
                op(this, rectPath, PathOperation.Difference)
            }

            // 2. Inner Light (Top-Left)
            val lightPaint = Paint().apply {
                asFrameworkPaint().apply {
                    color = innerLightColor.toArgb()
                    maskFilter = BlurMaskFilter(innerLightBlur.toPx(), BlurMaskFilter.Blur.NORMAL)
                }
            }
            drawIntoCanvas { canvas ->
                canvas.save()
                canvas.clipPath(rectPath) // clip to rounded rect
                canvas.translate(innerLightOffset.toPx(), innerLightOffset.toPx())
                canvas.drawPath(inversePath, lightPaint)
                canvas.restore()
            }

            // 3. Inner Dark (Bottom-Right)
            val darkPaint = Paint().apply {
                asFrameworkPaint().apply {
                    color = innerDarkColor.toArgb()
                    maskFilter = BlurMaskFilter(innerDarkBlur.toPx(), BlurMaskFilter.Blur.NORMAL)
                }
            }
            drawIntoCanvas { canvas ->
                canvas.save()
                canvas.clipPath(rectPath)
                canvas.translate(innerDarkOffset.toPx(), innerDarkOffset.toPx())
                canvas.drawPath(inversePath, darkPaint)
                canvas.restore()
            }
        }
    }
}

/**
 * 2. CLAY CONCAVE (Sunken)
 * Deep inner dark top-left + Inner light bottom-right.
 */
fun Modifier.clayConcave(
    borderRadius: Dp = 16.dp,
    backgroundColor: Color = SurfaceContainer,
    innerDarkColor: Color = Color(0, 0, 0, 25), // 10% black
    innerLightColor: Color = Color(255, 255, 255, 204), // 80% white
    innerDarkOffset: Dp = 4.dp,
    innerLightOffset: Dp = -4.dp,
    innerDarkBlur: Dp = 8.dp,
    innerLightBlur: Dp = 8.dp
) = this.background(backgroundColor, RoundedCornerShape(borderRadius))
    .drawBehind {
        val radiusPx = borderRadius.toPx()
        val rectPath = Path().apply {
            addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(radiusPx)))
        }
        val inversePath = Path().apply {
            addRect(Rect(-200f, -200f, size.width + 200f, size.height + 200f))
            op(this, rectPath, PathOperation.Difference)
        }

        // Inner Dark (Top-Left)
        val darkPaint = Paint().apply {
            asFrameworkPaint().apply {
                color = innerDarkColor.toArgb()
                maskFilter = BlurMaskFilter(innerDarkBlur.toPx(), BlurMaskFilter.Blur.NORMAL)
            }
        }
        drawIntoCanvas { canvas ->
            canvas.save()
            canvas.clipPath(rectPath)
            canvas.translate(innerDarkOffset.toPx(), innerDarkOffset.toPx())
            canvas.drawPath(inversePath, darkPaint)
            canvas.restore()
        }

        // Inner Light (Bottom-Right)
        val lightPaint = Paint().apply {
            asFrameworkPaint().apply {
                color = innerLightColor.toArgb()
                maskFilter = BlurMaskFilter(innerLightBlur.toPx(), BlurMaskFilter.Blur.NORMAL)
            }
        }
        drawIntoCanvas { canvas ->
            canvas.save()
            canvas.clipPath(rectPath)
            canvas.translate(innerLightOffset.toPx(), innerLightOffset.toPx())
            canvas.drawPath(inversePath, lightPaint)
            canvas.restore()
        }
    }
    .clip(RoundedCornerShape(borderRadius))
