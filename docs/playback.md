# Playback Pipeline (Media3)

MyPlayer uses the modern AndroidX Media3 library for audio playback. The architecture completely separates the UI from the audio engine, allowing playback to continue robustly in the background.

## Components

### 1. `MusicService`
This is a `MediaLibraryService` (a Foreground Service) that holds the actual `ExoPlayer` and `MediaSession` instances.
- It displays the persistent Android notification with playback controls.
- It handles audio focus (ducking when a notification chimes, pausing when a call comes in).
- It handles hardware media keys (Bluetooth headphones, steering wheel controls).

### 2. `MusicController`
This is a `@Singleton` wrapper around `MediaController` that the rest of the application (ViewModels) talks to. It acts as the remote control for the `MusicService`.
- Exposes `StateFlow`s for `isPlaying`, `currentPosition`, `currentSong`, `shuffleMode`, and `repeatMode`.
- **Thread Safety Constraint:** `MediaController.verifyApplicationThread()` enforces that all interactions (play, pause, seek, setMediaItem) must occur on the Main Thread.

## Hybrid Playback
The `MusicController` defines a unified `PlayableSong` sealed interface:
```kotlin
sealed interface PlayableSong {
    data class Local(val entity: SongEntity) : PlayableSong
    data class Online(val entity: OnlineSong) : PlayableSong
    data class Downloaded(val entity: DownloadedSongEntity) : PlayableSong
}
```
Depending on the type, it routes the playback either via a local `file://` URI or a remote `https://` streaming URL.

## Sleep Timer
The Sleep Timer runs as a coroutine `Job` inside the `MusicController`. It simply decrements a counter every 1000ms. When it hits zero, it calls `mediaController.pause()`. The remaining time is exposed as a `StateFlow` so the UI can display the countdown in real-time.
