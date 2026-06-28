# Public Method Index - MyPlayer

This index catalogs the critical public methods of the **MyPlayer** codebase, detailing their purpose, inputs, outputs, invocation context, and side effects.

---

## 1. Playback Controls (`MusicController`)

### `playSongs`
* **Purpose**: Sets up the play queue and triggers playback from a specific song index.
* **Parameters**:
  * `songs: List<SongEntity>` - List of local songs to load into the queue.
  * `startIndex: Int = 0` - The index to start playing.
* **Returns**: `Unit`
* **Who calls it**: `playSong()`, `playPlaylist()`, `playDownloadedSong()`, `MainViewModel`, `MiniPlayer`, ViewModels.
* **What it calls**: `MediaController.setMediaItems()`, `MediaController.prepare()`, `MediaController.play()`
* **Side Effects**: Clears `_songQueue` in memory, sets `_currentSong` to the item at `startIndex`, sets `_currentPosition` to `0L`, and publishes `isPlaying = true`.

### `playSong`
* **Purpose**: Dispatcher that resolves and plays a single playable song regardless of its type.
* **Parameters**: `song: PlayableSong` - Either Local, Downloaded, or Online.
* **Returns**: `Unit`
* **Who calls it**: `MainViewModel.playSong()`, screens, and list items.
* **What it calls**: `playSongs()`, `playDownloadedSong()`
* **Side Effects**: Redirects to specific player logic based on song sub-types. Logs warning if called with un-resolved Online song.

### `playOnlineSong`
* **Purpose**: Plays a streaming online track using a pre-resolved stream URL.
* **Parameters**: `song: OnlineSong` - Contains videoId, metadata, and decrypted streamUrl.
* **Returns**: `Unit`
* **Who calls it**: `OnlineSearchViewModel.streamSong()` (after stream URL extraction).
* **What it calls**: `MediaController.setMediaItem()`, `MediaController.prepare()`, `MediaController.play()`
* **Side Effects**: Clears local queue list, sets `_currentSong = null`, sets `_currentOnlineSong = song`, resets seeking timeline, and starts playback.

### `playPause`
* **Purpose**: Alternates playback between paused and running states.
* **Parameters**: None
* **Returns**: `Unit`
* **Who calls it**: `MainViewModel.playPause()`, `MiniPlayer`, `NowPlayingScreen`.
* **What it calls**: `MediaController.isPlaying`, `MediaController.pause()`, `MediaController.play()`
* **Side Effects**: Triggers UI state updates (`isPlaying = true/false`) and starts/stops progress timeline update jobs.

### `skipToNext`
* **Purpose**: Advances the player queue.
* **Parameters**: None
* **Returns**: `Unit`
* **Who calls it**: `MainViewModel.skipToNext()`, `NowPlayingScreen`, player widgets.
* **What it calls**: `MediaController.hasNextMediaItem()`, `MediaController.seekToNext()`, `MusicRepository.getAllSongs()`
* **Side Effects**: If there are no more songs in the current queue, queries `MusicRepository` and plays a random song from local library as a fallback.

### `startSleepTimer`
* **Purpose**: Configures a background countdown job to automatically pause playback after a set duration.
* **Parameters**: `minutes: Int` - Length of countdown.
* **Returns**: `Unit`
* **Who calls it**: `MainViewModel.startSleepTimer()`, `NowPlayingScreen` (timer dialog).
* **What it calls**: Coroutines `delay()`
* **Side Effects**: Launches `sleepTimerJob` which counts down every second, updates `_sleepTimerRemainingSeconds`, and calls `MediaController.pause()` when the timer expires.

---

## 2. API & Decryption (`InnertubeApi` & `StringEncryptionManager`)

### `search`
* **Purpose**: Submits queries to the YouTube Music Innertube search endpoint.
* **Parameters**:
  * `query: String` - Search string.
  * `continuationToken: String? = null` - Pagination token.
* **Returns**: `SearchPage` (containing list of parsed `OnlineSong` items and continuation token).
* **Who calls it**: `InnertubeSearchPagingSource.load()`
* **What it calls**: `OkHttpClient.newCall().execute()`
* **Side Effects**: Executes network I/O on `Dispatchers.IO`. Decrypts the endpoint URL and client parameters on first run.

### `getStreamUrl`
* **Purpose**: Resolves the direct media link for a YouTube track.
* **Parameters**: `videoId: String` - The YouTube track ID.
* **Returns**: `String?` - The direct `.m4a` or `.webm` media stream URL, or `null` if resolution fails.
* **Who calls it**: `OnlineSearchViewModel.streamSong()`, `OnlineSearchViewModel.downloadSong()`, `DownloadWorker.doWork()`
* **What it calls**: `NewPipeExtractor.audioStreams.maxByOrNull { it.bitrate }`
* **Side Effects**: Network connection blocking. Throws `YouTubeStreamBlockedException` if bot detection blocks are detected.

---

## 3. Local Library Management (`MusicRepository` & `MediaScanner`)

### `addFolder`
* **Purpose**: Registers a directory URI for music indexing and runs a scan.
* **Parameters**:
  * `uri: String` - Storage Access Framework folder URI.
  * `name: String` - Friendly name.
* **Returns**: `Unit`
* **Who calls it**: `LibraryViewModel.addFolder()`
* **What it calls**: `FolderDao.addFolder()`, `MediaScanner.scanFolder()`
* **Side Effects**: Inserts folder URI into `FolderEntity` table and triggers database writes inside `MediaScanner` for discovered files.

### `addSongToPlaylist`
* **Purpose**: Maps a song into a playlist.
* **Parameters**:
  * `playlistId: Long` - Target playlist.
  * `song: PlayableSong` - Song details.
  * `position: Int` - Ordering index.
* **Returns**: `Unit`
* **Who calls it**: `LibraryViewModel.addSongToPlaylist()`, `PlaylistDetailViewModel`
* **What it calls**: `PlaylistDao.insertSongToPlaylist()`, `SongDao.insertSongs()`
* **Side Effects**: Writes a link entry in `PlaylistSongCrossReference`. If the song is Online or Downloaded, its metadata is cached in `SongEntity` first so SQLite FK constraint checks pass.

### `scanFolder`
* **Purpose**: Scans a directory and indexes audio files.
* **Parameters**: `folder: FolderEntity` - Directory metadata.
* **Returns**: `Unit`
* **Who calls it**: `MusicRepository.addFolder()`, `MediaScanner.scanAllFolders()`
* **What it calls**: `DocumentFile.listFiles()`, `MediaMetadataRetriever.extractMetadata()`, `SongDao.insertSongs()`
* **Side Effects**: Disk I/O. Indexes songs by writing them to the SQLite `songs` table.

---

## 4. Background Downloads (`DownloadRepository` & `DownloadWorker`)

### `startDownload`
* **Purpose**: Enqueues an expedited worker task for background file fetching.
* **Parameters**: `song: OnlineSong` - Contains streamUrl and metadata.
* **Returns**: `Boolean` - `true` if scheduled successfully, `false` if streamUrl is missing.
* **Who calls it**: `OnlineSearchViewModel.downloadSong()`
* **What it calls**: `WorkManager.enqueueUniqueWork()`
* **Side Effects**: Starts background service. Instantiates `DownloadWorker` via WorkManager.

---

## 5. Security & Verification (`SecurityManager` & `TamperDetectionManager`)

### `initialize`
* **Purpose**: Entry point that starts all environment security checks.
* **Parameters**: None
* **Returns**: `Unit`
* **Who calls it**: `MyPlayerApplication.onCreate()`
* **What it calls**: `IntegrityManager.warmup()`, launches `runAllChecks()` in coroutine.
* **Side Effects**: Runs intensive CPU and I/O checks asynchronously. Publishes final results to `SecurityStatus` flow.

### `check` (in `TamperDetectionManager`)
* **Purpose**: Compares current APK status against release parameters.
* **Parameters**: None
* **Returns**: `TamperCheckResult`
* **Who calls it**: `SecurityManager.runAllChecks()`
* **What it calls**: `SignatureVerifier.verify()`, `ZipFile` parsing, installer checks.
* **Side Effects**: Performs disk I/O. Computes classes.dex CRC hash if configured.
