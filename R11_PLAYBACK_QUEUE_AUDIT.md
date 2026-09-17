# Phase R11 – Playback Queue Order & Sequencing Flow Audit

## Executive Summary
This document provides the complete end-to-end trace and audit of the playback flow across all application entry points (Library / All Songs, Downloads, Playlists, Favorites, Search results) in MyPlayer V2. It analyzes queue establishment, index tracking, MediaItem factory resolution, ExoPlayer state management, and the root causes behind playback sequencing failures.

---

## Complete Playback Flow Tracing (By Source)

### 1. Library / All Songs
* **UI Trigger**: User taps a song in [LibraryScreen.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/ui/screens/library/LibraryScreen.kt).
* **Previous Flow (Bugged)**:
  1. `LibraryScreen` called `onPlaySong(song)` with only the single `PlayableSong` clicked.
  2. [MainScreen.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/ui/screens/main/MainScreen.kt) passed `viewModel.playSong(song)`.
  3. `MainViewModel.playSong(song)` invoked `musicController.playSongs(listOf(song.entity), 0)`.
  4. ExoPlayer was provided a queue of size 1 (`setMediaItems(listOf(mediaItem), 0, 0L)`).
  5. When the song ended, ExoPlayer transitioned to `Player.STATE_ENDED` because `hasNextMediaItem()` was `false`.
  6. **Result**: Auto-next never played the subsequent visible songs in Library.
* **Audited & Fixed Flow**:
  1. `LibraryScreen` uses `itemsIndexed(songs)` and passes `(songs, index)` to `onPlaySong(songs, index)`.
  2. `MainScreen` routes `onPlaySong` to `viewModel.playPlaylist(songs, index)`.
  3. `MainViewModel.playPlaylist` calls `musicController.playSongs(songs.map { it.entity }, index)`.
  4. `MusicController.playSongs` passes the entire visible list and the clicked index into `playbackRouter.playQueue(requests, startIndex)`.
  5. ExoPlayer receives all `MediaItem`s. When song `i` finishes, ExoPlayer automatically advances to song `i + 1` seamlessly.

---

### 2. Downloads Section
* **UI Trigger**: User taps a downloaded song in [DownloadsScreen.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/ui/screens/downloads/DownloadsScreen.kt).
* **Previous Flow (Bugged)**:
  1. `DownloadsScreen` invoked `viewModel.playSong(song)` passing only the single item clicked.
  2. `DownloadsViewModel` called `musicController.playDownloadedSong(song)`.
  3. `MusicController.playDownloadedSong` queried `downloadedSongDao.getAllDownloadsSync()`.
  4. In `DownloadedSongDao.kt`, `getAllDownloadsSync()` was `@Query("SELECT * FROM downloaded_songs")` (no explicit `ORDER BY`). SQLite returned items in `rowid ASC` (oldest downloaded first).
  5. Meanwhile, the UI list in `DownloadsViewModel` queried `getAllDownloads()` which was sorted `ORDER BY downloadedAt DESC` (newest downloaded first).
  6. The queue was reloaded from the database in reverse order compared to the visible list!
  7. When song at UI index `i` finished, ExoPlayer advanced to index `k + 1` in the database order, which corresponded to `i - 1` (the upper song) in the visible UI list! If the user clicked the newest download at UI index 0, it was the very last item in the database list, so playback stopped immediately.
  8. Any active search query filter on the Downloads screen was discarded because the queue was loaded directly from the database.
* **Audited & Fixed Flow**:
  1. `DownloadedSongDao.kt` `getAllDownloadsSync()` is explicitly sorted: `ORDER BY downloadedAt DESC`.
  2. `DownloadsViewModel` provides `playSong(songs: List<DownloadedSongEntity>, startIndex: Int)`.
  3. `DownloadsScreen` uses `itemsIndexed(downloads)` and passes the visible (potentially filtered) list and tapped index `(downloads, index)`.
  4. `MusicController.playDownloadedSongs(songs, startIndex)` is invoked, ensuring ExoPlayer's queue matches the exact visible order and starts at the tapped index.

---

### 3. Playlists & Favorites
* **UI Trigger**: User taps a song in [PlaylistDetailScreen.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/ui/screens/playlist/PlaylistDetailScreen.kt).
* **Flow**:
  1. `PlaylistDetailScreen` invokes `onPlayAll(songs, index)` passing the list and selected index.
  2. `MainScreen.kt` passes this to `viewModel.playPlaylist(songs, index)`.
  3. ExoPlayer receives the full list and advances from `index` forward.
  4. Favorites uses the same `playPlaylist` pipeline, ensuring deterministic forward order.

---

### 4. Search Results
* **UI Trigger**: User taps an online or local search result in [SearchScreen.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/ui/screens/search/SearchScreen.kt).
* **Flow**:
  1. For local search results, `SearchScreen` calls `viewModel.playSong(song)` or `playPlaylist(results, index)`.
  2. For online results, `SearchViewModel` resolves stream URLs via `PlaybackSourceResolver` and passes requests to `MusicController`.
  3. Pre-resolution background job in `PlaybackRouter` pre-fetches upcoming stream URLs.

---

## Detailed Component Audit

### PlaybackRouter & Resolution Pipeline
* **File**: [PlaybackRouter.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/playback/PlaybackRouter.kt)
* **Function**: `playQueue(requests: List<PlayRequest>, startIndex: Int)`
* **Audit Finding**:
  - Previously, after immediately resolving the target song at `startIndex`, `PlaybackRouter` launched a background coroutine iterating `0 until requests.size` to resolve remaining items.
  - In a queue of 20 songs with `startIndex = 10`, it spent time resolving songs 0, 1, 2, ... before song 11! If the current song was short or skipped quickly, song 11 was not yet resolved.
* **Fix Applied**:
  - Reordered pre-resolution iteration to prioritize upcoming items: `(clampedIndex + 1 until requests.size) + (0 until clampedIndex)`.

### MediaItemFactory & URI Integrity
* **File**: [MediaItemFactory.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/playback/MediaItemFactory.kt)
* **Function**: `createMediaItem(item: MusicItem, resolvedUri: String?)`
* **Audit Finding**:
  - Absolute local file paths (e.g. `/storage/emulated/0/...` or `/data/...`) passed into `Uri.parse(path)` could fail URI scheme validation in ExoPlayer's DefaultMediaSourceFactory if the scheme `file://` was omitted.
* **Fix Applied**:
  - If `path.startsWith("/")`, the URI is built safely via `Uri.fromFile(File(path))`.

### ExoPlayer Queue Ownership & State Transitions
* **File**: [MusicController.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/playback/MusicController.kt)
* **Ownership Model**: **Option A (ExoPlayer owns queue advancement)**.
  - ExoPlayer holds the full list of `MediaItem`s.
  - Media item transitions occur automatically inside ExoPlayer when a song finishes.
  - `MusicController` listens to `onMediaItemTransition(mediaItem, reason)` and updates `_currentSong` and playback state.
* **Queue Corruption in skipToNext**:
  - Previously, `skipToNext()` had a fallback: if `controller.hasNextMediaItem()` was false, it fetched a random song from `musicRepository.getAllSongs().firstOrNull()?.random()` and called `playSongs(listOf(it.random()), 0)`.
  - This wiped the entire active queue whenever next was clicked at the end of a playlist.
  - Removed this queue-wiping fallback; now respects repeat mode and stops gracefully at end-of-queue.
* **Previous Navigation**:
  - `skipToPrevious()` previously only popped from `playbackHistory`. If the user began playback at index 3 in a 10-song playlist, `playbackHistory` was empty, so clicking previous repeatedly seeked to 0 ms without moving to index 2.
  - Fixed with the 3-second rule: if position > 3000ms, rewind to beginning; otherwise, if history exists pop history, otherwise if `controller.hasPreviousMediaItem()` call `controller.seekToPrevious()`.

---

## Verification Summary
- **Tests Executed**: Unit tests in `PlaybackQueueOrderTest.kt` verifying queue order preservation, descending downloads sorting, 3-second rewind rule, and repeat mode calculations.
- **Build Verification**: `testDebugUnitTest` passed with exit code 0.
