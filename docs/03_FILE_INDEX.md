# Codebase File Index - MyPlayer

This index catalogs every important source file in the **MyPlayer** project, detailing its purpose, responsibilities, API footprints, dependencies, and risk level.

---

## 1. Application and Configuration

### [MyPlayerApplication.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/MyPlayerApplication.kt)
* **Purpose**: Core application class and DI entry point.
* **Responsibilities**: Initializes WorkManager configuration with Hilt injection support, sets up custom Coil ImageLoader components, and triggers the asynchronous environment security checks via `SecurityManager`.
* **Dependencies**: `HiltWorkerFactory`, `SecurityManager`, `AudioAlbumArtFetcher`
* **Public APIs**: `onCreate()`, `workManagerConfiguration`, `newImageLoader()`
* **Risk Level**: **Medium** (affects early startup hooks).

### [MainActivity.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/MainActivity.kt)
* **Purpose**: Single-activity entry point.
* **Responsibilities**: Calls `enableEdgeToEdge()` and sets up the root Compose tree using `MyPlayerTheme` and `MainScreen`.
* **Dependencies**: `MainScreen`, `MyPlayerTheme`
* **Public APIs**: `onCreate()`
* **Risk Level**: **Low** (delegates all UI and state directly to MainScreen).

---

## 2. Core Playback Layer

### [MusicController.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/playback/MusicController.kt)
* **Purpose**: Singleton controller bridging Compose UI and the background `MusicService`.
* **Responsibilities**: Manages the connection lifecycle to `MediaController`, maps `MediaItem` instances to DB entries, updates seekbar timelines, drives the sleep timer, and delegates play queue commands.
* **Dependencies**: `MusicRepository`, `MediaController`, `SessionToken`, `MusicService`
* **Public APIs**: `playSongs()`, `playSong()`, `playPlaylist()`, `playOnlineSong()`, `playDownloadedSong()`, `playPause()`, `skipToNext()`, `skipToPrevious()`, `seekTo()`, `startSleepTimer()`, `cancelSleepTimer()`, `getCurrentPosition()`
* **Private Methods**: `setupController()`, `startPositionUpdater()`, `stopPositionUpdater()`, `updateCurrentSong()`
* **Risk Level**: **High** (all audio controls route through this singleton).

### [MusicService.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/playback/MusicService.kt)
* **Purpose**: Foreground service running the ExoPlayer.
* **Responsibilities**: Exposes the `MediaSession` to controllers, initializes ExoPlayer with audio focus attributes, and handles swiped-away termination states.
* **Dependencies**: `ExoPlayer`, `MediaSession`
* **Public APIs**: `onCreate()`, `onGetSession()`, `onDestroy()`, `onTaskRemoved()`
* **Risk Level**: **High** (controls background service persistence and notification bindings).

---

## 3. Remote Data and Stream Decryption

### [InnertubeApi.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/data/online/InnertubeApi.kt)
* **Purpose**: Interface for YouTube Music searches and stream resolution.
* **Responsibilities**: Masks as web/VR clients, constructs JSON POST payloads, parses search query responses, and interfaces with `NewPipeExtractor` to resolve direct streaming URLs.
* **Dependencies**: `OkHttpClient`, `StringEncryptionManager`, `NewPipeExtractor`
* **Public APIs**: `search()`, `getStreamUrl()`
* **Private Methods**: `parseSearchResponse()`, `findMusicShelfContents()`, `parseMusicItem()`, `parseDurationToMs()`
* **Risk Level**: **High** (vital for search, streaming, and downloads).

### [NewPipeDownloader.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/data/online/NewPipeDownloader.kt)
* **Purpose**: Custom HTTP client bridge for NewPipeExtractor.
* **Responsibilities**: Translates NewPipe extractor HTTP requests into OkHttp queries.
* **Dependencies**: `OkHttpClient`
* **Public APIs**: `execute()`, `post()`, `get()`
* **Risk Level**: **Medium** (failures prevent the extractor from communicating with YouTube).

---

## 4. Repositories and Scan Engines

### [HybridLibraryRepository.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/data/repository/HybridLibraryRepository.kt)
* **Purpose**: Merges offline databases.
* **Responsibilities**: Combines local tracks (`SongDao`) and offline downloads (`DownloadedSongDao`) into a unified, sorted lists flow.
* **Dependencies**: `SongDao`, `DownloadedSongDao`
* **Public APIs**: `getHybridLibrary()`, `searchHybridLibrary()`
* **Risk Level**: **Medium** (drives Library and Searches UI).

### [DownloadRepository.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/data/repository/DownloadRepository.kt)
* **Purpose**: Orchestrates file downloads.
* **Responsibilities**: Enqueues workers, maps download payloads, sets progress percentages, and deletes downloaded items from disk.
* **Dependencies**: `DownloadedSongDao`, `WorkManager`, `DownloadWorker`
* **Public APIs**: `startDownload()`, `deleteDownload()`, `getAllDownloads()`, `isDownloaded()`, `setDownloadProgress()`, `removeDownload()`
* **Risk Level**: **Medium** (manages download schedules).

### [MediaScanner.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/data/repository/MediaScanner.kt)
* **Purpose**: Local disk scanner.
* **Responsibilities**: Reads metadata (title, artist, album, duration) using `MediaMetadataRetriever`, identifies supported formats (mp3, m4a, wav, flac, ogg), and inserts records into `SongDao`.
* **Dependencies**: `SongDao`, `FolderDao`, `MediaMetadataRetriever`
* **Public APIs**: `scanFolder()`, `scanAllFolders()`
* **Risk Level**: **Medium** (drives local library population).

### [MusicRepository.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/data/repository/MusicRepository.kt)
* **Purpose**: Local music database wrapper.
* **Responsibilities**: Controls folder importing, playlist creation, song reordering, favorites toggling, and play count tracking.
* **Dependencies**: `SongDao`, `PlaylistDao`, `FavoriteDao`, `FolderDao`, `RecentHistoryDao`, `MediaScanner`
* **Public APIs**: `getAllSongs()`, `getTrendingSongs()`, `getRecentlyAddedSongs()`, `getMostPlayedSongs()`, `createPlaylist()`, `addSongToPlaylist()`, `toggleFavorite()`, `addRecentHistory()`, etc.
* **Risk Level**: **Medium** (manages core Room DB operations).

---

## 5. Security & Verification Layer

### [SecurityManager.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/security/SecurityManager.kt)
* **Purpose**: Entry point and orchestrator for all security integrity checks.
* **Responsibilities**: Executes signature, root, hook, Frida, debug, and NDK JNI checks asynchronously. Aggregates results into `SecurityStatus` flow.
* **Dependencies**: `SignatureVerifier`, `RootDetectionManager`, `FridaDetectionManager`, `EmulatorDetectionManager`, `HookDetectionManager`, `AntiDebugManager`, `TamperDetectionManager`, `SecurityNativeBridge`, `IntegrityManager`
* **Public APIs**: `initialize()`, `securityStatus`
* **Private Methods**: `runAllChecks()`, `runSafe()`
* **Risk Level**: **High** (mistakes can cause false-positive app crashes).

### [SecurityNativeBridge.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/security/SecurityNativeBridge.kt)
* **Purpose**: JNI wrapper for `libmyplayer_security.so`.
* **Responsibilities**: Imports native NDK functions and provides Kotlin wrappers.
* **Dependencies**: `libmyplayer_security.so`
* **Public APIs**: `runAllNativeChecks()`, `isFridaDetectedNative()`, `isDebuggerAttachedNative()`, `isRootedNative()`
* **Risk Level**: **High** (JNI method names must match native implementation exactly).

### [native-lib.cpp](file:///e:/MyPlayer/app/src/main/cpp/native-lib.cpp)
* **Purpose**: C++ NDK library.
* **Responsibilities**: Implements obfuscated strings decryption, reads `/proc/self/maps` and `/proc/self/status` for TracerPid and Frida, performs TCP port scanning, checks `/system/bin/su` binaries, and prevents tracing via ptrace.
* **Dependencies**: CMake, Standard C/C++ libraries, JNI header, Linux/Android system headers.
* **Public APIs**: `nativeRunAllChecks()`, `nativeIsFridaDetected()`, `nativeIsDebuggerAttached()`, `nativeIsRooted()`, `nativeGetBaseUrlBytes()`
* **Risk Level**: **Critical** (compiled binary; mistakes cause native crashes).

### [StringEncryptionManager.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/security/StringEncryptionManager.kt)
* **Purpose**: Runtime keystore and XOR decryption manager.
* **Responsibilities**: Obfuscates critical API constants via XOR arrays at compile-time and decrypts them in-memory at runtime. Also generates AES-GCM keys via Android Keystore.
* **Dependencies**: `AndroidKeyStore`, `Cipher`, `SecretKey`
* **Public APIs**: `baseUrl`, `ytmApiKey`, `clientWebName`, `clientWebVersion`, `clientVrName`, `encrypt()`, `decrypt()`
* **Private Methods**: `deobfuscate()`, `getOrCreateKey()`
* **Risk Level**: **High** (corrupting XOR tables breaks the network layer).

### [SignatureVerifier.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/security/SignatureVerifier.kt)
* **Purpose**: APK signature check.
* **Responsibilities**: Retrieves current APK certificate hash at runtime, computes SHA-256 fingerprint, and validates it against expected release keys.
* **Dependencies**: `PackageManager`, `MessageDigest`
* **Public APIs**: `verify()`, `getSigningCertificateSHA256()`
* **Risk Level**: **High** (repackaging protection).

---

## 6. Local Database Layer

### [AppDatabase.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/data/local/AppDatabase.kt)
* **Purpose**: Room DB definition.
* **Responsibilities**: Declares tables, versioning (v2), DAO accessors, and applies `MIGRATION_1_2` migrations.
* **Dependencies**: Room framework, DAOs, Entities
* **Public APIs**: `songDao()`, `playlistDao()`, `favoriteDao()`, `downloadedSongDao()`, etc.
* **Risk Level**: **High** (schema changes require SQLite migrations).

---

## 7. UI Components & Theme

### [Modifiers.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/ui/theme/Modifiers.kt)
* **Purpose**: Custom Compose layout modifiers.
* **Responsibilities**: Implements `.claySurface()` and `.clayConcave()` layout filters via custom Inner/Outer shadows, Blur filters, and pressed State flows.
* **Dependencies**: Compose Graphics framework
* **Public APIs**: `claySurface()`, `clayConcave()`
* **Risk Level**: **Medium** (affects overall application aesthetics and performance).

### [GlassComponents.kt](file:///e:/MyPlayer/app/src/main/java/com/example/myplayer/ui/components/GlassComponents.kt)
* **Purpose**: Core claymorphic UI kit.
* **Responsibilities**: Exposes `ClayCard`, `ClayButton`, `ClayIconButton`, and `ClayProgressBar`.
* **Dependencies**: `Modifiers.kt`
* **Public APIs**: `ClayCard()`, `ClayButton()`, `ClayIconButton()`, `ClayProgressBar()`, `PremiumSectionHeader()`
* **Risk Level**: **Low** (reusable UI widgets).
