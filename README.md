<div align="center">


---

## 📖 Overview

MyPlayer is an advanced Android music player built with a modern technology stack. It seamlessly blends local library playback with online YouTube streaming, powered by a robust MVVM architecture, Media3, and Jetpack Compose.

The application is heavily optimized for performance, boasting 120fps scrolling, zero main-thread blocking, and sub-50ms touch latency.

<div align="center">
  <br/>
  <i>[ UI Screenshots Placeholder ]</i>
  <br/>
</div>

---

## ✨ Major Features

- **Hybrid Playback Engine:** Play local files and stream online YouTube audio via the same unified Media3 `MusicController`.
- **Intelligent Autoplay:** A recommendation engine that queues up visually and musically related tracks infinitely once your queue ends, powered by the InnerTube API.
- **Real-Time Synced Lyrics:** Automatic lyric fetching (via LRCLIB) with smooth, highly-optimized auto-scrolling synced to the playback position.
- **Premium UI / UX:** A unique, tactile "Clay" Neumorphic design system with fluid animations, micro-interactions, and a floating mini-player.
- **Performance First:** Engineered for 120Hz displays. Features single-pass layout measurement, lifecycle-aware StateFlow collection, and strict thread isolation.
- **Offline Support:** Full support for downloading online tracks to local storage for offline listening.
- **Sleep Timer:** Integrated playback sleep timer.

---

## 🏗 Current Architecture

MyPlayer follows a strict **MVVM (Model-View-ViewModel)** pattern with unidirectional data flow (UDF) powered by Kotlin Coroutines and StateFlow.

### Technology Stack & Libraries

| Category                | Technology                                          |
| ----------------------- | --------------------------------------------------- |
| **Language**      | Kotlin                                              |
| **UI Toolkit**    | Jetpack Compose (Material3)                         |
| **Audio Engine**  | AndroidX Media3 (ExoPlayer, MediaSession)           |
| **Architecture**  | MVVM, Hilt (Dependency Injection)                   |
| **Concurrency**   | Kotlin Coroutines, Flow / StateFlow                 |
| **Local Data**    | Room Database, Jetpack DataStore                    |
| **Network**       | OkHttp3, YouTube InnerTube API (Reverse Engineered) |
| **Lyrics API**    | LRCLIB (Open Source, No API Key)                    |
| **Image Loading** | Coil                                                |

---

## 🚀 Installation & Build Instructions

### Prerequisites

- Android Studio Koala (or newer)
- JDK 17+
- Minimum SDK: **24 (Android 7.0)**
- Target SDK: **34 (Android 14)**

### Building from Source

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/MyPlayer.git
   ```
2. Open the project in Android Studio.
3. Sync the Gradle files.
4. Build and Run:
   ```bash
   ./gradlew assembleDebug
   ```

### Permissions Required

- `android.permission.INTERNET`: For streaming music, fetching recommendations, and downloading lyrics.
- `android.permission.FOREGROUND_SERVICE`: For background audio playback.
- `android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK`: Required in Android 14+ for Media3.

---

## 📁 Project Structure

```text
com.example.myplayer
├── data/               # Data layer (Room, APIs, Repositories, Recommendations)
├── di/                 # Hilt Dependency Injection modules
├── playback/           # Media3 MusicController and Background Service
├── ui/                 # Presentation layer
│   ├── common/         # Reusable Compose components
│   ├── components/     # Specific domain components (Recommendation cards, etc)
│   ├── navigation/     # NavHost and Routing
│   ├── screens/        # Main screens (Home, Library, NowPlaying, Search, Settings)
│   └── theme/          # Clay Design System modifiers, Colors, Typography
└── MainActivity.kt
```

*(For a full breakdown, see [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md))*

---

## 🚧 Known Limitations

- Background offline syncing of playlists is not currently supported.
- YouTube API (InnerTube) integration is unofficial and may be subject to undocumented rate limits if abused.

---

## 🔮 Future Roadmap

- Android Auto Support
- Wear OS Companion App
- Cross-device Playback Sync
- Desktop App Companion
- Advanced Material You integration (Dynamic Colors)

*(See [ROADMAP.md](ROADMAP.md) for details)*

---

## 📚 Documentation Index

Dive deeper into the engineering of MyPlayer:

- [Features Detail](FEATURES.md)
- [Architecture &amp; Diagrams](ARCHITECTURE.md)
- [Performance Optimizations](PERFORMANCE.md)
- [Recommendation Engine](RECOMMENDATION_ENGINE.md)
- [Security](SECURITY.md)
- [Changelog](CHANGELOG.md)

---

## 🤝 Contribution Guide

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code style, PR process, and development rules.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE_NOTICE.md](LICENSE_NOTICE.md) file for details.
