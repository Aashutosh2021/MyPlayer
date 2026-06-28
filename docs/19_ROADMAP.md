# Product Roadmap - MyPlayer

This roadmap outlines the future development phases for **MyPlayer**, showing how to implement pending requirements and expand existing architecture.

---

## Phase 1: DSP & Sound Customization

* **Objective**: Build the digital signal processing (DSP) features described in product plans but currently left as empty package templates.
* **Target Files**:
  * Create managers inside `com.example.myplayer.playback.dsp` (e.g. `EqualizerManager.kt`, `BassBoostManager.kt`, `LoudnessManager.kt`).
  * Create screen components inside `com.example.myplayer.ui.screens.dsp`.
* **Implementation Plan**:
  * Instantiate Android's `Equalizer`, `BassBoost`, and `LoudnessEnhancer` audio effects.
  * Retrieve the audio session ID from ExoPlayer (`player.audioSessionId`) and attach the effects.
  * Bind slider states to viewmodels, enabling real-time adjustments.
  * Build a dedicated DSP settings panel in Compose using the Claymorphic aesthetic, with convex dials and concave slider wells.

---

## Phase 2: Codebase Modernization (JSON & Scanner)

* **Objective**: Address technical debt, improve parsing reliability, and expand file importing features.
* **Target Tasks**:
  * **Migrate to Kotlinx Serialization**:
    * Declare strongly-typed data classes representing Innertube search requests and responses.
    * Replace manual `JSONObject` navigation in `InnertubeApi.kt` with compiler-generated serializers.
  * **Recursive File Scanner**:
    * Refactor `MediaScanner.scanFolder()` to traverse directory trees recursively.
    * Ensure files discovered in sub-folders are indexed with their relative path segments so the UI can represent parent-child folder structures in the Library folders tab.

---

## Phase 3: Immersive UI Additions

* **Objective**: Elevate the claymorphic styling to create an immersive listening experience.
* **Target Tasks**:
  * **Dynamic Album Art Theming**:
    * Use the Android Palette library to extract dominant, vibrant, and dark colors from the current song's album art.
    * Update `MyPlayerTheme` dynamically, replacing static blue accents with gradients derived from the album art.
  * **Audio Visualizer**:
    * Implement an audio visualizer on the `NowPlayingScreen` using ExoPlayer's FFT audio processor hook.
  * **Synced Lyrics Support**:
    * Integrate a lyrics scraper or parser (reading standard `.lrc` files or syncing from online APIs).
    * Render scrolling text on the `NowPlayingScreen`, aligned with the current playback timeline position.

---

## Phase 4: Syncing & Ecosystem Integration

* **Objective**: Expand MyPlayer from a local app into a connected client.
* **Target Tasks**:
  * **Cross-Device Playlist Syncing**:
    * Add a lightweight sync server connector (or WebDAV/Google Drive backup integrations) to back up custom playlists, history, and favorites.
  * **Cast Support**:
    * Integrate Google Cast and Bluetooth media router controls, allowing streams to be cast directly to speakers and TVs.
