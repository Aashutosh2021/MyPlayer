package com.example.myplayer.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val EditorialLightColorScheme = lightColorScheme(
    primary            = CharcoalPrimary,
    onPrimary          = PureWhite,
    primaryContainer   = SoftGraySurface,
    onPrimaryContainer = CharcoalPrimary,
    secondary          = SlateSecondary,
    onSecondary        = PureWhite,
    secondaryContainer = SoftGraySurface,
    onSecondaryContainer = CharcoalPrimary,
    tertiary           = SilverMuted,
    onTertiary         = PureWhite,
    background         = PureWhite,
    onBackground       = CharcoalPrimary,
    surface            = PureWhite,
    onSurface          = CharcoalPrimary,
    surfaceVariant     = SoftGraySurface,
    onSurfaceVariant   = SlateSecondary,
    outline            = SilverMuted,
    outlineVariant     = CardBorderOlive,
    error              = Color(0xFFD32F2F),
    onError            = PureWhite
)

private val EditorialDarkColorScheme = darkColorScheme(
    primary            = PureWhite,
    onPrimary          = CharcoalPrimary,
    primaryContainer   = Color(0xFF263346),
    onPrimaryContainer = PureWhite,
    secondary          = SilverMuted,
    onSecondary        = CharcoalPrimary,
    secondaryContainer = Color(0xFF1C2330),
    onSecondaryContainer = PureWhite,
    tertiary           = SlateSecondary,
    onTertiary         = PureWhite,
    background         = CharcoalPrimary,
    onBackground       = PureWhite,
    surface            = CharcoalPrimary,
    onSurface          = PureWhite,
    surfaceVariant     = Color(0xFF1F2633),
    onSurfaceVariant   = SilverMuted,
    outline            = Color(0xFF384355),
    outlineVariant     = Color(0xFF263040),
    error              = Color(0xFFFF5252),
    onError            = PureWhite
)

@Composable
fun MyPlayerTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    val colorScheme = if (darkTheme) EditorialDarkColorScheme else EditorialLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}