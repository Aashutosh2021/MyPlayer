# 🎵 MyPlayer v3.5 — Release Notes

We are excited to release **MyPlayer v3.5**! This release brings major reliability improvements to long-session online streaming, fixes UI layout ergonomics on the Now Playing screen, and introduces direct redirection to our new official web portal for seamless app updates.

---

## 🚀 What's New in Version 3.5

---

### 🛡️ Resilient Online Streaming & Automatic Stream Refresh
Eliminated the critical bug where music playback would suddenly halt with *"Error: no internet connection"* during extended listening sessions or when resuming playback after several hours.

- **5-Hour Stream TTL Cache**: YouTube InnerTube audio stream URLs naturally expire after ~6 hours. MyPlayer now enforces a strict 5-hour Time-To-Live (TTL) cache validation in `PlaybackSourceResolver`. Stale URLs are proactively refreshed with fresh streaming links before playback starts.
- **Smart Stream Recovery on HTTP 403**: If ExoPlayer encounters an expired stream URL (HTTP 403 Forbidden), `PlaybackErrorHandler` correctly distinguishes it from a network blackout. Instead of failing permanently, it signals `PlaybackRouter` to re-resolve the audio source dynamically and resume playback from the exact same timestamp without interrupting your queue.
- **Zero Interruption Queueing**: Next-track prefetching now validates stream longevity so consecutive tracks in background playback remain uninterrupted.

---

### 🎨 Now Playing UI Ergonomics & Radial Controller Polish
- **Favorite Button Repositioning**: Resolved the layout overlap where the Favorite (heart) button was conflicting with the interactive 270° radial seek arc on the Now Playing screen.
- **Improved Touch Targets**: Restructured the action button row with balanced padding, ensuring effortless toggling of favorites without accidental track scrubbing.

---

### 🌐 Official Web Portal & Direct In-App Update Redirection
- **Direct Web Portal Updates**: The in-app update checker now redirects directly to the official MyPlayer Web Portal: [https://aashutosh2021.github.io/MyPlayer/](https://aashutosh2021.github.io/MyPlayer/).
- **In-App Update Dialog**: Whenever a new update is released, users are presented with a clean prompt directing them straight to the web portal with one tap.
- **Settings Navigation**: Added an **Official Website** quick-link under the *About* section in Settings for easy access to release archives, documentation, and feature spotlights.

---

## 🛠️ Technical Improvements & Stability

- **Test Suite Verification**: 100% passing test suites across all core modules (PlaybackRouter, PlaybackErrorHandler, PlaybackSourceResolver, Artwork, Lyrics, Sync Play).
- **Graceful Error Recovery**: Network and stream error categorization refined to avoid false "no internet" diagnostics when the network is fully active.
- **Android Target**: Built against Android SDK 36, compiled with Kotlin 2.1+, and packaged as `versionName = "3.5"` (`versionCode = 10`).

---

## 📦 Changes at a Glance

| Component | Improvement |
| :--- | :--- |
| **`PlaybackSourceResolver`** | Added 5-hour TTL check for cached InnerTube stream URLs; forces fresh resolution when expired. |
| **`PlaybackErrorHandler`** | Refactored error classification to identify HTTP 403 stream expiry and trigger transparent retry. |
| **`PlaybackRouter`** | Seamless stream re-resolution and playback resumption at current position without dropping queue. |
| **`NowPlayingScreen`** | Adjusted Favorite button alignment and padding to prevent overlap with the 270° radial arc. |
| **`UpdateChecker` & `UpdateNotifier`** | Updated update target URL to `https://aashutosh2021.github.io/MyPlayer/`. |
| **`SettingsScreen` & `MainActivity`** | Added web portal update dialog and direct "Official Website" navigation link. |
| **`build.gradle.kts`** | Bumped version to `3.5` (`versionCode = 10`). |

---

## 📥 Download & Links

- **Website**: [https://aashutosh2021.github.io/MyPlayer/](https://aashutosh2021.github.io/MyPlayer/)
- **Releases Archive**: [https://aashutosh2021.github.io/MyPlayer/releases.html](https://aashutosh2021.github.io/MyPlayer/releases.html)
- **Direct APK Download**: [Download MyPlayer v3.5 APK](https://github.com/Aashutosh2021/MyPlayer/releases/download/version3.5/app-release.apk)

---

*Thank you for using MyPlayer! Star our repository and share your feedback on GitHub.*
