# Project Memory: MyPlayer

This document serves as the high-level entry point and Project DNA for the **MyPlayer** codebase. It provides the core identity, business goals, technology stack, constraints, and instructions for future AI agents and developers.

---

## 1. Project Summary & Purpose

**MyPlayer** is a premium, localized, and streaming-capable Android music player application. It integrates local file discovery and playback with streaming and downloading from YouTube Music.

### Business Goals
* **Unified Music Library**: Seamlessly merge offline (local files) and online (streaming/downloaded) music into a single cohesive player interface.
* **Tactile Design Language**: Implement a custom **Claymorphic** visual style that feels tactile, modern, and high-fidelity.
* **Offline Independence**: Provide background downloading capability so users can build an offline collection and play music with zero network connectivity.
* **Privacy & Security**: Enforce environment integrity and reverse-engineering checks to safeguard the app's streaming logic and decryption methods.

### Technical Stack
* **Language**: Kotlin 2.0.21
* **UI Framework**: Jetpack Compose (Material 3)
* **Dependency Injection**: Hilt
* **Database**: Room (SQLite)
* **Local Persistence**: Preferences DataStore
* **Audio Playback**: Media3 (ExoPlayer)
* **Concurrency**: Kotlin Coroutines & Flow
* **Background Tasks**: WorkManager
* **Network Interfacing**: OkHttp
* **Security & Obfuscation**: JNI/NDK C++ Bridge, R8/ProGuard, Keystore AES-GCM

---

## 2. Core Architecture

MyPlayer adheres to the **MVVM (Model-View-ViewModel)** architectural pattern. The system is split into three clean layers:

```
[ UI Layer (Compose Screens / ViewModels) ]
                     ↓
         [ Repository Layer ]
                     ↓
[ Data Layer (Room SQLite DB / Innertube HTTP API) ]
```

1. **UI Layer**: Compose screens observe reactive `StateFlow` bindings from Hilt-injected ViewModels. The visual theme uses a soft Cloud Blue background (`#EEF3FF`) and tactile, claymorphic card and button structures.
2. **Repository Layer**: Acts as the single point of contact for ViewModels. It abstracts the origin of data (e.g., merging local and downloaded lists into a single flow in `HybridLibraryRepository`).
3. **Data Layer**: Room handles local databases; OkHttp handles outbound connections; `InnertubeApi` handles search and metadata; and `NewPipeExtractor` extracts stream URLs.
4. **Decoupled Playback Service**: A foreground `MusicService` (derived from `MediaSessionService`) hosts a singleton `ExoPlayer` instance. ViewModels and screens interact with the service through the `MusicController` singleton.

---

## 3. Coding Conventions

All code in this project must adhere to the following standards:
* **Dependency Injection**: Always inject dependencies via Hilt constructors. Do not initialize database instances or network clients manually outside Hilt modules.
* **Reactive Data Flows**: Expose data from repositories and viewmodels as Kotlin `Flow` or `StateFlow` objects.
* **Coroutine Scopes**: Run I/O operations (database, files, network calls) on `Dispatchers.IO` using structured concurrency.
* **Compose State Collection**: Collect viewmodel state in Composables using `collectAsStateWithLifecycle()` to prevent memory leaks during lifecycle transitions.
* **Claymorphic Aesthetics**: UI containers must utilize the custom `.claySurface()` and `.clayConcave()` modifiers from `Modifiers.kt` instead of raw cards or borders.

---

## 4. Things NEVER to Modify (Strict Constraints)

> [!WARNING]
> The following components are critical for app security, streaming reliability, and core architecture. Do NOT modify them unless explicitly requested.

1. **Native Security Bridge (`libmyplayer_security.so` / `native-lib.cpp`)**:
   * Do not alter the native check methods (`nativeIsFridaDetected`, `nativeIsDebuggerAttached`, `nativeIsRooted`, `nativeRunAllChecks`).
   * Do not change JNI function signatures, as this will break link bindings with `SecurityNativeBridge.kt`.
2. **String Encryption & Key Storage (`StringEncryptionManager.kt`)**:
   * The rotating XOR keys and Base64 encryption configurations are matched against specific NDK parameters. Any change will break network connectivity.
3. **Signature Verification (`SignatureVerifier.kt`)**:
   * Do not comment out or remove signature check triggers. They validate release fingerprints and prevent repackaged APK distribution.
4. **Stream Extraction Pipeline (`NewPipeExtractor`)**:
   * The stream extraction in `InnertubeApi.kt` relies on a specific sequence (`ServiceList.YouTube.getStreamExtractor` -> `fetchPage` -> `audioStreams`). Modifying this stream resolution flow can trigger YouTube bot detection blocks.
5. **MusicController Singleton**:
   * `MusicController` is a `@Singleton` and must remain the sole gateway for playback controls to avoid player state desync.

---

## 5. Instructions for Future AI Agents

Before writing code or proposing additions:
1. **Verify DSP Skeleton Status**: Note that `playback/dsp` and `ui/screens/dsp` are currently empty. If asked to implement audio FX, build inside these folders using Media3's audio processor hook.
2. **Check for Security Flags**: The security architecture utilizes a *soft fail* policy where suspicious environments (e.g., emulators) trigger logs rather than crashes. Maintain this soft-fail behavior unless instructed to enforce a hard-fail crash.
3. **Maintain Local-First Integrity**: The Room Database is the source of truth for library lists and downloaded song entities. Ensure all offline actions are reflected in the database immediately.

---

## Index of Memory Graph Documents

* [02_ARCHITECTURE.md](02_ARCHITECTURE.md) - System design and data/request flows.
* [03_FILE_INDEX.md](03_FILE_INDEX.md) - Complete file-level responsibilities.
* [04_CLASS_INDEX.md](04_CLASS_INDEX.md) - Code class relationships and fields.
* [05_METHOD_INDEX.md](05_METHOD_INDEX.md) - Public APIs and method inputs/outputs.
* [06_DEPENDENCY_GRAPH.md](06_DEPENDENCY_GRAPH.md) - System dependency visualizer.
* [07_EVENT_GRAPH.md](07_EVENT_GRAPH.md) - Reactive event flow tracking.
* [08_DATA_FLOW.md](08_DATA_FLOW.md) - Local/online data lifecycle maps.
* [09_API_MAP.md](09_API_MAP.md) - Innertube client and extractor APIs.
* [10_DATABASE_MAP.md](10_DATABASE_MAP.md) - Schema design and Dao query index.
* [11_FEATURE_MAP.md](11_FEATURE_MAP.md) - Tracing user features to files.
* [12_UI_MAP.md](12_UI_MAP.md) - Screen trees and navigation rules.
* [13_CONFIGURATION_MAP.md](13_CONFIGURATION_MAP.md) - Build, gradle, and Proguard mappings.
* [14_GLOBAL_VARIABLES.md](14_GLOBAL_VARIABLES.md) - Singletons, static maps, and shared state.
* [15_EXTERNAL_SERVICES.md](15_EXTERNAL_SERVICES.md) - Third-party libraries audit.
* [16_CALL_GRAPH.md](16_CALL_GRAPH.md) - Main execution call paths.
* [17_PROJECT_RULES.md](17_PROJECT_RULES.md) - AI coding rules and design patterns.
* [18_CURRENT_STATE.md](18_CURRENT_STATE.md) - Completed features, bugs, and technical debt.
* [19_ROADMAP.md](19_ROADMAP.md) - Inferred future backlog items.
* [20_AI_CONTEXT.md](20_AI_CONTEXT.md) - High-density memory summary for prompt injections.
