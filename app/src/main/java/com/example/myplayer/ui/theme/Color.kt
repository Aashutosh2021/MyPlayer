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

// ── Mochy palettes (mirrors ui.mochy values; hex hardcoded to avoid a cycle) ──
// Night (dark)
private val MochyBlack         = Color(0xFF070709)
private val MochySurface       = Color(0xFF141418)
private val MochySurfaceHigh   = Color(0xFF1E1E24)
private val MochyCardBorder    = Color(0xFF282830)
private val MochyTextPrimary   = Color(0xFFF5F5F7)
private val MochyTextSecondary = Color(0xFF9A9AA6)
// Day (light)
private val MochyDayBackground = Color(0xFFFFFFFF)
private val MochyDaySurface    = Color(0xFFF7F7F9)
private val MochyDaySurfaceHigh = Color(0xFFEFEFF2)
private val MochyDayCardBorder = Color(0xFFE2E2E8)
private val MochyDayTextPrimary = Color(0xFF17171C)
private val MochyDayTextSecondary = Color(0xFF6B6B76)

// ── Theme Tokens (day/night adaptive for legacy & direct token callers) ──
val Primary: Color              get() = if (ThemeState.isDarkMode) PureWhite else MochyDayTextPrimary
val OnPrimary: Color            get() = if (ThemeState.isDarkMode) CharcoalPrimary else PureWhite
val PrimaryContainer: Color     get() = if (ThemeState.isDarkMode) Color(0xFF3A3A42) else Color(0xFFEDEDEF)
val OnPrimaryContainer: Color   get() = if (ThemeState.isDarkMode) PureWhite else MochyDayTextPrimary

val Secondary: Color            get() = if (ThemeState.isDarkMode) MochyTextSecondary else MochyDayTextSecondary
val OnSecondary: Color          get() = if (ThemeState.isDarkMode) CharcoalPrimary else PureWhite
val SecondaryContainer: Color   get() = if (ThemeState.isDarkMode) Color(0xFF1B1B20) else Color(0xFFF5F5F7)
val OnSecondaryContainer: Color get() = if (ThemeState.isDarkMode) PureWhite else MochyDayTextPrimary

val Tertiary: Color             = Color(0xFF8B5CF6)
val OnTertiary: Color           = PureWhite
val TertiaryContainer: Color    get() = if (ThemeState.isDarkMode) MochySurfaceHigh else MochyDaySurfaceHigh
val OnTertiaryContainer: Color  = PureWhite

val Background: Color           get() = if (ThemeState.isDarkMode) MochyBlack else MochyDayBackground
val OnBackground: Color         get() = if (ThemeState.isDarkMode) MochyTextPrimary else MochyDayTextPrimary
val Surface: Color              get() = if (ThemeState.isDarkMode) MochySurface else MochyDaySurface
val SurfaceVariant: Color       get() = if (ThemeState.isDarkMode) MochySurfaceHigh else MochyDaySurfaceHigh
val Outline: Color              get() = if (ThemeState.isDarkMode) MochyCardBorder else MochyDayCardBorder
val CardBorderOlive: Color      get() = Outline
val OutlineVariant: Color       get() = CardBorderOlive

val Error                       = Color(0xFFD32F2F)
val OnError                     = PureWhite
val ErrorContainer              = Color(0xFFFFDAD6)
val OnErrorContainer            = Color(0xFF93000A)

val NeonLimePrimary: Color      get() = Primary
val OnNeonLime: Color           get() = OnPrimary
val ClayPrimary: Color          get() = Primary
val OnClayPrimary: Color        get() = OnPrimary
val ClaySecondary: Color        get() = Secondary
val ClayTertiary: Color         get() = Secondary

// ── Backgrounds & Surfaces ────────────────────────────────────────────────
val DeepOliveBackground: Color    get() = Background
val CloudBlueBackground: Color    get() = Background
val SurfaceLight: Color           get() = Surface
val SurfaceContainerLowest: Color get() = if (ThemeState.isDarkMode) MochyBlack else MochyDayBackground
val SurfaceContainerLow: Color    get() = if (ThemeState.isDarkMode) Color(0xFF101014) else MochyDaySurface
val SurfaceContainer: Color       get() = if (ThemeState.isDarkMode) MochySurface else Color(0xFFF2F2F4)
val SurfaceContainerHigh: Color   get() = SurfaceVariant
val SurfaceContainerHighest: Color get() = Outline

// ── Text & Content ────────────────────────────────────────────────────────
val OnSurface: Color        get() = OnBackground
val OnSurfaceVariant: Color get() = Secondary
val TextMuted: Color        get() = Secondary

// ── Borders & Shadows ─────────────────────────────────────────────────────
val NeonLimeGlow: Color     get() = Primary

val ClayShadowOuter: Color      get() = if (ThemeState.isDarkMode) Color(0xFF000000).copy(alpha = 0.25f) else Color(0xFF171C26).copy(alpha = 0.06f)
val ClayShadowInnerLight: Color get() = if (ThemeState.isDarkMode) Color(0xFFFFFFFF).copy(alpha = 0.06f) else Color(0xFFFFFFFF).copy(alpha = 0.80f)
val ClayShadowInnerDark: Color  get() = if (ThemeState.isDarkMode) Color(0xFF000000).copy(alpha = 0.35f) else Color(0xFF171C26).copy(alpha = 0.05f)

val GradientPrimarySecondary: List<Color> get() = if (ThemeState.isDarkMode) listOf(PureWhite, SilverMuted) else listOf(CharcoalPrimary, SlateSecondary)
val GradientPrimaryTertiary: List<Color>  get() = if (ThemeState.isDarkMode) listOf(PureWhite, SlateSecondary) else listOf(CharcoalPrimary, SilverMuted)
val GradientBannerCard: List<Color>       get() = if (ThemeState.isDarkMode) listOf(MochySurfaceHigh, MochyBlack) else listOf(SoftGraySurface, PureWhite)
