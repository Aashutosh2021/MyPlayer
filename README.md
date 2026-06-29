<div align="center">
<div align="center">
<hr />
<h2 id="--Overview">📖 Overview</h2>
<p>MyPlayer is an advanced Android music player built with a modern technology stack. It seamlessly blends local library playback with online YouTube streaming, powered by a robust MVVM architecture, Media3, and Jetpack Compose.</p>
<p>The application is heavily optimized for performance, boasting 120fps scrolling, zero main-thread blocking, and sub-50ms touch latency.</p>
<div align="center">
  <br/>
  <i>[ UI Screenshots Placeholder ]</i>
  <br/>
</div>
<hr />
<h2 id="--Major-Features">✨ Major Features</h2>
<ul>
<li><strong>Hybrid Playback Engine:</strong> Play local files and stream online YouTube audio via the same unified Media3 <code>MusicController</code>.</li>
<li><strong>Intelligent Autoplay:</strong> A recommendation engine that queues up visually and musically related tracks infinitely once your queue ends, powered by the InnerTube API.</li>
<li><strong>Real-Time Synced Lyrics:</strong> Automatic lyric fetching (via LRCLIB) with smooth, highly-optimized auto-scrolling synced to the playback position.</li>
<li><strong>Premium UI / UX:</strong> A unique, tactile &quot;Clay&quot; Neumorphic design system with fluid animations, micro-interactions, and a floating mini-player.</li>
<li><strong>Performance First:</strong> Engineered for 120Hz displays. Features single-pass layout measurement, lifecycle-aware StateFlow collection, and strict thread isolation.</li>
<li><strong>Offline Support:</strong> Full support for downloading online tracks to local storage for offline listening.</li>
<li><strong>Sleep Timer:</strong> Integrated playback sleep timer.</li>
</ul>
<hr />
<h2 id="--Current-Architecture">🏗 Current Architecture</h2>
<p>MyPlayer follows a strict <strong>MVVM (Model-View-ViewModel)</strong> pattern with unidirectional data flow (UDF) powered by Kotlin Coroutines and StateFlow.</p>
<h3 id="Technology-Stack---Libraries">Technology Stack &amp; Libraries</h3>
<table>
<thead>
<tr>
<th>Category</th>
<th>Technology</th>
</tr>
</thead>
<tbody>
<tr>
<td><strong>Language</strong></td>
<td>Kotlin</td>
</tr>
<tr>
<td><strong>UI Toolkit</strong></td>
<td>Jetpack Compose (Material3)</td>
</tr>
<tr>
<td><strong>Audio Engine</strong></td>
<td>AndroidX Media3 (ExoPlayer, MediaSession)</td>
</tr>
<tr>
<td><strong>Architecture</strong></td>
<td>MVVM, Hilt (Dependency Injection)</td>
</tr>
<tr>
<td><strong>Concurrency</strong></td>
<td>Kotlin Coroutines, Flow / StateFlow</td>
</tr>
<tr>
<td><strong>Local Data</strong></td>
<td>Room Database, Jetpack DataStore</td>
</tr>
<tr>
<td><strong>Network</strong></td>
<td>OkHttp3, YouTube InnerTube API (Reverse Engineered)</td>
</tr>
<tr>
<td><strong>Lyrics API</strong></td>
<td>LRCLIB (Open Source, No API Key)</td>
</tr>
<tr>
<td><strong>Image Loading</strong></td>
<td>Coil</td>
</tr>
</tbody>
</table>
<hr />
<h2 id="--Installation---Build-Instructions">🚀 Installation &amp; Build Instructions</h2>
<h3 id="Prerequisites">Prerequisites</h3>
<ul>
<li>Android Studio Koala (or newer)</li>
<li>JDK 17+</li>
<li>Minimum SDK: <strong>24 (Android 7.0)</strong></li>
<li>Target SDK: <strong>34 (Android 14)</strong></li>
</ul>
<h3 id="Building-from-Source">Building from Source</h3>
<ol>
<li>Clone the repository:
<pre><code class="language-bash">git clone https://github.com/yourusername/MyPlayer.git
</code></pre>
</li>
<li>Open the project in Android Studio.</li>
<li>Sync the Gradle files.</li>
<li>Build and Run:
<pre><code class="language-bash">./gradlew assembleDebug
</code></pre>
</li>
</ol>
<h3 id="Permissions-Required">Permissions Required</h3>
<ul>
<li><code>android.permission.INTERNET</code>: For streaming music, fetching recommendations, and downloading lyrics.</li>
<li><code>android.permission.FOREGROUND_SERVICE</code>: For background audio playback.</li>
<li><code>android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK</code>: Required in Android 14+ for Media3.</li>
</ul>
<hr />
<h2 id="--Project-Structure">📁 Project Structure</h2>
<pre><code class="language-text">com.example.myplayer
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
</code></pre>
<p><em>(For a full breakdown, see <a href="PROJECT_STRUCTURE.md">PROJECT_STRUCTURE.md</a>)</em></p>
<hr />
<h2 id="--Known-Limitations">🚧 Known Limitations</h2>
<ul>
<li>Background offline syncing of playlists is not currently supported.</li>
<li>YouTube API (InnerTube) integration is unofficial and may be subject to undocumented rate limits if abused.</li>
</ul>
<hr />
<h2 id="--Future-Roadmap">🔮 Future Roadmap</h2>
<ul>
<li>Android Auto Support</li>
<li>Wear OS Companion App</li>
<li>Cross-device Playback Sync</li>
<li>Desktop App Companion</li>
<li>Advanced Material You integration (Dynamic Colors)</li>
</ul>
<p><em>(See <a href="ROADMAP.md">ROADMAP.md</a> for details)</em></p>
<hr />
<h2 id="--Documentation-Index">📚 Documentation Index</h2>
<p>Dive deeper into the engineering of MyPlayer:</p>
<ul>
<li><a href="FEATURES.md">Features Detail</a></li>
<li><a href="ARCHITECTURE.md">Architecture &amp; Diagrams</a></li>
<li><a href="PERFORMANCE.md">Performance Optimizations</a></li>
<li><a href="RECOMMENDATION_ENGINE.md">Recommendation Engine</a></li>
<li><a href="SECURITY.md">Security</a></li>
<li><a href="CHANGELOG.md">Changelog</a></li>
</ul>
<hr />
<h2 id="--Contribution-Guide">🤝 Contribution Guide</h2>
<p>We welcome contributions! Please see <a href="CONTRIBUTING.md">CONTRIBUTING.md</a> for details on our code style, PR process, and development rules.</p>
<h2 id="--License">📄 License</h2>
<p>This project is licensed under the MIT License - see the <a href="LICENSE_NOTICE.md">LICENSE_NOTICE.md</a> file for details.</p>


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
