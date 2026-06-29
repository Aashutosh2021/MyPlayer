# User Interface (Clay Design System)

MyPlayer completely discards standard Material 3 flat design in favor of a bespoke, 3D, tactile "Clay/Glassmorphism" interface.

## Core Modifiers

To achieve this look without crippling rendering performance via Gaussian blurs, the app utilizes two custom `Modifier` extensions defined in `ClayModifiers.kt`.

### 1. `Modifier.claySurface`
Used to create raised, button-like elements. It utilizes `drawWithCache` to paint shadows:
- An outer drop shadow.
- An inner highlight on the top-left edge to simulate a light source.
- An inner dark gradient on the bottom-right edge to simulate curvature.

### 2. `Modifier.clayConcave`
Used to create sunken wells (like the album art container on the Now Playing screen). It reverses the gradients of the surface modifier to make the element appear pushed into the background.

## Performance Optimization in Compose

Custom draw modifiers can be extremely expensive if they re-allocate gradient brushes on every frame. 

To achieve 120fps scrolling:
- Both clay modifiers use `drawWithCache` instead of `drawBehind`. This means the complex gradients, shapes, and `Paint` objects are allocated exactly once, and cached until the layout bounds actually change.
- Lists (`LazyColumn`, `LazyRow`) use explicit height modifiers to prevent double-measurement passes.

## Navigation
Navigation is handled by Jetpack Navigation Compose. The application uses a single-activity architecture (`MainActivity`). The root composable is `MainScreen`, which hosts the `NavHost` and manages the floating `MiniPlayer` and `FloatingNavBar` overlays based on the current route.
