# Phase R11 – Playback Queue & Sequencing Fix Report

## 1. Overview of Fixes Applied
Phase R11 addressed sequencing failures, missing auto-next playback, and reversed/scrambled queue progression across Library, Downloads, and general playback. All changes were targeted, strictly respecting the architectural constraints (no new architecture, no UI redesign, no database schema changes, <= 20 files modified).

---

## 2. Detailed Fix Breakdown by File

### 1. DownloadedSongDao.kt
* **Path**: [app/src/main/java/com/example/myplayer/data/local/dao/DownloadedSongDao.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/data/local/dao/DownloadedSongDao.kt)
* **Function**: `getAllDownloadsSync(): List<DownloadedSongEntity>`
* **Change**: Added `ORDER BY downloadedAt DESC` to match the query used by the UI in `getAllDownloads()`.
* **Rationale**: Eliminates inversion between database query results and visible UI order.

### 2. MusicController.kt
* **Path**: [app/src/main/java/com/example/myplayer/playback/MusicController.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/playback/MusicController.kt)
* **Functions**:
  - `playDownloadedSongs(songs: List<DownloadedSongEntity>, startIndex: Int)` (NEW)
  - `playDownloadedSong(song: DownloadedSongEntity)` (UPDATED to delegate)
  - `skipToPrevious()` (UPDATED with 3000ms threshold & ExoPlayer fallback)
  - `skipToNext()` (UPDATED to remove destructive queue wiping)
  - `updateCurrentSong()` (UPDATED to sync with `controller.currentMediaItemIndex`)
* **Rationale**: Ensures the full list of downloaded songs is queued in the exact visible order; fixes previous and next button behavior; prevents queue destruction.

### 3. PlaybackRouter.kt
* **Path**: [app/src/main/java/com/example/myplayer/playback/PlaybackRouter.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/playback/PlaybackRouter.kt)
* **Function**: `playQueue(requests: List<PlayRequest>, startIndex: Int)`
* **Change**: Reordered background resolution to prioritize upcoming items: `(clampedIndex + 1 until requests.size) + (0 until clampedIndex)`.
* **Rationale**: Prevents delays and missing stream URLs when advancing forward in the queue.

### 4. MediaItemFactory.kt
* **Path**: [app/src/main/java/com/example/myplayer/playback/MediaItemFactory.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/playback/MediaItemFactory.kt)
* **Function**: `createMediaItem(item: MusicItem, resolvedUri: String?)`
* **Change**: Handled local file paths safely via `if (path.startsWith("/")) Uri.fromFile(File(path)) else Uri.parse(path)`.
* **Rationale**: Guarantees ExoPlayer can resolve local files without URI parse scheme issues.

### 5. LibraryScreen.kt
* **Path**: [app/src/main/java/com/example/myplayer/ui/screens/library/LibraryScreen.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/ui/screens/library/LibraryScreen.kt)
* **Change**: Updated callback from `onPlaySong: (PlayableSong) -> Unit` to `onPlaySong: (List<PlayableSong>, Int) -> Unit`. In `LazyColumn`, used `itemsIndexed(songs) { index, song -> SongItem(..., onClick = { onPlaySong(songs, index) }) }`.
* **Rationale**: Passes full library song list and clicked index to player.

### 6. MainScreen.kt
* **Path**: [app/src/main/java/com/example/myplayer/ui/screens/main/MainScreen.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/ui/screens/main/MainScreen.kt)
* **Change**: Updated `Screen.Library.route` to handle `onPlaySong = { songs, index -> viewModel.playPlaylist(songs, index) }`.
* **Rationale**: Seamlessly routes Library songs to existing playlist playback mechanism.

### 7. DownloadsViewModel.kt
* **Path**: [app/src/main/java/com/example/myplayer/ui/screens/downloads/DownloadsViewModel.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/ui/screens/downloads/DownloadsViewModel.kt)
* **Function**: Added `playSong(songs: List<DownloadedSongEntity>, startIndex: Int)`.
* **Rationale**: Bridges Downloads UI list to `MusicController.playDownloadedSongs`.

### 8. DownloadsScreen.kt
* **Path**: [app/src/main/java/com/example/myplayer/ui/screens/downloads/DownloadsScreen.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/ui/screens/downloads/DownloadsScreen.kt)
* **Change**: In `LazyColumn`, used `itemsIndexed(downloads) { index, song -> ... onClick = { viewModel.playSong(downloads, index) } }`.
* **Rationale**: Ensures the exact visible, filtered list and tapped index are passed to playback.

---

## 3. Verification & Safety
* **Files Modified**: 8 source files + 1 test file (9 total, well below 20-file constraint).
* **Database Compatibility**: Zero schema migrations; strictly modified query ordering.
* **Build & Test Verification**:
  - `testDebugUnitTest` executed cleanly with exit code 0.
  - `PlaybackQueueOrderTest` passed 100% of test cases.
* **Remaining Risks**: None. Existing UI state flows and coroutine scopes are preserved.
