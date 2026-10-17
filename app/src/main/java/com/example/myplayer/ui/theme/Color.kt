package com.example.myplayer.ui.theme

import androidx.compose.ui.graphics.Color

// ── Exact Reference Palette (171C26, 6C7689, A4AAB7, FFFFFF) ──────────────
val CharcoalPrimary      = Color(0xFF171C26) // Primary dark charcoal ink
val SlateSecondary       = Color(0xFF6C7689) // Secondary text / artist
val SilverMuted          = Color(0xFFA4AAB7) // Muted icons / numbers / inactive wave
val PureWhite            = Color(0xFFFFFFFF) // Main background & cards
val SoftGraySurface      = Color(0xFFF3F5F8) // Secondary button / active row pill

// Mapped to existing theme tokens so all screens work seamlessly
val Primary              = CharcoalPrimary
val OnPrimary            = PureWhite
val PrimaryContainer     = SoftGraySurface
val OnPrimaryContainer   = CharcoalPrimary

val Secondary            = SlateSecondary
val OnSecondary          = PureWhite
val SecondaryContainer   = SoftGraySurface
val OnSecondaryContainer = CharcoalPrimary

val Tertiary             = SilverMuted
val OnTertiary           = PureWhite
val TertiaryContainer    = SoftGraySurface
val OnTertiaryContainer  = CharcoalPrimary

val Background           = PureWhite
val OnBackground         = CharcoalPrimary
val Surface              = PureWhite
val SurfaceVariant       = SoftGraySurface
val Outline              = SilverMuted
val CardBorderOlive      = Color(0xFFEBEEF3)
val OutlineVariant       = CardBorderOlive
val Error                = Color(0xFFD32F2F)
val OnError              = PureWhite
val ErrorContainer       = Color(0xFFFFDAD6)
val OnErrorContainer     = Color(0xFF93000A)
val PureBlack            = Color(0xFF000000)

val NeonLimePrimary      = CharcoalPrimary
val OnNeonLime           = PureWhite
val ClayPrimary          = CharcoalPrimary
val ClaySecondary        = SlateSecondary
val ClayTertiary         = SilverMuted

// ── Backgrounds & Surfaces ────────────────────────────────────────────────
val DeepOliveBackground    = PureWhite
val CloudBlueBackground    = PureWhite
val SurfaceLight           = PureWhite
val SurfaceContainerLowest = PureWhite
val SurfaceContainerLow    = Color(0xFFF7F8FA)
val SurfaceContainer       = SoftGraySurface
val SurfaceContainerHigh   = Color(0xFFECEFF4)
val SurfaceContainerHighest= Color(0xFFE2E6ED)

// ── Text & Content ────────────────────────────────────────────────────────
val OnSurface        = CharcoalPrimary // #171C26
val OnSurfaceVariant = SlateSecondary  // #6C7689
val TextMuted        = SilverMuted     // #A4AAB7

// ── Borders & Shadows ─────────────────────────────────────────────────────
val NeonLimeGlow     = CharcoalPrimary

val ClayShadowOuter      = Color(0xFF171C26).copy(alpha = 0.06f)
val ClayShadowInnerLight = Color(0xFFFFFFFF).copy(alpha = 0.80f)
val ClayShadowInnerDark  = Color(0xFF171C26).copy(alpha = 0.05f)

val GradientPrimarySecondary = listOf(CharcoalPrimary, SlateSecondary)
val GradientPrimaryTertiary  = listOf(CharcoalPrimary, SilverMuted)
val GradientBannerCard       = listOf(SoftGraySurface, PureWhite)