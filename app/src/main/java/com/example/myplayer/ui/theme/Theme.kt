package com.example.myplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ClayLightColorScheme = lightColorScheme(
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

    background         = CloudBlueBackground,
    onBackground       = OnSurface,

    surface            = SurfaceLight,
    onSurface          = OnSurface,
    surfaceVariant     = SurfaceContainer,
    onSurfaceVariant   = OnSurfaceVariant,

    outline            = TextMuted,
    outlineVariant     = SurfaceContainerHigh,

    error              = Color(0xFFBA1A1A),
    onError            = Color.White,
    errorContainer     = Color(0xFFFFDAD6),
    onErrorContainer   = Color(0xFF93000A)
)

@Composable
fun MyPlayerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ClayLightColorScheme,
        typography  = Typography,
        content     = content
    )
}