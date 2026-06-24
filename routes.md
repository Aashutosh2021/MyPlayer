# Routing Map - MyPlayer

The application uses a single-activity architecture with **Jetpack Compose Navigation**. The navigation graph is hosted in `MainScreen.kt` and is driven by the `Screen` sealed class.

## Routes Table

| Route | Composable | ViewModel | Purpose | Auth Required |
| :--- | :--- | :--- | :--- | :--- |
| `home` | `HomeScreen` | `HomeViewModel` | Dashboard with most played, recent songs, and favorites. | No |
| `search` | `SearchScreen` | `SearchViewModel` | Local and Online music discovery. | No |
| `library` | `LibraryScreen` | `LibraryViewModel` | Access to local songs, folders, and playlists. | No |
| `downloads` | `DownloadsScreen` | `DownloadsViewModel` | Management of songs downloaded from the web. | No |
| `settings` | `SettingsScreen` | `SettingsViewModel` | App preferences and download folder configuration. | No |
| `now_playing` | `NowPlayingScreen` | `MainViewModel` | Full-screen playback controls and audio metadata. | No |
| `playlist_detail/{playlistId}` | `PlaylistDetailScreen` | `PlaylistDetailViewModel` | View and manage songs within a specific playlist. | No |

## Navigation Logic
- **Entry Point**: `MainActivity` $\rightarrow$ `MainScreen` $\rightarrow$ `NavHost`.
- **Start Destination**: `Screen.Home.route`.
- **Navigation UI**: 
    - `FloatingNavBar`: Provides top-level switching between Home, Search, Library, and Downloads.
    - `MiniPlayer`: Located in the overlay; provides a direct shortcut to the `NowPlaying` screen.
- **Route Visibility**: The `FloatingNavBar` is hidden when the user is on the `NowPlaying` screen or the `PlaylistDetail` screen to maximize screen real estate.
