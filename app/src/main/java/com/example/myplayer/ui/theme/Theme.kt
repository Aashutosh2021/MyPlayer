package com.example.myplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkOliveColorScheme = darkColorScheme(
    primary            = ClayPrimary,
    onPrimary          = OnPrimary,
    primaryContainer   = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,

    secondary          = ClaySecondary,
    onSecondary        = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,

    tertiary           = ClayTertiary,
    onTertiary         = OnTertiary,
    tertiaryContainer  = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,

    background         = DeepOliveBackground,
    onBackground       = OnSurface,

    surface            = SurfaceLight,
    onSurface          = OnSurface,
    surfaceVariant     = SurfaceContainer,
    onSurfaceVariant   = OnSurfaceVariant,

    outline            = TextMuted,
    outlineVariant     = SurfaceContainerHigh,

    error              = Color(0xFFFF5252),
    onError            = Color(0xFF38000A),
    errorContainer     = Color(0xFF491118),
    onErrorContainer   = Color(0xFFFFDAD6)
)

@Composable
fun MyPlayerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkOliveColorScheme,
        typography  = Typography,
        content     = content
    )
}