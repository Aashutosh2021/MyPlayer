package com.example.myplayer.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// ── Theme State Bridge (0ms instant sync across App, DataStore & SharedPreferences) ──
object ThemeState {
    var isDarkMode by mutableStateOf(false)
    var onThemeChanged: ((Boolean) -> Unit)? = null

    fun toggle() {
        val next = !isDarkMode
        isDarkMode = next
        onThemeChanged?.invoke(next)
    }

    fun setDark(enabled: Boolean) {
        isDarkMode = enabled
        onThemeChanged?.invoke(enabled)
    }
}

// ── Exact Reference Palette (171C26, 6C7689, A4AAB7, FFFFFF) ──────────────
val CharcoalPrimary      = Color(0xFF171C26) // Primary dark charcoal ink
val SlateSecondary       = Color(0xFF6C7689) // Secondary text / artist
val SilverMuted          = Color(0xFFA4AAB7) // Muted icons / numbers / inactive wave
val PureWhite            = Color(0xFFFFFFFF) // Main background & cards
val SoftGraySurface      = Color(0xFFF3F5F8) // Secondary button / active row pill
val PureBlack            = Color(0xFF000000)

// ── Dynamic Theme Tokens (Auto-adapts to ThemeState for legacy & direct token callers) ──
val Primary: Color              get() = if (ThemeState.isDarkMode) PureWhite else CharcoalPrimary
val OnPrimary: Color            get() = if (ThemeState.isDarkMode) CharcoalPrimary else PureWhite
val PrimaryContainer: Color     get() = if (ThemeState.isDarkMode) Color(0xFF263346) else SoftGraySurface
val OnPrimaryContainer: Color   get() = if (ThemeState.isDarkMode) PureWhite else CharcoalPrimary

val Secondary: Color            get() = if (ThemeState.isDarkMode) SilverMuted else SlateSecondary
val OnSecondary: Color          get() = if (ThemeState.isDarkMode) CharcoalPrimary else PureWhite
val SecondaryContainer: Color   get() = if (ThemeState.isDarkMode) Color(0xFF1C2330) else SoftGraySurface
val OnSecondaryContainer: Color get() = if (ThemeState.isDarkMode) PureWhite else CharcoalPrimary

val Tertiary: Color             get() = if (ThemeState.isDarkMode) SlateSecondary else SilverMuted
val OnTertiary: Color           get() = if (ThemeState.isDarkMode) CharcoalPrimary else PureWhite
val TertiaryContainer: Color    get() = if (ThemeState.isDarkMode) Color(0xFF263346) else SoftGraySurface
val OnTertiaryContainer: Color  get() = if (ThemeState.isDarkMode) PureWhite else CharcoalPrimary

val Background: Color           get() = if (ThemeState.isDarkMode) CharcoalPrimary else PureWhite
val OnBackground: Color         get() = if (ThemeState.isDarkMode) PureWhite else CharcoalPrimary
val Surface: Color              get() = if (ThemeState.isDarkMode) CharcoalPrimary else PureWhite
val SurfaceVariant: Color       get() = if (ThemeState.isDarkMode) Color(0xFF1F2633) else SoftGraySurface
val Outline: Color              get() = if (ThemeState.isDarkMode) Color(0xFF384355) else SilverMuted
val CardBorderOlive: Color      get() = if (ThemeState.isDarkMode) Color(0xFF263040) else Color(0xFFEBEEF3)
val OutlineVariant: Color       get() = CardBorderOlive

val Error                       = Color(0xFFD32F2F)
val OnError                     = PureWhite
val ErrorContainer              = Color(0xFFFFDAD6)
val OnErrorContainer            = Color(0xFF93000A)

val NeonLimePrimary: Color      get() = if (ThemeState.isDarkMode) PureWhite else CharcoalPrimary
val OnNeonLime: Color           get() = if (ThemeState.isDarkMode) CharcoalPrimary else PureWhite
val ClayPrimary: Color          get() = if (ThemeState.isDarkMode) PureWhite else CharcoalPrimary
val OnClayPrimary: Color        get() = if (ThemeState.isDarkMode) CharcoalPrimary else PureWhite
val ClaySecondary: Color        get() = if (ThemeState.isDarkMode) SilverMuted else SlateSecondary
val ClayTertiary: Color         get() = if (ThemeState.isDarkMode) SlateSecondary else SilverMuted

// ── Backgrounds & Surfaces ────────────────────────────────────────────────
val DeepOliveBackground: Color    get() = if (ThemeState.isDarkMode) CharcoalPrimary else PureWhite
val CloudBlueBackground: Color    get() = if (ThemeState.isDarkMode) CharcoalPrimary else PureWhite
val SurfaceLight: Color           get() = if (ThemeState.isDarkMode) Color(0xFF1F2633) else PureWhite
val SurfaceContainerLowest: Color get() = if (ThemeState.isDarkMode) Color(0xFF131720) else PureWhite
val SurfaceContainerLow: Color    get() = if (ThemeState.isDarkMode) Color(0xFF1C222E) else Color(0xFFF7F8FA)
val SurfaceContainer: Color       get() = if (ThemeState.isDarkMode) Color(0xFF1F2633) else SoftGraySurface
val SurfaceContainerHigh: Color   get() = if (ThemeState.isDarkMode) Color(0xFF263346) else Color(0xFFECEFF4)
val SurfaceContainerHighest: Color get() = if (ThemeState.isDarkMode) Color(0xFF323E52) else Color(0xFFE2E6ED)

// ── Text & Content ────────────────────────────────────────────────────────
val OnSurface: Color        get() = if (ThemeState.isDarkMode) PureWhite else CharcoalPrimary
val OnSurfaceVariant: Color get() = if (ThemeState.isDarkMode) SilverMuted else SlateSecondary
val TextMuted: Color        get() = SilverMuted

// ── Borders & Shadows ─────────────────────────────────────────────────────
val NeonLimeGlow: Color     get() = if (ThemeState.isDarkMode) PureWhite else CharcoalPrimary

val ClayShadowOuter: Color      get() = if (ThemeState.isDarkMode) Color(0xFF000000).copy(alpha = 0.25f) else Color(0xFF171C26).copy(alpha = 0.06f)
val ClayShadowInnerLight: Color get() = if (ThemeState.isDarkMode) Color(0xFFFFFFFF).copy(alpha = 0.06f) else Color(0xFFFFFFFF).copy(alpha = 0.80f)
val ClayShadowInnerDark: Color  get() = if (ThemeState.isDarkMode) Color(0xFF000000).copy(alpha = 0.35f) else Color(0xFF171C26).copy(alpha = 0.05f)

val GradientPrimarySecondary: List<Color> get() = if (ThemeState.isDarkMode) listOf(PureWhite, SilverMuted) else listOf(CharcoalPrimary, SlateSecondary)
val GradientPrimaryTertiary: List<Color>  get() = if (ThemeState.isDarkMode) listOf(PureWhite, SlateSecondary) else listOf(CharcoalPrimary, SilverMuted)
val GradientBannerCard: List<Color>       get() = if (ThemeState.isDarkMode) listOf(Color(0xFF1F2633), CharcoalPrimary) else listOf(SoftGraySurface, PureWhite)