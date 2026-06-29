# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.0] - Release Candidate 2 (RC-2) - Present

### Added
- **Performance Investigation Sprint:** Comprehensive optimization across the entire application stack.
- `collectAsStateWithLifecycle()` implemented globally to pause background state collection and save battery.
- `Modifier.height` constraints added to nested `LazyRow` components in `HomeScreen` to prevent double-pass measurement during vertical scrolling.

### Changed
- `MusicController` coroutine scope restored to `Dispatchers.Main` to adhere to `MediaController` thread contracts, preventing playback crashes.
- IO-bound operations (Database queries, DataStore reads, Network requests) inside `MusicController` dispatched to `Dispatchers.IO` using `withContext` to prevent UI thread blocking.
- `LyricsTab` scroll animation optimized using `derivedStateOf` to prevent redundant scroll triggers and reduce main-thread backpressure.
- `MainScreen` synthetic `SongEntity` creation wrapped in `remember` to stop 120 allocations per second and reduce Garbage Collection overhead.

### Fixed
- Fixed 2-3 second touch delay across the application.
- Fixed scroll jank in `HomeScreen` and `LibraryScreen`.
- Fixed `MediaController method is called from a wrong thread` crash.

### Removed
- Removed all unfinished DSP and Audio Engine placeholders, resources, and dependencies to ensure a clean, production-ready codebase.

## [1.1.0] - Release Candidate 1 (RC-1)

### Added
- **Recommendation User Interface:** A premium, intuitive, and fast interface for the Recommendation System.
- Up Next tab in `NowPlayingScreen` featuring a `RecommendationQueuePreview`.
- Added dynamic "Clay/Glassmorphism" UI elements to recommendation cards.

### Changed
- Stabilized and hardened Recommendation Autoplay.
- Refactored `RecommendationCoordinator` to separate responsibilities, introducing `RecommendationPlaybackRepository` and `RecommendationCacheRepository`.

### Fixed
- Fixed edge cases and race conditions in the recommendation playback pipeline.

## [1.0.0] - Beta Release

### Added
- **Recommendation Engine (Backend):** Intelligent YouTube-powered recommendation system fetching visually and musically related tracks.
- **Autoplay Integration:** Seamless infinite playback when the user queue ends.
- **Lyrics Support:** Real-time synced and unsynced lyrics fetching via LRCLIB.
- **Hybrid Playback Pipeline:** Unified handling of local files and online streams via Media3.
- **Sleep Timer:** Fully integrated sleep timer with auto-pause.
- **Settings DataStore:** Persistent user preferences for Autoplay and Sleep Timer.
- **Search System:** YouTube InnerTube API integration for searching online music.
- **Offline Support:** Ability to download streamed tracks to local storage.

### Changed
- Migrated legacy playback system to modern AndroidX Media3 architecture.
- Re-architected application to strict MVVM with Hilt for Dependency Injection.
- Redesigned UI to a unique "Clay" Neumorphic design system with custom composable modifiers (`claySurface`, `clayConcave`).

## [0.1.0] - Initial Alpha

### Added
- Basic local file playback.
- Local library scanning and display.
- Simple Play/Pause functionality.
- Initial Jetpack Compose setup.
