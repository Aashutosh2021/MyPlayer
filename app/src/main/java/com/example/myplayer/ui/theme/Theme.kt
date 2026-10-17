package com.example.myplayer.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
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

@Composable
fun MyPlayerTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = EditorialLightColorScheme,
        typography  = Typography,
        content     = content
    )
}