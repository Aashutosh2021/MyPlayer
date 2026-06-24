---
name: Tactile Rhythm
colors:
  surface: '#faf9ff'
  surface-dim: '#d8d9e3'
  surface-bright: '#faf9ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f2f3fd'
  surface-container: '#ecedf7'
  surface-container-high: '#e7e7f1'
  surface-container-highest: '#e1e2ec'
  on-surface: '#191b22'
  on-surface-variant: '#424753'
  inverse-surface: '#2e3038'
  inverse-on-surface: '#eff0fa'
  outline: '#727785'
  outline-variant: '#c2c6d6'
  surface-tint: '#0059c6'
  primary: '#0057c2'
  on-primary: '#ffffff'
  primary-container: '#2c70e2'
  on-primary-container: '#fefcff'
  inverse-primary: '#afc6ff'
  secondary: '#6b38d4'
  on-secondary: '#ffffff'
  secondary-container: '#8455ef'
  on-secondary-container: '#fffbff'
  tertiary: '#006574'
  on-tertiary: '#ffffff'
  tertiary-container: '#008092'
  on-tertiary-container: '#f8fdff'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#d9e2ff'
  primary-fixed-dim: '#afc6ff'
  on-primary-fixed: '#001a43'
  on-primary-fixed-variant: '#004398'
  secondary-fixed: '#e9ddff'
  secondary-fixed-dim: '#d0bcff'
  on-secondary-fixed: '#23005c'
  on-secondary-fixed-variant: '#5516be'
  tertiary-fixed: '#a2eeff'
  tertiary-fixed-dim: '#2fd9f4'
  on-tertiary-fixed: '#001f25'
  on-tertiary-fixed-variant: '#004e5a'
  background: '#faf9ff'
  on-background: '#191b22'
  surface-variant: '#e1e2ec'
typography:
  display-lg:
    fontFamily: Geist
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 56px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Geist
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.01em
  headline-lg-mobile:
    fontFamily: Geist
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
  title-md:
    fontFamily: Geist
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-lg:
    fontFamily: Geist
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Geist
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-md:
    fontFamily: Geist
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.05em
rounded:
  sm: 0.5rem
  DEFAULT: 1rem
  md: 1.5rem
  lg: 2rem
  xl: 3rem
  full: 9999px
spacing:
  base: 4px
  xs: 8px
  sm: 12px
  md: 16px
  lg: 24px
  xl: 32px
  container-padding: 20px
  element-gap: 16px
---

## Brand & Style

The design system is centered on a premium **Claymorphic** aesthetic, specifically tailored for a tactile and immersive Android music experience. The brand personality is playful yet sophisticated, evoking a sense of "digital physicalness" where every UI element feels like a soft, matte plastic or clay object. 

The target audience is music enthusiasts who appreciate high-quality hardware aesthetics and expressive interfaces. The visual language utilizes heavy inner shadows to simulate 3D volume, combined with extremely soft, multi-layered outer shadows to create a sense of floating depth. The emotional response is one of comfort, high-quality craftsmanship, and intuitive interaction through physical metaphors.

## Colors

The palette is anchored by "Soft Cloud Blue" to provide a low-contrast, easy-on-the-eyes canvas that enhances the 3D clay effect. 

- **Primary (Electric Blue):** Used for main actions and active playback states.
- **Secondary (Vivid Violet):** Used for creative accents, genres, or secondary highlights.
- **Accent (Cyan):** Reserved for high-energy interactions, progress indicators, or "live" badges.
- **Surface Strategy:** Surfaces are slightly brighter than the background (`#F5F7FF`) to allow the 3D shadows to pop. 
- **Dark Mode:** In dark mode, the background shifts to "Deep Midnight." Surface colors should shift to a slightly lighter navy to maintain the clay effect through subtle top-down lighting (inner highlights).

## Typography

This design system utilizes **Geist** for its technical precision and clean, modern skeleton, which balances the "softness" of the clay UI. 

Headlines use heavy weights and tight letter spacing to command attention against large, puffy containers. Body text remains legible with generous line heights. For music player contexts, the `display-lg` style is intended for track titles on the "Now Playing" screen, while `label-md` is used for time stamps and metadata in all-caps for a premium, structured feel.

## Layout & Spacing

The layout follows a **fluid grid** with significant safe areas to allow 3D shadows to breathe without clipping. 

- **Grid:** A 12-column grid for desktop/tablet and a 4-column grid for mobile.
- **Margins:** 20px minimum screen margins to prevent the "puffy" edges of containers from touching the screen boundary.
- **Rhythm:** An 8px-based spacing system is used for most elements, but a 4px increment is allowed for tight metadata grouping (e.g., Artist Name vs. Album Name).
- **Safe Zones:** Components must have at least 12px of internal padding to accommodate the "inner shadow" visual without crowding the content.

## Elevation & Depth

Depth is the core differentiator of this design system. It is achieved through a three-layer shadow technique:

1.  **Outer Soft Shadow:** A large, diffused shadow (`blur: 40px`, `opacity: 10%`, `color: primary-shadow-tint`) that makes the element appear to float high above the background.
2.  **Inner Highlight:** A light, semi-transparent white inner shadow positioned at the top-left (e.g., `x: 4, y: 4, blur: 8`) to simulate a light source.
3.  **Inner Depth Shadow:** A darker, semi-transparent inner shadow positioned at the bottom-right (e.g., `x: -6, y: -6, blur: 12`) to create the "clay" volume effect.

**Hierarchy Tiers:**
- **Level 0 (Background):** Base cloud blue.
- **Level 1 (Cards/Lists):** Subtle clay volume, low elevation.
- **Level 2 (Buttons/Navigation):** High clay volume, medium elevation.
- **Level 3 (Active Knobs/Modals):** Maximum volume and high-contrast shadows.

## Shapes

The design system uses extreme roundedness to reinforce the "squishy" and friendly nature of clay. 

- **Primary Containers:** 32dp (rounded-lg).
- **Buttons & Small Cards:** 28dp (rounded-md).
- **Dynamic Elements:** Elements like the track progress bar and volume sliders use "Pill" shapes (maximum radius) to maintain the tactile metaphor.
- **Visual Rule:** Avoid sharp corners entirely; even "rectangular" album art should have a minimum radius of 16dp to match the system language.

## Components

### Buttons
Buttons should appear as raised clay "lozenges." 
- **Primary:** Electric Blue background, white text, with a prominent white inner highlight on the top edge. 
- **Interaction:** When pressed, the outer shadow disappears and the inner shadows invert to create a "pressed-in" or "concave" effect.

### Cards (Album/Track)
Cards use the `surface_color_hex`. They should have a soft 1px stroke in a slightly darker blue to define the edges before the shadow begins. Album art inside cards should be inset with a small "well" effect.

### Sliders & Knobs
The seeker bar is a deep "well" (concave) while the playhead/knob is a highly elevated "sphere" (convex). The knob should feature a subtle radial gradient to enhance the spherical 3D look.

### Navigation
The navigation bar is a floating "clay island" at the bottom of the screen. Active states are indicated not just by color, but by an "inner-pressed" depth shift for the icon, making the selected tab look like it has been physically pushed into the clay.

### Input Fields
Search bars should use the "concave" look, appearing as if they are carved into the surface of the app.