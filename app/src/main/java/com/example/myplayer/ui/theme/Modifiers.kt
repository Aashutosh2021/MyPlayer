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
import androidx.compose.ui.draw.drawWithCache
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
        this.drawWithCache {
            val radiusPx = borderRadius.toPx()
            val outerPaint = Paint().apply {
                asFrameworkPaint().apply {
                    color = outerShadowColor.toArgb()
                    maskFilter = BlurMaskFilter(outerBlur.toPx(), BlurMaskFilter.Blur.NORMAL)
                }
            }
            onDrawBehind {
                drawIntoCanvas { canvas ->
                    canvas.save()
                    canvas.translate(0f, outerOffsetY.toPx())
                    canvas.drawRoundRect(0f, 0f, size.width, size.height, radiusPx, radiusPx, outerPaint)
                    canvas.restore()
                }
            }
        }
        .background(backgroundColor, RoundedCornerShape(borderRadius))
        .drawWithCache {
            val radiusPx = borderRadius.toPx()
            val rectPath = Path().apply {
                addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(radiusPx)))
            }
            val inversePath = Path().apply {
                addRect(Rect(-200f, -200f, size.width + 200f, size.height + 200f))
                op(this, rectPath, PathOperation.Difference)
            }

            val lightPaint = Paint().apply {
                asFrameworkPaint().apply {
                    color = innerLightColor.toArgb()
                    maskFilter = BlurMaskFilter(innerLightBlur.toPx(), BlurMaskFilter.Blur.NORMAL)
                }
            }
            
            val darkPaint = Paint().apply {
                asFrameworkPaint().apply {
                    color = innerDarkColor.toArgb()
                    maskFilter = BlurMaskFilter(innerDarkBlur.toPx(), BlurMaskFilter.Blur.NORMAL)
                }
            }
            
            onDrawWithContent {
                drawContent()
                drawIntoCanvas { canvas ->
                    canvas.save()
                    canvas.clipPath(rectPath)
                    canvas.translate(innerLightOffset.toPx(), innerLightOffset.toPx())
                    canvas.drawPath(inversePath, lightPaint)
                    canvas.restore()
                    
                    canvas.save()
                    canvas.clipPath(rectPath)
                    canvas.translate(innerDarkOffset.toPx(), innerDarkOffset.toPx())
                    canvas.drawPath(inversePath, darkPaint)
                    canvas.restore()
                }
            }
        }
    }
}

/**
 * 2. CLAY CONCAVE (Sunken)
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
) = this.background(backgroundColor, RoundedCornerShape(borderRadius))
    .drawWithCache {
        val radiusPx = borderRadius.toPx()
        val rectPath = Path().apply {
            addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(radiusPx)))
        }
        val inversePath = Path().apply {
            addRect(Rect(-200f, -200f, size.width + 200f, size.height + 200f))
            op(this, rectPath, PathOperation.Difference)
        }

        val darkPaint = Paint().apply {
            asFrameworkPaint().apply {
                color = innerDarkColor.toArgb()
                maskFilter = BlurMaskFilter(innerDarkBlur.toPx(), BlurMaskFilter.Blur.NORMAL)
            }
        }
        
        val lightPaint = Paint().apply {
            asFrameworkPaint().apply {
                color = innerLightColor.toArgb()
                maskFilter = BlurMaskFilter(innerLightBlur.toPx(), BlurMaskFilter.Blur.NORMAL)
            }
        }
        
        onDrawWithContent {
            drawContent()
            drawIntoCanvas { canvas ->
                canvas.save()
                canvas.clipPath(rectPath)
                canvas.translate(innerDarkOffset.toPx(), innerDarkOffset.toPx())
                canvas.drawPath(inversePath, darkPaint)
                canvas.restore()

                canvas.save()
                canvas.clipPath(rectPath)
                canvas.translate(innerLightOffset.toPx(), innerLightOffset.toPx())
                canvas.drawPath(inversePath, lightPaint)
                canvas.restore()
            }
        }
    }
    .clip(RoundedCornerShape(borderRadius))
