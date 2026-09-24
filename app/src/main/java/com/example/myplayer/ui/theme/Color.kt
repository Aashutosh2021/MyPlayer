package com.example.myplayer.ui.theme

import androidx.compose.ui.graphics.Color

// ── Core Brand / Electric Neon Lime & Dark Olive Palette ──────────────────
val NeonLimePrimary      = Color(0xFFD2F83A)
val OnNeonLime           = Color(0xFF12160E)
val ClayPrimary          = NeonLimePrimary
val OnPrimary            = OnNeonLime
val PrimaryContainer     = Color(0xFF232D17)
val OnPrimaryContainer   = Color(0xFFD2F83A)

val ClaySecondary        = Color(0xFF8BA62B)
val OnSecondary          = Color(0xFF12160E)
val SecondaryContainer   = Color(0xFF1B2313)
val OnSecondaryContainer = Color(0xFFD6F575)

val ClayTertiary         = Color(0xFF556942)
val OnTertiary           = Color(0xFFFFFFFF)
val TertiaryContainer    = Color(0xFF1E2616)
val OnTertiaryContainer  = Color(0xFFDCEBC9)

// ── Backgrounds & Surfaces (Deep Olive-Charcoal) ──────────────────────────
val DeepOliveBackground    = Color(0xFF12160E)
val CloudBlueBackground    = DeepOliveBackground // Backward-compatible alias
val SurfaceLight           = Color(0xFF1A2216)   // Main dark olive card surface
val SurfaceContainerLowest = Color(0xFF0E130B)
val SurfaceContainerLow    = Color(0xFF151C12)
val SurfaceContainer       = Color(0xFF1E2618)
val SurfaceContainerHigh   = Color(0xFF26311F)
val SurfaceContainerHighest= Color(0xFF324029)

// ── Text & Content ────────────────────────────────────────────────────────
val OnSurface        = Color(0xFFF5F8F0) // Crisp light white
val OnSurfaceVariant = Color(0xFF9AA592) // Soft sage olive
val TextMuted        = Color(0xFF6E7A66) // Inactive sage gray

// ── Borders & Glows ───────────────────────────────────────────────────────
val CardBorderOlive  = Color(0xFF2A3620)
val NeonLimeGlow     = Color(0xFFD2F83A)

// ── Shadows & Highlights (used in clay/surface modifiers) ─────────────────
val ClayShadowOuter      = Color(0xFF000000).copy(alpha = 0.55f)
val ClayShadowInnerLight = Color(0xFFFFFFFF).copy(alpha = 0.07f)
val ClayShadowInnerDark  = Color(0xFF000000).copy(alpha = 0.50f)

// ── Gradients ─────────────────────────────────────────────────────────────
val GradientPrimarySecondary = listOf(NeonLimePrimary, Color(0xFF8BA62B))
val GradientPrimaryTertiary  = listOf(NeonLimePrimary, Color(0xFF556942))
val GradientBannerCard       = listOf(Color(0xFF24301B), Color(0xFF151C12))