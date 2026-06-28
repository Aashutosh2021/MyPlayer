# Feature Map - MyPlayer

This feature map links user-facing functionalities (features) to their technical implementations, responsible files, layout coordinates, and business logic.

---

## 1. Local Library Playback

* **Description**: Scan, import, and play local audio files (MP3, M4A, WAV, FLAC, OGG) stored on the device.
* **Responsible Files**:
  * [MediaScanner.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/data/repository/MediaScanner.kt) - Scans folders and extracts metadata.
  * [SongDao.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/data/local/dao/SongDao.kt) - Manages song tables.
  * [MusicRepository.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/data/repository/MusicRepository.kt) - Repository bridge.
* **Related UI**:
  * `HomeScreen.kt` - Displays most-played, recently added, and local tracks.
  * `LibraryScreen.kt` - Displays lists of songs, folders, and custom playlists.
* **Business Logic**:
  * Users add folders via Storage Access Framework (SAF).
  * `MediaScanner` scans folder contents (flat scan only, ignoring sub-folders and explicit formats like "aac").
  * Exposes flows of `SongEntity` items, sorted alphabetically.
  * Triggers play queue loading in `MusicController` via Uri parsing.

---

## 2. Online Streaming (YouTube Music)

* **Description**: Search and stream tracks from YouTube Music without requiring user authentication.
* **Responsible Files**:
  * [InnertubeApi.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/data/online/InnertubeApi.kt) - Connects to YTM Innertube endpoints.
  * [NewPipeDownloader.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/data/online/NewPipeDownloader.kt) - Network downloader backend.
  * [OnlineSearchRepository.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/data/repository/OnlineSearchRepository.kt) - Coordinates search and search history.
* **Related UI**:
  * `SearchScreen.kt` - Search query input, recent queries, and search result list.
* **Business Logic**:
  * Sends POST requests to Innertube `/search` using public web credentials and a songs-only filter payload.
  * Parses JSON to list of `OnlineSong` objects.
  * When a song is selected, calls `NewPipeExtractor` to resolve watch links to raw audio URLs (preferring the highest bitrate codec).
  * Injects the resolved URL into `ExoPlayer` via `MusicController.playOnlineSong()`.

---

## 3. Offline Background Downloads

* **Description**: Saves online streams to local storage for offline playback.
* **Responsible Files**:
  * [DownloadWorker.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/data/download/DownloadWorker.kt) - Foreground CoroutineWorker managing downloads.
  * [DownloadRepository.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/data/repository/DownloadRepository.kt) - Enqueues download requests.
  * [DownloadedSongDao.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/data/local/dao/DownloadedSongDao.kt) - Manages download database.
* **Related UI**:
  * `DownloadsScreen.kt` - Displays offline songs and folder settings.
  * `SearchScreen.kt` - Triggers downloads and displays progress indicators.
* **Business Logic**:
  * Resolves stream URL, then enqueues an expedited background task in `WorkManager`.
  * Worker promotes itself to a foreground task with a persistent progress notification.
  * Downloads files in chunks using HTTP Range headers and custom user-agents to bypass YouTube bandwidth throttling.
  * Writes chunks to temporary files, renaming them to `.m4a` on completion.
  * Caches metadata in `downloaded_songs` table, enabling library views to list the track offline.

---

## 4. Playback & Queue Management

* **Description**: Singleton interface controlling current track, playback states, seeking, and countdown sleep timers.
* **Responsible Files**:
  * [MusicController.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/playback/MusicController.kt) - State flows and controls controller interface.
  * [MusicService.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/playback/MusicService.kt) - Foreground service running ExoPlayer.
* **Related UI**:
  * `MiniPlayer.kt` - Floating music control banner on all major screens.
  * `NowPlayingScreen.kt` - Immersive full-screen player dashboard.
* **Business Logic**:
  * Handles play/pause, skip next/prev, timeline seeking.
  * Emits StateFlow streams (`isPlaying`, `currentSong`, `currentPosition`, etc.) back to viewmodels.
  * Position update job polls player progress every 500ms when playing.
  * Runs a background coroutine timer that pauses ExoPlayer once the sleep timer count hits zero.

---

## 5. Playlist Customization

* **Description**: Create, rename, delete, and reorder custom song lists.
* **Responsible Files**:
  * [PlaylistDao.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/data/local/dao/PlaylistDao.kt) - Room queries for playlists and cross-references.
  * [PlaylistSongCrossReference.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/data/local/entity/PlaylistSongCrossReference.kt) - Junction table mapping.
* **Related UI**:
  * `LibraryScreen.kt` - Playlists tab and "Create Playlist" dialog.
  * `PlaylistDetailScreen.kt` - Reorderable list of playlist tracks.
* **Business Logic**:
  * Database-level mapping. Playlists map to songs via `PlaylistSongCrossReference` junction table.
  * Adding an Online track to a playlist inserts a stub entry in `songs` table first to satisfy foreign key constraints.
  * Reordering updates the `position` index column of all affected tracks in the junction table.

---

## 6. Security Hardening

* **Description**: Validates environment integrity to prevent runtime tampering, dynamic reverse engineering (Frida), and sideloading of modified APKs.
* **Responsible Files**:
  * [SecurityManager.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/security/SecurityManager.kt) - Coordinates validation tasks.
  * [native-lib.cpp](file:///e:/MyPlayer/app/src/main/cpp/native-lib.cpp) - JNI C++ library for anti-tamper logic.
  * [StringEncryptionManager.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/security/StringEncryptionManager.kt) - In-memory XOR decryption.
* **Related UI**: None (runs silently in the background at launch).
* **Business Logic**:
  * At launch, scans for Frida processes, TCP port blocks, debugger attachment states, root binaries, and APK certificate modifications.
  * Implements a soft-fail strategy: warns in developer logs and reports states, but does not crash.
  * Encrypts compile-time API key and URL constants as XOR-obfuscated byte tables to prevent static `strings` analysis of raw binary or DEX code.
