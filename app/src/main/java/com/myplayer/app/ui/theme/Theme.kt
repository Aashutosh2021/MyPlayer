package com.myplayer.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * App-wide MyPlayer theme with day/night support. Both schemes share the MyPlayer
 * neutral philosophy (near-neutral grays + purple accent); night is the dark
 * palette, day is its light counterpart. Hex values are hardcoded here (not
 * imported from ui.myplayer) to avoid a package cycle.
 */
private val MyPlayerNightColorScheme = darkColorScheme(
    primary                 = PureWhite,                 // white accent on dark
    onPrimary               = CharcoalPrimary,           // dark text/icon on white
    primaryContainer        = Color(0xFF3A3A42),         // MyPlayer primary pill
    onPrimaryContainer      = Color(0xFFFFFFFF),
    secondary               = Color(0xFF9A9AA6),         // MyPlayer secondary text
    onSecondary             = CharcoalPrimary,
    secondaryContainer      = Color(0xFF1B1B20),         // MyPlayer secondary pill
    onSecondaryContainer    = Color(0xFFFFFFFF),
    tertiary                = Color(0xFF8B5CF6),         // MyPlayer accent purple
    onTertiary              = Color(0xFFFFFFFF),
    background              = Color(0xFF070709),         // MyPlayerBlack
    onBackground            = Color(0xFFF5F5F7),         // MyPlayerTextPrimary
    surface                 = Color(0xFF141418),         // MyPlayerSurface
    onSurface               = Color(0xFFF5F5F7),
    surfaceVariant          = Color(0xFF1E1E24),         // MyPlayerSurfaceHigh
    onSurfaceVariant        = Color(0xFF9A9AA6),         // MyPlayerTextSecondary
    surfaceContainerLowest  = Color(0xFF070709),
    surfaceContainerLow     = Color(0xFF101014),
    surfaceContainer        = Color(0xFF141418),
    surfaceContainerHigh    = Color(0xFF1E1E24),
    surfaceContainerHighest = Color(0xFF282830),
    outline                 = Color(0xFF282830),         // MyPlayerCardBorder
    outlineVariant          = Color(0xFF282830),
    error                   = Color(0xFFFF5252),
    onError                 = PureWhite,
    errorContainer          = Color(0xFF3A1A1C),
    onErrorContainer        = Color(0xFFFFB4AB)
)

private val MyPlayerDayColorScheme = lightColorScheme(
    primary                 = Color(0xFF17171C),         // near-black accent on light
    onPrimary               = Color(0xFFFFFFFF),
    primaryContainer        = Color(0xFFEDEDEF),         // light primary pill
    onPrimaryContainer      = Color(0xFF17171C),
    secondary               = Color(0xFF6B6B76),         // light secondary text
    onSecondary             = Color(0xFFFFFFFF),
    secondaryContainer      = Color(0xFFF5F5F7),         // light secondary pill
    onSecondaryContainer    = Color(0xFF17171C),
    tertiary                = Color(0xFF8B5CF6),
    onTertiary              = Color(0xFFFFFFFF),
    background              = Color(0xFFFFFFFF),
    onBackground            = Color(0xFF17171C),
    surface                 = Color(0xFFF7F7F9),
    onSurface               = Color(0xFF17171C),
    surfaceVariant          = Color(0xFFEFEFF2),
    onSurfaceVariant        = Color(0xFF6B6B76),
    surfaceContainerLowest  = Color(0xFFFFFFFF),
    surfaceContainerLow     = Color(0xFFF7F7F9),
    surfaceContainer        = Color(0xFFF2F2F4),
    surfaceContainerHigh    = Color(0xFFEFEFF2),
    surfaceContainerHighest = Color(0xFFE2E2E8),
    outline                 = Color(0xFFE2E2E8),
    outlineVariant          = Color(0xFFE2E2E8),
    error                   = Color(0xFFD32F2F),
    onError                 = Color(0xFFFFFFFF),
    errorContainer          = Color(0xFFFFDAD6),
    onErrorContainer        = Color(0xFF93000A)
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
            // Night = light icons, day = dark icons.
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    val colorScheme = if (darkTheme) MyPlayerNightColorScheme else MyPlayerDayColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
