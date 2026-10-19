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
    primary                 = CharcoalPrimary,
    onPrimary               = PureWhite,
    primaryContainer        = SoftGraySurface,
    onPrimaryContainer      = CharcoalPrimary,
    secondary               = SlateSecondary,
    onSecondary             = PureWhite,
    secondaryContainer      = SoftGraySurface,
    onSecondaryContainer    = CharcoalPrimary,
    tertiary                = SilverMuted,
    onTertiary              = PureWhite,
    background              = PureWhite,
    onBackground            = CharcoalPrimary,
    surface                 = PureWhite,
    onSurface               = CharcoalPrimary,
    surfaceVariant          = SoftGraySurface,
    onSurfaceVariant        = SlateSecondary,
    surfaceContainerLowest  = PureWhite,
    surfaceContainerLow     = Color(0xFFF7F8FA),
    surfaceContainer        = SoftGraySurface,
    surfaceContainerHigh    = Color(0xFFECEFF4),
    surfaceContainerHighest = Color(0xFFE2E6ED),
    outline                 = SilverMuted,
    outlineVariant          = Color(0xFFEBEEF3),
    error                   = Color(0xFFD32F2F),
    onError                 = PureWhite
)

private val EditorialDarkColorScheme = darkColorScheme(
    primary                 = PureWhite,
    onPrimary               = CharcoalPrimary,
    primaryContainer        = Color(0xFF263346),
    onPrimaryContainer      = PureWhite,
    secondary               = SilverMuted,
    onSecondary             = CharcoalPrimary,
    secondaryContainer      = Color(0xFF1C2330),
    onSecondaryContainer    = PureWhite,
    tertiary                = SlateSecondary,
    onTertiary              = PureWhite,
    background              = CharcoalPrimary,
    onBackground            = PureWhite,
    surface                 = CharcoalPrimary,
    onSurface               = PureWhite,
    surfaceVariant          = Color(0xFF1F2633),
    onSurfaceVariant        = SilverMuted,
    surfaceContainerLowest  = Color(0xFF131720),
    surfaceContainerLow     = Color(0xFF1C222E),
    surfaceContainer        = Color(0xFF1F2633),
    surfaceContainerHigh    = Color(0xFF263346),
    surfaceContainerHighest = Color(0xFF323E52),
    outline                 = Color(0xFF384355),
    outlineVariant          = Color(0xFF263040),
    error                   = Color(0xFFFF5252),
    onError                 = PureWhite
)

@Composable
fun MyPlayerTheme(
    darkTheme: Boolean = ThemeState.isDarkMode,
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