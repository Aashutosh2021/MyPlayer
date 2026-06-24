# Project Memory: MyPlayer

## Project Overview
MyPlayer is a premium Android music player that integrates local audio file management with online streaming from YouTube Music. It distinguishes itself through a high-fidelity audio focus (DSP capabilities) and a modern "Glassmorphism" UI design.

## Business Purpose
The application solves the problem of fragmented music libraries by allowing users to:
1. Organize local files.
2. Discover and stream online music without a dedicated subscription.
3. Permanently download online streams for offline use.
4. Fine-tune audio output via an integrated DSP system.

## Tech Stack
- **Language**: Kotlin 2.0.21
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: MVVM (Model-View-ViewModel)
- **DI**: Hilt
- **Database**: Room (SQLite)
- **Persistence**: Preferences DataStore
- **Playback Engine**: Media3 (ExoPlayer)
- **Concurrency**: Kotlin Coroutines & Flow
- **Background Processing**: WorkManager (for downloads)
- **Networking**: OkHttp
- **API Integration**: YouTube Music Innertube API
- **Stream Extraction**: NewPipeExtractor (Bypasses bot detection/poToken)
- **Image Loading**: Coil

## Repository Structure
```text
.
├── app/
│   ├── src/main/java/com/example/myplayer/
│   │   ├── data/
│   │   │   ├── download/         # DownloadWorker, DownloadService
│   │   │   ├── local/            # Room DB, Entities, DAOs, DataStore
│   │   │   ├── online/           # InnertubeApi, NewPipeDownloader, OnlineSong model
│   │   │   └── repository/       # Music, Download, HybridLibrary, OnlineSearch Repos
│   │ la/di/                      # Hilt Modules (Database, Network, Player, Cache)
│   │   ├── playback/             # MusicService, MusicController, DSP Managers
│   │   └── ui/                   # Compose UI (Common, Components, Navigation, Screens, Theme)
│   └── build.gradle.kts          # App-level dependencies and config
├── gradle/
│   └── libs.versions.toml        # Centralized dependency management
└── structure.md                 # Project tree overview
```

## System Architecture
The app uses a layered architecture:
**UI Layer** $\rightarrow$ **ViewModel Layer** $\rightarrow$ **Repository Layer** $\rightarrow$ **Data Source Layer** (Local DB / Remote API)

### Architecture Diagram
`User` $\rightarrow$ `Compose UI` $\rightarrow$ `ViewModel` $\rightarrow$ `Repository` $\rightarrow$ `[Room DB | Innertube API]`
`MusicController` $\rightarrow$ `MusicService (MediaSessionService)` $\rightarrow$ `ExoPlayer` $\rightarrow$ `Audio Hardware`

## Routing Map
The app uses a single-activity architecture with Jetpack Compose Navigation.
- **Root**: `MainScreen` hosts the `NavHost`.
- **Primary Destinations**: Home, Search, Library, Settings, Downloads.
- **Detail Destinations**: Now Playing, Playlist Details.
- **Navigation UI**: `FloatingNavBar` handles primary switches; `MiniPlayer` provides a shortcut to `NowPlaying`.

## Frontend Architecture
- **Root Component**: `MainScreen` wraps the entire app.
- **Global State**: Managed by `MainViewModel` (playback status, current song).
- **Theming**: Custom `MyPlayerTheme` using a dark palette with neon accents.
- **Visual Style**: Glassmorphism implemented via custom modifiers (`glassMorphism`) and dedicated components (`GlassCard`, `NeonButton`).
- **Screen Pattern**: Each screen has a corresponding ViewModel (e.g., `HomeViewModel`, `SearchViewModel`) injected via Hilt.

## Backend Architecture (Local-First)
Since there is no custom server, the "backend" consists of:
1. **Room Database**: The source of truth for all local and downloaded content.
2. **Innertube API**: The remote data provider for YouTube Music.
3. **NewPipeExtractor**: The logic layer that resolves YouTube's obfuscated stream URLs.
4. **WorkManager**: The background orchestration layer for file downloads.

## Database Architecture
- **Core Entity**: `SongEntity` (Local songs).
- **Online Bridge**: `DownloadedSongEntity` (Web songs saved locally).
- **Relationships**: `PlaylistEntity` $\xleftrightarrow{M:N}$ `SongEntity` via `PlaylistSongCrossReference`.
- **Utility Tables**: `FavoriteEntity`, `FolderEntity`, `RecentHistoryEntity`, `RecentSearchEntity`.

## Authentication Flow
- **Auth Status**: No user authentication required.
- **API Access**: Uses public YouTube Music web client credentials (public API key and client context).

## API Inventory
- **Innertube API (`/search`)**: Searches for music using `CLIENT_CONTEXT` (WEB_REMIX).
- **NewPipeExtractor**: Resolves `videoId` to direct `.m4a` or `.webm` audio stream URLs.

## Data Flow Diagrams
### 1. Online Playback Flow
`User Search` $\rightarrow$ `OnlineSearchRepository` $\rightarrow$ `InnertubeApi` $\rightarrow$ `OnlineSong` $\rightarrow$ `MusicController` $\rightarrow$ `InnertubeApi.getStreamUrl()` $\rightarrow$ `ExoPlayer`.

### 2. Download Flow
`User Click Download` $\rightarrow$ `DownloadRepository` $\rightarrow$ `WorkManager` $\rightarrow$ `DownloadWorker` $\rightarrow$ `InnertubeApi.getStreamUrl()` $\rightarrow$ `OkHttp Stream` $\rightarrow$ `File System` $\rightarrow$ `DownloadedSongDao` $\rightarrow$ `UI Update`.

## Environment Variables & Config
- **API Key**: Public YouTube Music web key embedded in `InnertubeApi`.
- **Client Contexts**: `WEB_REMIX` for search, `ANDROID_VR` for streaming.
- **Storage**: `PreferencesManager` stores the user-selected download folder URI.

## Third Party Integrations
- **YouTube Music (Innertube)**: Search and metadata.
- **NewPipeExtractor**: Stream URL decryption and extraction.
- **Google Media3**: Media session and playback.
- **Hilt**: Dependency injection.

## Feature Inventory
| Feature | Purpose | Key Files |
| :--- | :--- | :--- |
| **Hybrid Library** | Merge local and downloaded songs | `HybridLibraryRepository.kt` |
| **Streaming** | Stream from YT Music | `InnertubeApi.kt`, `MusicService.kt` |
| **Downloads** | Save web songs for offline | `DownloadWorker.kt`, `DownloadService.kt` |
| **DSP** | Audio effects (EQ, Bass) | `playback/dsp/` directory |
| **Sleep Timer** | Stop playback after delay | `MusicController.kt`, `NowPlayingScreen.kt` |
| **Local Scanning** | Import device music | `MediaScanner.kt` |

## Dependency Graph
- **Critical Path**: `MusicController` $\rightarrow$ `MusicService` $\rightarrow$ `ExoPlayer`.
- **Data Path**: `ViewModel` $\rightarrow$ `Repository` $\rightarrow$ `Dao/Api`.
- **DI Path**: `DatabaseModule`/`NetworkModule` $\rightarrow$ `Repositories` $\rightarrow$ `ViewModels`.

## Important Files
- `InnertubeApi.kt`: Core of the online functionality.
- `MusicController.kt`: Singleton bridge for playback state.
- `DownloadWorker.kt`: Logic for background file saving.
- `MainScreen.kt`: Application shell and navigation.

## Performance Notes
- **Paging 3**: Used in `OnlineSearchRepository` to prevent memory spikes during large searches.
- **Foreground Services**: Used for both playback (`MusicService`) and downloads (`DownloadService`) to prevent OS process death.
- **IO Dispatchers**: All database and network calls are wrapped in `Dispatchers.IO`.

## Technical Debt
- **Hardcoded API Key**: The public key is in the code; should be moved to a secure config or fetched.
- **Manual JSON Parsing**: Uses `JSONObject` instead of `kotlinx.serialization` for API responses.

## Development Workflow
- **Build System**: Gradle (Kotlin DSL).
- **DI**: Hilt for automated dependency injection.
- **UI**: Compose Previews used for component iteration.

## Deployment Process
- **Target**: Android APK/AAB.
- **Min SDK**: 28 (Android 9).
- **Target SDK**: 36.

## Known Risks
- **YouTube API Changes**: Dependence on an unofficial API means potential breakages if YouTube changes its response format.
- **Bot Detection**: NewPipeExtractor is used to mitigate this, but YouTube may implement stronger protections.