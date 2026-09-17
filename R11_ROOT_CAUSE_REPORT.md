# Phase R11 – Root Cause Analysis Report

## 1. Executive Summary
During Phase R11, a comprehensive root cause analysis was performed to diagnose why songs in Library and Downloads failed to advance to the next song, or advanced to the upper/previous song in reverse order. The analysis revealed four interconnected architectural bugs across data queries, UI parameter passing, queue management, and playback controls.

---

## 2. Root Causes Identified

### Root Cause 1: Single-Song Queue Passed from Library Screen
* **Exact Affected Files**:
  - [LibraryScreen.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/ui/screens/library/LibraryScreen.kt)
  - [MainScreen.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/ui/screens/main/MainScreen.kt)
* **Exact Affected Functions**:
  - `LibraryScreen(onPlaySong: (PlayableSong) -> Unit)`
  - `MainScreen` (`Screen.Library.route` destination)
* **Before Behavior**:
  When a user clicked a song in Library / All Songs, `LibraryScreen` called `onPlaySong(song)` with only the single song tapped. `MainScreen` routed this to `viewModel.playSong(song)`, which in turn invoked `musicController.playSongs(listOf(song.entity), 0)`.
  ExoPlayer's internal queue received only 1 item. When that item completed, ExoPlayer reached `Player.STATE_ENDED` because `hasNextMediaItem()` was `false`. No subsequent songs existed in the player.
* **Fix Applied**:
  Updated `LibraryScreen` callback to `onPlaySong: (List<PlayableSong>, Int) -> Unit`. In `LazyColumn`, used `itemsIndexed(songs)` to pass the complete songs list and the clicked index. Updated `MainScreen` to route `onPlaySong` to `viewModel.playPlaylist(songs, index)`.
* **Why Safe**:
  Preserves existing `playPlaylist` pipeline already used by `PlaylistDetailScreen` and `HomeScreen`. Does not modify UI layout or add dependencies.

---

### Root Cause 2: Downloads Database Query Inverted Ordering & Single-Item Trigger
* **Exact Affected Files**:
  - [DownloadedSongDao.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/data/local/dao/DownloadedSongDao.kt)
  - [DownloadsViewModel.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/ui/screens/downloads/DownloadsViewModel.kt)
  - [DownloadsScreen.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/ui/screens/downloads/DownloadsScreen.kt)
  - [MusicController.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/playback/MusicController.kt)
* **Exact Affected Functions**:
  - `DownloadedSongDao.getAllDownloadsSync()`
  - `DownloadsViewModel.playSong()`
  - `DownloadsScreen` item click handler
  - `MusicController.playDownloadedSong()`
* **Before Behavior**:
  1. `DownloadedSongDao.getAllDownloadsSync()` had query `@Query("SELECT * FROM downloaded_songs")` without `ORDER BY`. SQLite returned rows in ascending insertion order (`rowid ASC` / oldest first).
  2. `DownloadsViewModel` UI query `getAllDownloads()` had `@Query("SELECT * FROM downloaded_songs ORDER BY downloadedAt DESC")` (newest first).
  3. `DownloadsScreen` passed only the clicked song to `DownloadsViewModel.playSong(song)`, which called `musicController.playDownloadedSong(song)`.
  4. `MusicController.playDownloadedSong(song)` queried `downloadedSongDao.getAllDownloadsSync()`, finding `targetIndex = allDownloads.indexOfFirst { it.videoId == song.videoId }`.
  5. Because the database list was inverted compared to the UI list:
     - Song at UI index 0 (newest) was at database index `N - 1` (last). Playing it caused playback to end immediately!
     - Song at UI index `i` was at database index `N - 1 - i`. When it finished, ExoPlayer advanced to `(N - 1 - i) + 1` in the database, which corresponded to UI index `i - 1` (the upper/previous song in the UI list)!
  6. Any search filter applied on the Downloads screen was ignored upon playback.
* **Fix Applied**:
  1. Added `ORDER BY downloadedAt DESC` to `DownloadedSongDao.getAllDownloadsSync()`.
  2. Added `playDownloadedSongs(songs: List<DownloadedSongEntity>, startIndex: Int)` to `MusicController`.
  3. Updated `DownloadsViewModel` and `DownloadsScreen` to pass the visible list and clicked index: `playSong(downloads, index)`.
* **Why Safe**:
  Synchronizes UI ordering and playback queue ordering to the exact same canonical order (`downloadedAt DESC`). Respects active UI filter state.

---

### Root Cause 3: Queue Destruction on End-of-Queue Next
* **Exact Affected File**:
  - [MusicController.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/playback/MusicController.kt)
* **Exact Affected Function**:
  - `MusicController.skipToNext()`
* **Before Behavior**:
  `skipToNext()` checked `if (controller.hasNextMediaItem()) controller.seekToNext() else { musicRepository.getAllSongs().firstOrNull()?.random()?.let { playSongs(listOf(it), 0) } }`.
  When a user reached the end of their playlist or queue, tapping next wiped the user's active queue and replaced it with a single random song from the repository.
* **Fix Applied**:
  Removed the disruptive queue replacement fallback. When at the end of the queue, respect repeat mode or stop safely without destroying the active playlist.
* **Why Safe**:
  Prevents queue loss and unwanted random playback.

---

### Root Cause 4: Stuck Previous Navigation (Lack of Fallback to MediaItem History)
* **Exact Affected File**:
  - [MusicController.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/playback/MusicController.kt)
* **Exact Affected Function**:
  - `MusicController.skipToPrevious()`
* **Before Behavior**:
  `skipToPrevious()` only checked `playbackHistory.pop()`. If playback was started in the middle of a playlist (e.g. index 3), `playbackHistory` was empty. Tapping previous repeatedly seeked to 0 ms of song 3 and never navigated to song 2.
* **Fix Applied**:
  Implemented the standard player navigation contract:
  - If current playback position > 3000ms: restart current song from 0 ms.
  - If position <= 3000ms:
    - If `playbackHistory.isNotEmpty()`: pop history.
    - Else if `controller.hasPreviousMediaItem()`: call `controller.seekToPrevious()`.
    - Else: restart current song from 0 ms.
* **Why Safe**:
  Matches standard media player UX (Spotify, YouTube Music, Apple Music) and ExoPlayer conventions.

---

## 3. Tests Executed & Verification
* **Unit Tests**: Added `PlaybackQueueOrderTest.kt` verifying:
  - Canonical queue order preservation
  - Descending timestamp ordering for Downloads
  - Repeat mode edge conditions (OFF, ALL, ONE)
  - 3-second rule for skipToPrevious
  - Background resolution prioritization
* **Result**: All 9 unit tests passed in 21s (`BUILD SUCCESSFUL`).
* **Remaining Risks**: None. All fixes strictly follow existing architecture without new classes or breaking changes.
