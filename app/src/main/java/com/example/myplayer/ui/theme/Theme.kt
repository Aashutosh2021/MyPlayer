package com.example.myplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StitchColorScheme = lightColorScheme(
    primary            = Primary,
    onPrimary          = OnPrimary,
    primaryContainer   = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,

    secondary          = Secondary,
    onSecondary        = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,

    tertiary           = Tertiary,
    onTertiary         = OnTertiary,
    tertiaryContainer  = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,

    background         = Background,
    onBackground       = OnBackground,

    surface            = Surface,
    onSurface          = OnSurface,
    surfaceVariant     = SurfaceVariant,
    onSurfaceVariant   = OnSurfaceVariant,

    outline            = Outline,
    outlineVariant     = OutlineVariant,

    error              = Error,
    onError            = OnError,
    errorContainer     = ErrorContainer,
    onErrorContainer   = OnErrorContainer
)

@Composable
fun MyPlayerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StitchColorScheme,
        typography  = Typography,
        content     = content
    )
}


