# 🎵 MyPlayer v3.6 — Release Notes

We are pleased to release **MyPlayer v3.6**! This release directly targets and eliminates the background and screen-off streaming errors that occurred when switching between apps or locking the device, ensuring uninterrupted online playback in all operating conditions.

---

## 🚀 What's Fixed & Improved in Version 3.6

---

### 🔋 Continuous Background & Screen-Off Audio Playback
Resolved the issue where online streaming would fail with *"Network error while streaming audio. Stream server could not be reached"* or *"Failed to load audio stream"* whenever the app was sent to Recent Apps, switched to another app, or the screen was turned off.

- **Service-Level Wake & Wi-Fi Locks**: `MusicService` now maintains a high-priority `PARTIAL_WAKE_LOCK` and `WifiLock` (`WIFI_MODE_FULL_LOW_LATENCY`) throughout playback, buffering, and track resolution. CPU sleep and Wi-Fi power-saving throttling will no longer sever active audio streams.
- **Persistent Foreground Service Mode**: Ensured `MusicService` remains in foreground mode during stream extraction, prefetching, and network recovery states, preventing Android 12+ background execution restrictions from terminating streaming sockets.
- **Battery Optimization Exemption Support**: Added `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` permission to safeguard playback against aggressive background battery savers on OEM skins (ColorOS, Realme UI, OxygenOS, HyperOS).

---

### 🔁 Uninterrupted Online Queue Transitions & Next-Track Prefetching
Fixed a queue boundary defect where advancing to subsequent online tracks in the background would halt playback.

- **Full Online Queue Registration**: `MusicController` now registers the entire online playlist with `PlaybackRouter` on playback start, allowing background prefetching to resolve upcoming tracks before the currently playing song ends.
- **Seamless Track Transitions**: Updated `PlaybackRouter` to avoid aborting pre-resolution on dynamically indexed items, guaranteeing that next songs are ready in buffer prior to `STATE_ENDED`.

---

### 🛡️ Resilient Transient Network Recovery & Socket Reconnection
- **Transient Drop Protection**: Network hiccup recovery in `MusicController` now differentiates between actual HTTP 403 stream expirations and momentary socket switches (e.g., switching between Wi-Fi and 5G). Existing valid stream URLs are reused before attempting costly re-extractions.
- **Progressive Retries**: Recovery attempts now follow a multi-stage backoff and will not prematurely terminate on the first null response.
- **Enhanced Load Retries & Timeouts**: Increased ExoPlayer stream read/connect timeouts to 30s and raised `minLoadableRetryCount` to 5 in `PlayerModule`.
- **ExoPlayer Singleton Guarantee**: Annotated ExoPlayer dependency provider with `@Singleton` to guarantee consistent state across services and controllers.

---

### ⏱️ UI Timestamp & Playback Reset Polish
- **Clean State Reset**: Resolved an issue where stopping or exhausting playback would display "No song playing" alongside stale duration and position timestamps (e.g., 3:37 / 3:11). `stopPlayback()` now resets all position and duration counters to `0:00`.

---

## 📦 Changes at a Glance

| Component | Improvement |
| :--- | :--- |
| **`MusicService`** | Added service-level WakeLock and Wi-Fi lock; retained foreground state across resolution and recovery. |
| **`PlaybackRouter`** | Fixed dynamic queue pre-resolution so next tracks prefetch before reaching `STATE_ENDED`. |
| **`MusicController`** | Passed full queue to `PlaybackRouter`; improved transient error recovery without premature retries abort; reset timestamps on stop. |
| **`PlayerModule`** | Added `@Singleton` to `provideExoPlayer`; increased HTTP timeouts to 30s and load error retries to 5. |
| **`AndroidManifest.xml`** | Added `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` permission. |
| **`app/build.gradle.kts`** | Bumped version to `3.6` (`versionCode = 11`). |

---

## 📥 Download & Links

- **Website**: [https://aashutosh2021.github.io/MyPlayer/](https://aashutosh2021.github.io/MyPlayer/)
- **Releases Archive**: [https://aashutosh2021.github.io/MyPlayer/releases.html](https://aashutosh2021.github.io/MyPlayer/releases.html)
- **Direct APK Download**: [Download MyPlayer v3.6 APK](https://github.com/Aashutosh2021/MyPlayer/releases/download/version3.6/app-release.apk)

---

*Thank you for using MyPlayer! Star our repository and share your feedback on GitHub.*
