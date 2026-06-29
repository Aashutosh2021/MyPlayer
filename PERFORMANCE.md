# Performance Optimization

MyPlayer is engineered to run fluidly at 120fps with minimal impact on system resources. During the RC-2 Performance Investigation Sprint, several deep architectural bottlenecks were identified and resolved. 

This document outlines the specific optimizations implemented.

## 1. Thread Isolation & Dispatcher Enforcement
A common pitfall in Android media apps is allowing background polling (like playback position) or database reads to compete with the UI thread. 

- **The Problem:** The `MusicController` coroutine scope was initially set to `Dispatchers.Main` to satisfy `MediaController` constraints, but this caused heavy database queries (`getAllSongs()`) and DataStore reads to block frame rendering.
- **The Optimization:** We maintained the `Dispatchers.Main` scope to satisfy `MediaController.verifyApplicationThread()`, but surgically dispatched all IO-bound work (Room DB, DataStore, InnerTube Network) to `Dispatchers.IO` using `withContext`.
- **Result:** Zero UI blocking during Autoplay resolution or fallback song fetching.

## 2. Compose Recomposition Reduction
Jetpack Compose is highly efficient, but careless state reads can trigger hundreds of redundant recompositions.

### `derivedStateOf` for Lyrics Scrolling
- **The Problem:** The `currentPositionMs` state flows from the `MusicController` every 500ms. In the Lyrics tab, reading this state directly inside a `remember` block forced the active line index to recompute and trigger a scroll animation 120 times per minute, severely lagging the UI.
- **The Optimization:** We wrapped the line index calculation in `derivedStateOf`. The `LaunchedEffect` that triggers the scroll animation now only receives a state invalidation when the *actual lyric line changes*, not every 500ms.

### Memoization of Synthetic Objects
- **The Problem:** The floating `MiniPlayer` requires a `SongEntity`. When streaming online, a synthetic `SongEntity` was being created inline using `?:`. Because `MainScreen` recomposes on every clock tick, this resulted in 120 object allocations per second on the Main Thread.
- **The Optimization:** Wrapped the synthetic object creation in `remember(currentOnlineSong)`. It is now allocated exactly once per song change.

## 3. Lifecycle-Aware State Collection
- **The Problem:** `MainScreen` collected 7 different `StateFlow` streams using `collectAsState()`. This meant the UI continued to observe and process position updates and sleep timer ticks even when the app was backgrounded.
- **The Optimization:** Migrated globally to `collectAsStateWithLifecycle()`. All background state collection instantly halts when the Activity stops, drastically reducing background CPU and battery drain.

## 4. Single-Pass Measurement (Nested Lazy Layouts)
- **The Problem:** `HomeScreen` featured horizontal `LazyRow` components nested inside a vertical `LazyColumn` without explicit height constraints. Compose defaults to an "unbounded" measurement pass first, then a bounded pass, doubling the CPU cost for every scroll frame.
- **The Optimization:** Applied `Modifier.height(156.dp)` to the `LazyRow` components based on their exact content size. 
- **Result:** Restored 120fps scrolling on the Home screen.

## 5. Regex Hoisting
- **The Problem:** The `LyricsRepository` instantiated new `Regex` objects for parsing `.lrc` timestamps and cleaning noisy YouTube titles on every method call. JVM regex compilation is an expensive operation.
- **The Optimization:** Hoisted all `Regex` instances to `companion object` constants so they are compiled exactly once when the class is loaded.

## 6. Gated State Observation
- **The Problem:** The `NowPlayingScreen` collected `lyricsPosition` at the top level, passing it down to the `AnimatedContent` block. This meant the screen recomposed 120 times per minute even if the user was looking at the "Up Next" tab.
- **The Optimization:** Pushed the `collectAsStateWithLifecycle()` call inside the `tab == 0` branch. The flow is now completely unsubscribed when the lyrics tab is hidden.
