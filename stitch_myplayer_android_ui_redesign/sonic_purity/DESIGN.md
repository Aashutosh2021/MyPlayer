---
name: Sonic Purity
colors:
  surface: '#f9f9f9'
  surface-dim: '#dadada'
  surface-bright: '#f9f9f9'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f3f3f4'
  surface-container: '#eeeeee'
  surface-container-high: '#e8e8e8'
  surface-container-highest: '#e2e2e2'
  on-surface: '#1a1c1c'
  on-surface-variant: '#45474b'
  inverse-surface: '#2f3131'
  inverse-on-surface: '#f0f1f1'
  outline: '#76777c'
  outline-variant: '#c6c6cc'
  surface-tint: '#595e6a'
  primary: '#000103'
  on-primary: '#ffffff'
  primary-container: '#171c26'
  on-primary-container: '#7f8491'
  inverse-primary: '#c2c6d4'
  secondary: '#555f71'
  on-secondary: '#ffffff'
  secondary-container: '#d9e3f9'
  on-secondary-container: '#5b6577'
  tertiary: '#000002'
  on-tertiary: '#ffffff'
  tertiary-container: '#161c26'
  on-tertiary-container: '#7e8491'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#dee2f0'
  primary-fixed-dim: '#c2c6d4'
  on-primary-fixed: '#171c26'
  on-primary-fixed-variant: '#424752'
  secondary-fixed: '#d9e3f9'
  secondary-fixed-dim: '#bdc7dc'
  on-secondary-fixed: '#121c2b'
  on-secondary-fixed-variant: '#3d4759'
  tertiary-fixed: '#dde2f0'
  tertiary-fixed-dim: '#c1c7d4'
  on-tertiary-fixed: '#161c26'
  on-tertiary-fixed-variant: '#414752'
  background: '#f9f9f9'
  on-background: '#1a1c1c'
  surface-variant: '#e2e2e2'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  display-md:
    fontFamily: Inter
    fontSize: 26px
    fontWeight: '700'
    lineHeight: 32px
    letterSpacing: -0.015em
  headline-lg:
    fontFamily: Inter
    fontSize: 22px
    fontWeight: '700'
    lineHeight: 28px
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
  title-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 22px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
  body-sm:
    fontFamily: Inter
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 18px
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.04em
  label-sm:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 14px
    letterSpacing: 0.02em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  margin: 1.25rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

The design system establishes a hyper-refined, music-first aesthetic tailored for modern Android audio applications. Rooted in tactile minimalism, it synthesizes the expansive white canvases of contemporary editorial interfaces with subtle, soft-depth surfaces reminiscent of soft UI and modern Jetpack Compose Material 3 ergonomics.

### Personality & Emotional Response
- **Pure & Uncluttered:** Eliminates visual noise so album artwork and musical metadata remain the absolute focal point.
- **Tactile & Fluid:** Employs soft, floating elevations and deep, rounded geometries that invite physical touch while maintaining snappy, low-friction interactions.
- **Sophisticated Monolith:** Relies on high-contrast monochromatic charcoal and off-white neutrals to present audio like modern gallery art.

### Target Audience
Audiophiles, everyday commuters, and modern mobile music listeners seeking an intuitive, distraction-free player that balances Android-native navigation patterns with tailored micro-interactions.

## Colors

The palette is strictly calibrated around high-contrast monochromatic tones with subtle cool undertones.

- **Primary (`#171C26`):** Deep charcoal slate. Used for primary text headings, high-priority interactive buttons (e.g., active Play pill button, primary circular playback toggles), and prominent floating containers such as the bottom mini-player.
- **Secondary (`#6C7689`):** Medium slate gray. Dictates secondary typography (artist names, track duration, metadata labels) and unselected state controls.
- **Tertiary (`#A4AAB7`):** Soft mist gray. Applied to subtle borders, inactive waveform bars, dividers, and disabled icon actions.
- **Subtle Surface (`#F5F7FA`):** Cool ambient gray. Deployed for secondary button fills (e.g., Shuffle button), track row highlight pills, and container wells.
- **Primary Surface (`#FFFFFF`):** Pure white canvas, framing album artwork with clean contrast.

## Typography

The typography leverages Inter across all hierarchy levels to create maximum legibility at small sizes and crisp optical balance in display views.

- **Weight Hierarchy:** Bold (700) is reserved for prominent track titles, album names, and key screen headers. Semi-bold (600) anchors active items, button text, and track titles within lists. Medium (500) and Regular (400) govern artists, time stamps, and metadata.
- **Letter Spacing:** Headlines utilize slight negative tracking (`-0.01em` to `-0.02em`) to bind large letterforms cleanly. Labels and time-stamped durations use neutral to slight positive tracking (`+0.02em` to `+0.04em`) to ensure instant legibility.
- **Android Metrics:** All line heights align to an 4dp/8dp vertical rhythm for fluid text rendering in native Compose text composables.

## Layout & Spacing

The layout is grounded in a flexible 8dp modular grid system, adapting smoothly across mobile viewport widths while guaranteeing standard 48dp minimum interactive touch targets.

### Screen Layout & Margins
- **Canvas Margins:** Mobile viewports use `1.25rem` (20dp) horizontal padding to give artwork breathing room while maintaining comfortable thumb boundaries.
- **Internal Gutters:** Columns and horizontal card arrangements enforce `1rem` (16dp) spacing.
- **Bottom Clearance:** Screens include dynamic bottom content padding (minimum `6rem` / 96dp) to prevent persistent mini-player and navigation bar overlap.

### Adaptive Behavior
- **Compact (Mobile < 600dp):** Single-column stacked layouts. Album hero artwork occupies up to 80% screen width in dedicated now-playing screens.
- **Medium / Expanded (Foldables & Tablets >= 600dp):** Switches to a 2-column split-view where artwork and primary playback controls persist on the left container while queue and tracks flow along the right column.

## Elevation & Depth

Visual depth combines layered tonal surfaces with soft, low-opacity ambient shadows to give elements a smooth, floating presence.

- **Floating Hero Cards:** Album artwork cards feature a deep, soft dual shadow: `0px 16px 32px -8px rgba(23, 28, 38, 0.16)` and `0px 4px 12px rgba(23, 28, 38, 0.06)`, grounding the focal artwork above the canvas.
- **Tactile Floating Controls:** The circular main play button and persistent charcoal mini-player float above viewport content using `0px 10px 24px -4px rgba(23, 28, 38, 0.24)`.
- **Subtle Well States:** Selected track items in list views utilize a soft container inset or `#F5F7FA` pill background with `0px 2px 8px rgba(0, 0, 0, 0.03)` without harsh border outlines.
- **Outlines & Dividers:** High-contrast lines are avoided; separators use hairline soft strokes (`rgba(164, 170, 183, 0.2)`) or pure whitespace.

## Shapes

The interface balances organic pill components with rounded rectangular containers to deliver a smooth tactile experience.

- **Album Artwork:** Standardized with large corner radiuses: `16dp` for mini-player/list thumbs, `20dp` to `24dp` for prominent album covers.
- **Interactive Buttons:** Action buttons (`Play`, `Shuffle`) and interactive badges feature full pill radii (`9999px` / `rounded-full`).
- **Containers & Mini-Player:** The persistent floating bottom player uses an envelope radius of `20dp` to `24dp` with inset rounded artwork corners (`12dp`).
- **Track Selection Indicator:** Soft active row capsules adopt `14dp` to `16dp` radius.

## Components

### Action Buttons
- **Primary Pill ('Play'):** Height 48dp, background `#171C26`, foreground `#FFFFFF`, typography `title-md`. Includes an inline play triangle icon with 8dp spacing.
- **Secondary Pill ('Shuffle'):** Height 48dp, background `#F5F7FA`, foreground `#171C26`, typography `title-md`. Accompanied by a shuffle icon. Pressed state transitions subtly to `#E9ECEF`.
- **Main Circular Play Toggle:** 64dp × 64dp solid `#171C26` circle with pure white playback icon, elevated with a soft floating shadow.

### Track List Row
- **Height & Spacing:** 60dp row container with 48dp minimum touch target.
- **Prefix:** Two-digit index number (`01`, `02`) in `body-sm` (`#6C7689`) or an animated 3-bar equalizer waveform when active.
- **Body:** Song title in `title-md` (`#171C26`), subtitle artist name and track length in `body-sm` (`#6C7689`).
- **Active State:** Encapsulated in a light `#F5F7FA` rounded container running full width with subtle padding.
- **Suffix:** Ellipsis menu button (`#A4AAB7`) with 48dp tap bound.

### Waveform Audio Scrubber
- Consists of vertical rounded soundwave bars.
- **Elapsed Bars:** `#171C26` solid fill.
- **Remaining Bars:** `#A4AAB7` muted fill.
- Flanked below by current timestamp and track duration in `label-sm` (`#6C7689`).

### Persistent Mini-Player
- **Container:** Floating horizontal capsule pinned above the bottom navigation bar. Background `#171C26`, foreground `#FFFFFF`, border-radius `20dp`, horizontal margin `16dp`, padding `8dp 16dp 8dp 8dp`.
- **Contents:** 44dp thumbnail artwork (`rounded-md`), track title and artist stacked in white/light gray, inline favorite heart icon, and pill-shaped white mini play/pause control button (`#FFFFFF` with `#171C26` icon).

### Bottom Navigation Bar
- **Dimensions:** 64dp height container set against `#FFFFFF`.
- **Destinations (4):** Home, Search, Library, Hotlist / History.
- **Icon States:** Selected item renders solid `#171C26` with bold indicator dot or icon weight; unselected items render in `#6C7689` outline.