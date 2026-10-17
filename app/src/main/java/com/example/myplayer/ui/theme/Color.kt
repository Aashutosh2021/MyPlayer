package com.example.myplayer.ui.theme

import androidx.compose.ui.graphics.Color

// -- Stitch Design Tokens (MyPlayer Android UI Redesign) --------------------------

// Primary
val Primary = Color(0xFF000103)
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFF171C26)
val OnPrimaryContainer = Color(0xFF7F8491)

// Secondary
val Secondary = Color(0xFF555F71)
val OnSecondary = Color(0xFFFFFFFF)
val SecondaryContainer = Color(0xFFD9E3F9)
val OnSecondaryContainer = Color(0xFF5B6577)

// Tertiary
val Tertiary = Color(0xFF000002)
val OnTertiary = Color(0xFFFFFFFF)
val TertiaryContainer = Color(0xFF161C26)
val OnTertiaryContainer = Color(0xFF7E8491)

// Surface & Background
val Background = Color(0xFFF9F9F9)
val OnBackground = Color(0xFF1A1C1C)
val Surface = Color(0xFFF9F9F9)
val OnSurface = Color(0xFF1A1C1C)
val SurfaceVariant = Color(0xFFEEEEEE)
val OnSurfaceVariant = Color(0xFF45474B)

// Surface Container Levels (Stitch Neutral Slate Palette)
val SurfaceContainerLowest = Color(0xFFFFFFFF)
val SurfaceContainerLow = Color(0xFFF3F3F4)
val SurfaceContainer = Color(0xFFEEEEEE)
val SurfaceContainerHigh = Color(0xFFE8E8E8)
val SurfaceContainerHighest = Color(0xFFE2E2E2)

// Outlines
val Outline = Color(0xFF76777C)
val OutlineVariant = Color(0xFFC6C6CC)

// Error
val Error = Color(0xFFBA1A1A)
val OnError = Color(0xFFFFFFFF)
val ErrorContainer = Color(0xFFFFDAD6)
val OnErrorContainer = Color(0xFF93000A)

// Pure White & Black
val PureWhite = Color(0xFFFFFFFF)
val PureBlack = Color(0xFF000000)

// Backward-compatible Aliases
val ClayPrimary = Primary
val ClaySecondary = Secondary
val ClayTertiary = OnPrimaryContainer
val DeepOliveBackground = Background
val CloudBlueBackground = Background
val TextMuted = OnPrimaryContainer
val CardBorderOlive = OutlineVariant
val NeonLimeGlow = OnPrimaryContainer
val NeonLimePrimary = Primary
val OnNeonLime = OnPrimary
val SurfaceLight = Surface

// Clay Shadows & Highlights
val ClayShadowOuter = Color(0xFF000000).copy(alpha = 0.08f)
val ClayShadowInnerLight = Color(0xFFFFFFFF).copy(alpha = 0.90f)
val ClayShadowInnerDark = Color(0xFF000000).copy(alpha = 0.04f)

// Gradients
val GradientPrimarySecondary = listOf(Primary, Secondary)
val GradientPrimaryTertiary = listOf(Primary, OnPrimaryContainer)
val GradientBannerCard = listOf(Color(0xFF202634), PrimaryContainer)
