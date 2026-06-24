package com.example.myplayer.ui.theme

import androidx.compose.ui.graphics.Color

// ── Core Brand / Claymorphism Palette ──────────────────────────────────────
val ClayPrimary   = Color(0xFF0057C2)
val OnPrimary     = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFF2C70E2)
val OnPrimaryContainer = Color(0xFFFEFCFF)

val ClaySecondary = Color(0xFF6B38D4)
val OnSecondary   = Color(0xFFFFFFFF)
val SecondaryContainer = Color(0xFF8455EF)
val OnSecondaryContainer = Color(0xFFFFFBFF)

val ClayTertiary  = Color(0xFF006574)
val OnTertiary    = Color(0xFFFFFFFF)
val TertiaryContainer = Color(0xFF008092)
val OnTertiaryContainer = Color(0xFFF8FDFF)

// ── Backgrounds & Surfaces ────────────────────────────────────────────────
val CloudBlueBackground = Color(0xFFEEF3FF)
val SurfaceLight        = Color(0xFFFAF9FF)
val SurfaceContainerLowest = Color(0xFFFFFFFF)
val SurfaceContainerLow    = Color(0xFFF2F3FD)
val SurfaceContainer       = Color(0xFFECEDF7)
val SurfaceContainerHigh   = Color(0xFFE7E7F1)
val SurfaceContainerHighest= Color(0xFFE1E2EC)

// ── Text ────────────────────────────────────────────────────────────────────
val OnSurface        = Color(0xFF191B22)
val OnSurfaceVariant = Color(0xFF424753)
val TextMuted        = Color(0xFF727785) // Outline color

// ── Shadows & Highlights (used in modifiers) ──────────────────────────────
val ClayShadowOuter = Color(0xFF0057C2).copy(alpha = 0.1f)
val ClayShadowInnerLight = Color(0xFFFFFFFF).copy(alpha = 0.8f)
val ClayShadowInnerDark = Color(0xFF000000).copy(alpha = 0.05f)

// ── Gradients ───────────────────────────────────────────────────────────────
val GradientPrimarySecondary = listOf(ClayPrimary, ClaySecondary)
val GradientPrimaryTertiary = listOf(ClayPrimary, ClayTertiary)