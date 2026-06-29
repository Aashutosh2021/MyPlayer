# Features List

This document represents the *currently implemented and working* feature set of MyPlayer as of RC-2.

---

## 🎵 Playback
- **Unified Hybrid Playback:** Seamlessly plays both local MP3s and online YouTube audio streams using the same unified Media3 engine. 
- **Foreground Service:** Audio continues playing in the background or when the screen is off via a dedicated MediaSession service.
- **Sleep Timer:** Allows users to set a timer (e.g., 15, 30, 60 minutes). At completion, playback automatically pauses.
- **Shuffle & Repeat:** Full support for `REPEAT_MODE_OFF`, `REPEAT_MODE_ALL`, `REPEAT_MODE_ONE`, and Shuffle queue generation.

## 📚 Library
- **Local Device Scanning:** Automatically discovers and parses MP3 files from Android device storage using `MediaStore`.
- **Playlists:** Users can create, edit, and manage custom playlists.
- **Recent / Most Played:** The Home Screen dynamically generates carousels based on user playback history.

## 🤖 Recommendations (Autoplay)
- **InnerTube Integration:** Fetches visually and musically related tracks for the currently playing song from YouTube.
- **Infinite Autoplay:** When the user-defined queue ends, the app automatically seamlessly transitions into playing intelligent recommendations.
- **Recommendation Queue UI:** Users can view the upcoming Autoplay queue directly from the "Up Next" tab on the Now Playing screen.

## 🎤 Lyrics
- **Real-Time Synced Lyrics:** Queries LRCLIB for `.lrc` format lyrics and highlights the active line dynamically in real-time synced to the playback clock.
- **Unsynced Fallback:** If synced lyrics are unavailable, displays plain text lyrics.
- **Regex Optimization:** Lyrics parsing is highly optimized, resolving brackets and noise keywords to increase matching accuracy with zero CPU stutter.

## 🎨 User Interface
- **Clay/Glassmorphism Design:** A unique, tactile UI design utilizing custom Compose modifiers (`claySurface`, `clayConcave`) to create 3D inner and outer shadows.
- **Floating Mini-Player:** A persistent, animated mini-player that hovers above the bottom navigation bar.
- **120Hz Optimized:** All lists (`LazyColumn`, `LazyRow`) use explicit height constraints and stable keys to guarantee zero-jank 120fps scrolling.

## 🔍 Search
- **Online YouTube Search:** A dedicated Search Screen allowing users to query the entire YouTube audio catalog natively.
- **Suggestions & Auto-complete:** Live query suggestions powered by the InnerTube API.

## ⚙️ Settings
- **Autoplay Toggle:** Users can disable the infinite Autoplay recommendation engine via a persistent DataStore switch.
- **Persistent Preferences:** All settings (Shuffle state, Repeat mode, Autoplay) survive app restarts.

## ⚡ Performance
- **Lifecycle-Aware State Collection:** All ViewModels expose `StateFlow` which the UI collects via `collectAsStateWithLifecycle()`, ensuring zero background CPU consumption.
- **Strict Thread Isolation:** UI rendering stays on `Dispatchers.Main`, heavy audio processing is handled by the `MediaController` IPC, and all DB/Network I/O is strictly forced to `Dispatchers.IO`.
- **Recomposition Reduction:** Synthetic objects and heavy calculations are cached via `remember` and `derivedStateOf`.

## 🔒 Security
- **Scoped Storage:** Obeys Android 10+ scoped storage guidelines for local file reading.
- **HTTPS Enforcement:** All network requests (InnerTube, LRCLIB, Coil Image Loading) are strictly HTTPS via OkHttp3.
