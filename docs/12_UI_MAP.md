# UI Map - MyPlayer

This map catalogs the user interface architecture of **MyPlayer**, including screens, Compose component hierarchies, navigation routes, button triggers, and ViewModel state bindings.

---

## 1. Global UI Shell: `MainScreen.kt`

* **Compose Component**: `MainScreen`
* **Route**: Root layout wrapper.
* **Layout Structure**:
  * `Scaffold` container.
  * `NavHost` containing all screen destinations.
  * `MiniPlayer` floating banner overlay.
  * `FloatingNavBar` bottom navigation panel.
* **Bindings & Observers**:
  * Binds `MainViewModel.currentSong` and `MainViewModel.currentOnlineSong` to determine if the `MiniPlayer` is visible.
  * Exposes navigation routes via the sealed class `Screen`.
  * Hides `FloatingNavBar` when on `NowPlaying` or `PlaylistDetail` screens.

---

## 2. Screens Catalog & Interactions

### Screen: Home
* **Route**: `Screen.Home.route` ("home")
* **Composable**: `HomeScreen`
* **ViewModel**: `HomeViewModel`
* **Layout Nodes**:
  * Header title.
  * **Most Played Row**: Horizontal scrolling card list.
  * **Recently Played List**: Vertical scrolling card list.
* **Interactions & Triggers**:
  * Clicking any song card -> invokes `MainViewModel.playSong(song)`.
* **State Bindings**:
  * `HomeViewModel.mostPlayedSongs` collected as state.
  * `HomeViewModel.recentSongs` collected as state.

---

### Screen: Search
* **Route**: `Screen.Search.route` ("search")
* **Composable**: `SearchScreen`
* **ViewModel**: `SearchViewModel` & `OnlineSearchViewModel`
* **Layout Nodes**:
  * Search text input field (claymorphic concave style).
  * Recent Search history chips.
  * Search results list (rendered using Paging 3 `LazyPagingItems`).
* **Interactions & Triggers**:
  * Typing text -> triggers 500ms query debounce.
  * Clicking search history chip -> fills input field and runs query.
  * Clicking result item -> invokes `OnlineSearchViewModel.streamSong(song)`.
  * Clicking download icon -> invokes `OnlineSearchViewModel.downloadSong(song)`.
* **State Bindings**:
  * `OnlineSearchViewModel.query` bound to input field text.
  * `OnlineSearchViewModel.searchResults` collected as lazy paging items.
  * `OnlineSearchViewModel.downloadProgress` observed to render progress bars on downloading cards.

---

### Screen: Library
* **Route**: `Screen.Library.route` ("library")
* **Composable**: `LibraryScreen`
* **ViewModel**: `LibraryViewModel`
* **Layout Nodes**:
  * Tab row: "Songs", "Folders", "Playlists".
  * **Songs Tab**: Vertical scroll of all local and downloaded tracks.
  * **Folders Tab**: Registered folder paths. "Add Folder" button.
  * **Playlists Tab**: Custom playlists list. "Create Playlist" floating action button.
* **Interactions & Triggers**:
  * Clicking song -> plays song immediately.
  * Clicking "Add Folder" -> opens Android system directory picker.
  * Clicking "Create Playlist" -> opens name input dialog.
  * Clicking playlist card -> navigates to `Screen.PlaylistDetail.route`.
* **State Bindings**:
  * `LibraryViewModel.hybridSongs` flow collected as state.
  * `LibraryViewModel.folders` flow collected as state.
  * `LibraryViewModel.playlists` flow collected as state.

---

### Screen: Downloads
* **Route**: `Screen.Downloads.route` ("downloads")
* **Composable**: `DownloadsScreen`
* **ViewModel**: `DownloadsViewModel`
* **Layout Nodes**:
  * Title header.
  * Downloaded files list.
  * Delete icon button on each file row.
* **Interactions & Triggers**:
  * Clicking song -> plays offline song using `MusicController.playDownloadedSong()`.
  * Clicking delete -> opens deletion confirmation dialog, deletes file, and removes DB entry.
* **State Bindings**:
  * `DownloadsViewModel.downloadedSongs` collected as state.

---

### Screen: Now Playing
* **Route**: `Screen.NowPlaying.route` ("now_playing")
* **Composable**: `NowPlayingScreen`
* **ViewModel**: `MainViewModel` (Global playback state)
* **Layout Nodes**:
  * High-res album artwork card.
  * Title & Artist text.
  * **Timeline Slider**: Tracks song progress; updates seeking indicator.
  * **Control Panel**: Play/Pause button, Skip Next, Skip Previous.
  * **Accents**: Favorite Heart icon, Sleep Timer Clock icon.
* **Interactions & Triggers**:
  * Dragging progress slider -> calls `MainViewModel.seekTo()`.
  * Clicking Heart -> calls `MainViewModel.toggleFavorite()`.
  * Clicking Clock -> opens Sleep Timer configuration dialog (15, 30, 45, 60 minutes options).
* **State Bindings**:
  * `MainViewModel.currentSong` / `currentOnlineSong` for metadata display.
  * `MainViewModel.currentPosition` and `MainViewModel.currentDuration` drive the timeline slider.
  * `MainViewModel.isPlaying` toggles the Play/Pause icon vector.
  * `MainViewModel.sleepTimerRemainingSeconds` displays countdown text on the Clock icon.

---

### Screen: Playlist Detail
* **Route**: `Screen.PlaylistDetail.route` ("playlist_detail/{playlistId}")
* **Composable**: `PlaylistDetailScreen`
* **ViewModel**: `PlaylistDetailViewModel`
* **Layout Nodes**:
  * Header title (playlist name).
  * Reorderable song list.
  * Delete/Remove button on each song item.
* **Interactions & Triggers**:
  * Dragging handles -> triggers position swaps.
  * Clicking song -> loads playlist queue starting from clicked song index.
* **State Bindings**:
  * `PlaylistDetailViewModel.playlistSongs` collected as state.
