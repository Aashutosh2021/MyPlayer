# Codebase Class Index - MyPlayer

This index catalogs the primary classes in the **MyPlayer** project, detailing their fields, interfaces, inheritance, constructors, and usage relationships.

---

## 1. Application & Activity

### `MyPlayerApplication`
* **Purpose**: Application controller and Hilt Android Entry Point.
* **Inheritance**: `android.app.Application`, `Configuration.Provider`, `coil.ImageLoaderFactory`
* **Fields**:
  * `workerFactory: HiltWorkerFactory` (Inject)
  * `securityManager: SecurityManager` (Inject)
* **Methods**: `onCreate()`, `getWorkManagerConfiguration()`, `newImageLoader()`
* **Used By**: Android OS (manifest launch target).
* **Uses**: `SecurityManager`, `AudioAlbumArtFetcher`

### `MainActivity`
* **Purpose**: Main host Activity.
* **Inheritance**: `androidx.activity.ComponentActivity`
* **Annotations**: `@AndroidEntryPoint`
* **Methods**: `onCreate()`
* **Used By**: Android OS
* **Uses**: `MainScreen` (Compose), `MyPlayerTheme`

---

## 2. Playback Engine

### `MusicService`
* **Purpose**: Foreground audio player service.
* **Inheritance**: `androidx.media3.session.MediaSessionService`
* **Annotations**: `@AndroidEntryPoint`
* **Fields**:
  * `player: ExoPlayer` (Inject)
  * `mediaSession: MediaSession?`
* **Methods**: `onCreate()`, `onGetSession()`, `onDestroy()`, `onTaskRemoved()`
* **Used By**: `MusicController`, Android OS
* **Uses**: `ExoPlayer`, `MediaSession`

### `MusicController`
* **Purpose**: Singleton player bridge manager.
* **Inheritance**: Implicit singleton (`@Singleton`)
* **Fields**:
  * `context: Context` (ApplicationContext Inject)
  * `musicRepository: MusicRepository` (Inject)
  * `mediaControllerFuture: ListenableFuture<MediaController>?`
  * `mediaController: MediaController?`
  * `_songQueue: MutableList<SongEntity>`
  * `_currentSong: MutableStateFlow<SongEntity?>`
  * `_currentOnlineSong: MutableStateFlow<OnlineSong?>`
  * `_isPlaying: MutableStateFlow<Boolean>`
  * `_currentPosition: MutableStateFlow<Long>`
  * `_currentDuration: MutableStateFlow<Long>`
  * `_sleepTimerRemainingSeconds: MutableStateFlow<Long>`
* **Methods**: `playSongs()`, `playSong()`, `playPlaylist()`, `playOnlineSong()`, `playDownloadedSong()`, `playPause()`, `skipToNext()`, `skipToPrevious()`, `seekTo()`, `startSleepTimer()`, `cancelSleepTimer()`, `getCurrentPosition()`
* **Used By**: ViewModels (`MainViewModel`, `HomeViewModel`, `SearchViewModel`, `OnlineSearchViewModel`)
* **Uses**: `MusicService`, `MusicRepository`

---

## 3. Remote Integration & Downloader

### `InnertubeApi`
* **Purpose**: YouTube Music API client wrapper.
* **Inheritance**: `@Singleton`
* **Fields**:
  * `okHttpClient: OkHttpClient` (Inject)
  * `enc: StringEncryptionManager` (Inject)
  * `BASE_URL: String` (Lazy string)
  * `API_KEY: String` (Lazy string)
  * `CLIENT_CONTEXT: String` (Lazy string)
  * `PLAY_CLIENT_CONTEXT: String` (Lazy string)
* **Methods**: `search()`, `getStreamUrl()`, `parseSearchResponse()`, `findMusicShelfContents()`, `parseMusicItem()`, `parseDurationToMs()`
* **Used By**: `OnlineSearchRepository`, `OnlineSearchViewModel`, `DownloadWorker`
* **Uses**: `NewPipeExtractor`, `OkHttpClient`, `StringEncryptionManager`

### `NewPipeDownloader`
* **Purpose**: HTTP driver bridge for NewPipeExtractor.
* **Inheritance**: `org.schabi.newpipe.extractor.downloader.Downloader`
* **Fields**:
  * `client: OkHttpClient`
* **Methods**: `execute()`, `post()`, `get()`
* **Used By**: `InnertubeApi` (inside `NewPipe.init`)
* **Uses**: `OkHttpClient`

---

## 4. Repositories

### `MusicRepository`
* **Purpose**: Offline database and scanner API manager.
* **Inheritance**: `@Singleton`
* **Fields**: `songDao`, `playlistDao`, `favoriteDao`, `folderDao`, `recentHistoryDao`, `mediaScanner`
* **Methods**: `getAllSongs()`, `getTrendingSongs()`, `getRecentlyAddedSongs()`, `getMostPlayedSongs()`, `searchSongs()`, `incrementPlayCount()`, `getAllFolders()`, `addFolder()`, `removeFolder()`, `rescanAllFolders()`, `getAllPlaylists()`, `getSongsInPlaylist()`, `createPlaylist()`, `renamePlaylist()`, `deletePlaylist()`, `addSongToPlaylist()`, `removeSongFromPlaylist()`, `reorderSongsInPlaylist()`, `getFavoriteSongs()`, `isFavorite()`, `toggleFavorite()`, `getRecentHistory()`, `addRecentHistory()`
* **Used By**: `MainViewModel`, `HomeViewModel`, `LibraryViewModel`, `SearchViewModel`, `MusicController`
* **Uses**: DAOs, `MediaScanner`

### `OnlineSearchRepository`
* **Purpose**: YouTube Music search operations wrapper.
* **Inheritance**: `@Singleton`
* **Fields**: `innertubeApi`, `recentSearchDao`
* **Methods**: `search()`, `getRecentSearches()`, `saveSearch()`, `deleteSearch()`, `clearAllSearches()`
* **Used By**: `OnlineSearchViewModel`
* **Uses**: `InnertubeApi`, `RecentSearchDao`, `InnertubeSearchPagingSource`

### `HybridLibraryRepository`
* **Purpose**: Combines database lists.
* **Inheritance**: `@Singleton`
* **Fields**: `songDao`, `downloadedSongDao`
* **Methods**: `getHybridLibrary()`, `searchHybridLibrary()`
* **Used By**: `LibraryViewModel`
* **Uses**: `SongDao`, `DownloadedSongDao`

### `DownloadRepository`
* **Purpose**: Drives download requests.
* **Inheritance**: `@Singleton`
* **Fields**: `context`, `downloadedSongDao`, `_downloadProgress`
* **Methods**: `startDownload()`, `deleteDownload()`, `getAllDownloads()`, `isDownloaded()`, `setDownloadProgress()`, `removeDownload()`
* **Used By**: `DownloadsViewModel`, `OnlineSearchViewModel`
* **Uses**: `WorkManager`, `DownloadedSongDao`

---

## 5. Security & Decryption

### `SecurityManager`
* **Purpose**: Orchestrates all security integrity tasks at startup.
* **Inheritance**: `@Singleton`
* **Fields**: `signatureVerifier`, `rootDetection`, `fridaDetection`, `emulatorDetection`, `hookDetection`, `antiDebug`, `tamperDetection`, `nativeBridge`, `integrityManager`
* **Methods**: `initialize()`, `runAllChecks()`
* **Used By**: `MyPlayerApplication`
* **Uses**: Security Sub-Managers

### `SecurityNativeBridge`
* **Purpose**: JNI Native Library bridge.
* **Inheritance**: `@Singleton`
* **Methods**: `runAllNativeChecks()`, `isFridaDetectedNative()`, `isDebuggerAttachedNative()`, `isRootedNative()`, `nativeRunAllChecks()`, `nativeIsFridaDetected()`, `nativeIsDebuggerAttached()`, `nativeIsRooted()`, `nativeGetBaseUrlBytes()`
* **Used By**: `SecurityManager`, `StringEncryptionManager`

### `StringEncryptionManager`
* **Purpose**: Runtime XOR compile-time constants decryptor and Keystore engine.
* **Inheritance**: `@Singleton`
* **Fields**: `cache: HashMap<String, String>`
* **Methods**: `baseUrl`, `ytmApiKey`, `clientWebName`, `clientWebVersion`, `clientVrName`, `encrypt()`, `decrypt()`, `deobfuscate()`, `getOrCreateKey()`
* **Used By**: `InnertubeApi`
* **Uses**: `AndroidKeyStore`

---

## 6. ViewModels

### `MainViewModel`
* **Purpose**: Roots global state bindings.
* **Inheritance**: `androidx.lifecycle.ViewModel`
* **Fields**: `musicController`, `musicRepository`, `currentSong`, `isPlaying`, `isFavorite`
* **Methods**: `playSong()`, `playPause()`, `toggleFavorite()`, `startSleepTimer()`, etc.
* **Used By**: `MainScreen`, `NowPlayingScreen`, `MiniPlayer`
