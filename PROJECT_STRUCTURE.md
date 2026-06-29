# Project Structure

MyPlayer is organized by feature and architectural layer. The root package is `com.example.myplayer`. 

As of the current RC-2 version, the codebase consists of **112 Kotlin files** spread across **41 packages**.

## Directory Tree

```text
com.example.myplayer
│
├── data/                                 # Data Layer (Repositories & Data Sources)
│   ├── local/
│   │   ├── datastore/                    # Jetpack DataStore (User Settings)
│   │   └── entity/                       # Room Database Entities
│   ├── lyrics/                           # LRCLIB API Integration
│   ├── online/                           # Network Models
│   ├── recommendation/                   # Autoplay Engine
│   │   ├── api/                          # InnerTube API network calls
│   │   ├── cache/                        # Recommendation Cache repository
│   │   ├── model/                        # Domain models for Recommendations
│   │   └── queue/                        # Up Next queue manager
│   └── repository/                       # Single Source of Truth Repositories
│
├── di/                                   # Dependency Injection Layer
│   └── AppModule.kt                      # Hilt Module definitions (DB, Network, Media)
│
├── playback/                             # Audio Engine Layer
│   ├── MusicController.kt                # Media3 wrapper singleton
│   ├── MusicService.kt                   # Media3 Foreground Service
│   └── PlaybackCompletionGuard.kt        # Edge-case prevention for Autoplay
│
├── ui/                                   # Presentation Layer (Jetpack Compose)
│   ├── common/                           # Reusable UI components (AlbumArt, Buttons)
│   ├── components/
│   │   └── recommendation/               # Specialized UI for Recommendation Queue
│   ├── navigation/                       # Jetpack Navigation Compose graphs
│   ├── screens/                          # Top-level UI Screens
│   │   ├── downloads/
│   │   ├── home/                         # Home Dashboard (Recent/Most Played)
│   │   ├── library/                      # Local Library and Playlists
│   │   ├── main/                         # Main Scaffold and FloatingNavBar
│   │   ├── nowplaying/                   # Playback UI, Lyrics Tab, Up Next Tab
│   │   ├── search/                       # Online YouTube Search
│   │   └── settings/                     # App Preferences (Autoplay toggle)
│   └── theme/                            # Theming and Design System
│       ├── Color.kt
│       ├── ClayModifiers.kt              # Custom Neumorphic shadow modifiers
│       ├── Theme.kt
│       └── Type.kt
│
└── MainActivity.kt                       # Single Activity Entry Point
```
