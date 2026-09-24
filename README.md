# 🎵 MyPlayer

<div align="center">

![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-100%25-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2B%20Clean-FF6F00?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)
![Version](https://img.shields.io/badge/Version-3.1-00C853?style=for-the-badge)

<p align="center">
  <b>A modern, high-performance Android music player combining local library playback, seamless online streaming, intelligent recommendations, real-time synchronized lyrics, and innovative dual-earbud playback.</b>
</p>

</div>

---

## 🌟 Highlights & Key Features

- **🎧 Dual Bud Mode**: Play two separate, independent audio streams simultaneously—one in your left earbud and another in your right earbud with individual volume controls!
- **🌐 Hybrid Playback Engine**: Seamlessly switch between local device storage and online YouTube Music streaming through a unified Media3 / ExoPlayer pipeline.
- **🖼️ 1000×1000 HD Artwork Engine**: Multi-tier artwork resolution integrating iTunes Search API, Deezer API, and YouTube Music to automatically fetch studio-grade HD album covers.
- **📜 Synced Lyrics**: Real-time synchronized lyrics powered by LRCLIB with silky smooth auto-scrolling and manual line scrubbing.
- **🧠 Smart Recommendations & Autoplay**: Dynamic next-song recommendation engine that surfaces contextual tracks based on current playback genres and artists.
- **💾 Offline Downloads & Backup**: Download online audio directly to local storage for offline playback, and export/import full library and playlist states using JSON backup & restore.
- **🎨 Tactile Claymorphism UI**: Beautiful, custom neumorphic/claymorphic design system (`claySurface`, `clayConcave`) crafted with Jetpack Compose for 120Hz displays.
- **🛡️ Secure & Resilient**: Built-in runtime integrity checks, network security configuration, and graceful audio fallback handlers.

---

## 📸 Screenshots

<div align="center">
  <img width="23%" alt="Home Screen" src="https://github.com/user-attachments/assets/966f5fd6-a381-4da5-808b-637b045afbf6" />
  <img width="23%" alt="Search Screen" src="https://github.com/user-attachments/assets/2f28e3ed-e120-4f58-b404-813e1edb0bcc" />
  <img width="23%" alt="Library Screen" src="https://github.com/user-attachments/assets/1eba9d7b-810e-47b7-aa7d-6872a2f26167" />
  <img width="23%" alt="Downloads Screen" src="https://github.com/user-attachments/assets/7e14d874-19f5-4de8-85b8-c6820d3b9263" />
</div>

<div align="center">
  <img width="23%" alt="Now Playing Screen" src="https://github.com/user-attachments/assets/69d3fae6-801b-4a57-b4e3-86d2d01f618a" />
  <img width="23%" alt="Lyrics Screen" src="https://github.com/user-attachments/assets/e009a6dd-f55a-440e-abe5-fe127ff5efd6" />
  <img width="23%" alt="Audio Engine" src="https://github.com/user-attachments/assets/e9a0b5da-ca5d-4855-9796-dbca5868e3d3" />
  <img width="23%" alt="Queue / Settings" src="https://github.com/user-attachments/assets/f183c1ca-572e-4b2f-9e44-552f5777cb87" />
</div>

---

## 🏗️ Architecture & Technology Stack

MyPlayer is engineered following **Clean Architecture** principles and the **MVVM** pattern with strict Unidirectional Data Flow (UDF).

| Layer | Technologies / Libraries |
| :--- | :--- |
| **Language** | [Kotlin](https://kotlinlang.org/) (Coroutines, StateFlow, Flow) |
| **UI Toolkit** | [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3 & Custom Clay Design System |
| **Audio Core** | [AndroidX Media3](https://developer.android.com/guide/topics/media/media3) (`ExoPlayer`, `MediaSession`, `MediaController`) |
| **Dependency Injection** | [Hilt](https://developer.android.com/training/dependency-injection/hilt-android) / Dagger |
| **Local Persistence** | [Room Database](https://developer.android.com/training/data-storage/room) & Jetpack DataStore |
| **Networking** | [OkHttp3](https://square.github.io/okhttp/) & [Retrofit](https://square.github.io/retrofit/) |
| **Image Loading** | [Coil](https://coil-kt.github.io/coil/) (Asynchronous image loading & disk caching) |
| **Paging** | AndroidX Paging 3 |

---

## 📁 Repository Structure

```text
MyPlayer/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/myplayer/
│   │   │   │   ├── data/            # Room entities, DAOs, Repositories, Artwork & Lyrics APIs
│   │   │   │   ├── di/              # Hilt Dependency Injection modules
│   │   │   │   ├── dualbud/         # Dual-channel audio routing & earbud mixer
│   │   │   │   ├── playback/        # Media3 MusicController, Service & Source Resolvers
│   │   │   │   ├── security/        # Integrity and runtime security verification
│   │   │   │   ├── ui/              # Compose screens, components, theme & navigation
│   │   │   │   └── util/            # Helpers and background art fetchers
│   │   │   └── res/                 # Vector drawables, mipmaps, XML security configs
│   │   └── test/                    # Comprehensive unit tests (Artwork, Playback, Queue, etc.)
│   └── build.gradle.kts
├── docs/                            # Architectural design documents & feature breakdowns
├── gradle/                          # Gradle wrapper and version catalog (libs.versions.toml)
├── CONTRIBUTING.md                  # Open source contribution guidelines
├── LICENSE                          # MIT Open Source License
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio Koala (2024.1.1)** or newer
- **JDK 17** or **JDK 21** (or Android Studio Embedded JBR)
- Android SDK with:
  - `compileSdk`: **35**
  - `minSdk`: **26** (Android 8.0 Oreo)
  - `targetSdk`: **34**

### Clone & Build

1. Clone the repository:
   ```bash
   git clone https://github.com/Aashutosh2021/MyPlayer.git
   cd MyPlayer
   ```

2. Open the project in Android Studio, or build via the command line:
   ```bash
   ./gradlew assembleDebug
   ```

3. Run the unit test suite:
   ```bash
   ./gradlew testDebugUnitTest
   ```

---

## 🤝 Contributing

Contributions are warmly welcome! Whether it's reporting a bug, improving documentation, or proposing new features:

1. Check open issues or create a new discussion.
2. Fork the repository and create a feature branch (`git checkout -b feature/amazing-feature`).
3. Commit your changes and ensure all tests pass (`./gradlew testDebugUnitTest`).
4. Submit a Pull Request targeting `main`.

Please review our [Contribution Guide](CONTRIBUTING.md) for details on code style and architecture practices.

---

## 📄 License

This project is open-sourced under the [MIT License](LICENSE).
