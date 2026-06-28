# Technical Requirements Document (TRD) - MyPlayer

## 1. Architectural Overview
MyPlayer follows the **MVVM (Model-View-ViewModel)** architectural pattern with a clear separation of concerns. The app is structured into layers:
- **UI Layer**: Jetpack Compose screens and ViewModels.
- **Domain/Repository Layer**: Repositories that abstract data sources (local vs. online).
- **Data Layer**: Room Database, Preferences DataStore, and Network APIs.
- **Service Layer**: A foreground `MediaSessionService` for persistent audio playback.
- **Security Layer**: Native C++ bridge, environment integrity checks, string encryption, and R8/ProGuard obfuscation to prevent reverse engineering and tampering.

## 2. Technical Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **DI Framework**: Hilt
- **Database**: Room
- **Persistence**: Preferences DataStore
- **Playback Engine**: Media3 (ExoPlayer)
- **Concurrency**: Kotlin Coroutines & Flow
- **Background Tasks**: WorkManager
- **Security & Obfuscation**: R8/ProGuard, Android Keystore, JNI/NDK (C++)

## 3. Detailed Component Analysis

### 3.1 Data Layer
- **Local Storage**:
    - **Room Database**: Managed by `AppDatabase`.
    - **Entities**: `SongEntity` (metadata), `PlaylistEntity` & `PlaylistSongCrossReference` (M:N relationship), `RecentHistoryEntity`, `FavoriteEntity`, `FolderEntity`, and `DownloadedSongEntity`.
    - **DAOs**: Specialized interfaces for querying songs, playlists, and search history.
- **Settings**: `SettingsDataStore` handles application-wide preferences.

### 3.2 Online & Network Layer
- **API Integration**: Uses the **YouTube Music Innertube API** for searching songs and metadata.
- **Stream Resolution**: Integrates **NewPipeExtractor** to resolve direct audio URLs from YouTube, bypassing bot detection.
- **Download Pipeline**:
    - `DownloadWorker` (CoroutineWorker) handles background downloads.
    - It extracts the stream URL, downloads the file, and updates the `DownloadedSongEntity` in the local database.

### 3.3 Playback & Audio Engine
- **Playback Service**: `MusicService` (extends `MediaSessionService`) hosts the `ExoPlayer` and `MediaSession`.
- **Controller**: `MusicController` acts as a singleton bridge, exposing `StateFlow` for the UI to observe playback state.

### 3.4 UI Layer
- **Navigation**: A sealed class `Screen` defines routes. `NavHost` manages transitions between Home, Search, Library, Settings, NowPlaying, DSP, and Downloads.
- **UI Components**:
    - **MainScreen**: Host for navigation and the floating overlay.
    - **MiniPlayer**: Floating component for immediate playback control.
    - **FloatingNavBar**: Bottom navigation for core app sections.
    - **Glass Components**: Custom `GlassCard`, `NeonButton`, and `NeonProgressBar` implementing a Glassmorphism aesthetic.
- **Styling**: Custom modifiers in `Modifiers.kt` (e.g., `glassMorphism`, `antigravityGlow`) provide consistent visual effects.
- **State Management**: ViewModels use `collectAsStateWithLifecycle` to bind repository flows to the UI safely.

### 3.5 Security Layer
- **Anti-Tampering**: Native C++ bridge (`SecurityNativeBridge`) detecting Frida, rooted environments, and emulators via standard heuristics.
- **Application Integrity**: `SignatureVerifier` validates the APK signature at runtime against the expected release signing certificate.
- **Obfuscation**: Aggressive R8/ProGuard rules are applied, renaming classes, shrinking resources, and stripping metadata.
- **Data Encryption**: `StringEncryptionManager` uses XOR and Android Keystore AES-GCM to hide sensitive API URLs and keys from the DEX strings table.
- **Anti-Debugging**: Prevents attaching debuggers via `Debug.isDebuggerConnected()` and checking `ApplicationInfo` flags.

## 4. Dependency Injection (Hilt)
- **Modules**:
    - `DatabaseModule`: Provides Room database and DAOs.
    - `NetworkModule`: Provides OkHttp and API services.
    - `PlayerModule`: Provides `ExoPlayer` and playback controllers.
    - `CacheModule`: Manages caching strategies.

## 5. Key External Dependencies
- **Media3**: Core playback, session, and UI components.
- **Room**: Local data persistence.
- **Hilt**: Dependency injection.
- **Coil**: Asynchronous image loading for album art.
- **NewPipeExtractor**: YouTube stream extraction.
- **WorkManager**: Reliable background downloads.