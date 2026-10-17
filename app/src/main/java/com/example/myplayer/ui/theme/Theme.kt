package com.example.myplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CharcoalMinimalColorScheme = lightColorScheme(
    primary            = CharcoalPrimary,
    onPrimary          = PureWhite,
    primaryContainer   = PrimaryContainer,
    onPrimaryContainer = PureWhite,

    secondary          = SlateSecondary,
    onSecondary        = PureWhite,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = CharcoalPrimary,

    tertiary           = LightSlateMuted,
    onTertiary         = PureWhite,
    tertiaryContainer  = TertiaryContainer,
    onTertiaryContainer = CharcoalPrimary,

    background         = CleanBackground,
    onBackground       = OnSurface,

    surface            = SurfaceLight,
    onSurface          = OnSurface,
    surfaceVariant     = SurfaceContainer,
    onSurfaceVariant   = OnSurfaceVariant,

    outline            = TextMuted,
    outlineVariant     = BorderSubtle,

    error              = Color(0xFFBA1A1A),
    onError            = PureWhite,
    errorContainer     = Color(0xFFFFDAD6),
    onErrorContainer   = Color(0xFF410002)
)

@Composable
fun MyPlayerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CharcoalMinimalColorScheme,
        typography  = Typography,
        content     = content
    )
}