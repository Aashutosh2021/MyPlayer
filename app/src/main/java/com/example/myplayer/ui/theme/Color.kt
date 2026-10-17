package com.example.myplayer.ui.theme

import androidx.compose.ui.graphics.Color

// ── Core Charcoal & Slate Minimal Palette (Image 2) ──────────────────────────
val CharcoalPrimary      = Color(0xFF171C26) // Deep Charcoal / Primary
val SlateSecondary       = Color(0xFF6C7689) // Slate Blue-Gray / Subtitles
val LightSlateMuted      = Color(0xFFA4AAB7) // Light Slate / Muted / Inactive
val PureWhite            = Color(0xFFFFFFFF) // Crisp Pure White
val CleanBackground      = Color(0xFFF7F8FA) // Soft off-white canvas
val CardSurface          = Color(0xFFFFFFFF) // Crisp card surface
val BorderSubtle         = Color(0xFFE5E7EB) // Subtle light border

// ── Backward-compatible Aliases for Entire App ────────────────────────────────
val NeonLimePrimary      = CharcoalPrimary
val OnNeonLime           = PureWhite
val ClayPrimary          = CharcoalPrimary
val OnPrimary            = PureWhite
val PrimaryContainer     = Color(0xFF222938)
val OnPrimaryContainer   = PureWhite

val ClaySecondary        = SlateSecondary
val OnSecondary          = PureWhite
val SecondaryContainer   = Color(0xFFEDF0F5)
val OnSecondaryContainer = CharcoalPrimary

val ClayTertiary         = LightSlateMuted
val OnTertiary           = PureWhite
val TertiaryContainer    = Color(0xFFF2F4F8)
val OnTertiaryContainer  = CharcoalPrimary

val DeepOliveBackground    = CleanBackground
val CloudBlueBackground    = CleanBackground
val SurfaceLight           = CardSurface
val SurfaceContainerLowest = PureWhite
val SurfaceContainerLow    = Color(0xFFF4F6F9)
val SurfaceContainer       = Color(0xFFECEFF4)
val SurfaceContainerHigh   = Color(0xFFE2E6EE)
val SurfaceContainerHighest= Color(0xFFD3D8E2)

val OnSurface        = CharcoalPrimary
val OnSurfaceVariant = SlateSecondary
val TextMuted        = LightSlateMuted

val CardBorderOlive  = BorderSubtle
val NeonLimeGlow     = LightSlateMuted

// ── Clay Shadows & Highlights ─────────────────────────────────────────────────
val ClayShadowOuter      = Color(0xFF000000).copy(alpha = 0.08f)
val ClayShadowInnerLight = Color(0xFFFFFFFF).copy(alpha = 0.90f)
val ClayShadowInnerDark  = Color(0xFF000000).copy(alpha = 0.04f)

// ── Gradients ─────────────────────────────────────────────────────────────────
val GradientPrimarySecondary = listOf(CharcoalPrimary, SlateSecondary)
val GradientPrimaryTertiary  = listOf(CharcoalPrimary, LightSlateMuted)
val GradientBannerCard       = listOf(Color(0xFF202634), Color(0xFF171C26))