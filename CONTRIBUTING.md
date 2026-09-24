# Contributing to MyPlayer

Thank you for your interest in contributing to MyPlayer! We are building a premium, hybrid local & online music player for Android, and we welcome contributions from the community.

---

## 🛠 Getting Started

1. **Fork** the repository on GitHub.
2. **Clone** your fork locally:
   ```bash
   git clone https://github.com/Aashutosh2021/MyPlayer.git
   ```
3. Open the project in **Android Studio Koala** (or newer).
4. Build and run the project:
   ```bash
   ./gradlew assembleDebug
   ```

---

## 📐 Architecture & Development Guidelines

To maintain code health and reliability:

### 1. Performance First
- **Zero Main-Thread Blocking:** Never run Room Database queries, network requests (OkHttp/Retrofit), or disk I/O on `Dispatchers.Main`. Wrap all I/O in `withContext(Dispatchers.IO)`.
- **MediaController:** MediaController operations must be handled on `Dispatchers.Main` per Media3 specifications.
- **Compose State:** Use `collectAsStateWithLifecycle()` when observing `StateFlow` in Jetpack Compose to avoid battery drain in background states.
- **List Optimization:** Ensure list items have stable keys and explicit sizing to eliminate redundant measurements.

### 2. Architecture Patterns
- Follow **MVVM (Model-View-ViewModel)** with Unidirectional Data Flow (UDF).
- Repositories expose immutable models and handle caching, offline storage, and online fallbacks.
- Use **Hilt** for dependency injection.

### 3. UI & Design System
- MyPlayer features a bespoke "Clay" tactile neumorphic design system (`claySurface`, `clayConcave`).
- Adhere to the established color tokens and typography in `com.example.myplayer.ui.theme`.

---

## 🧪 Testing

Before submitting your changes, run all unit tests to make sure everything passes:
```bash
./gradlew testDebugUnitTest
```

---

## 📬 Pull Request Process

1. Create a descriptive feature branch:
   ```bash
   git checkout -b feature/your-feature-name
   ```
2. Commit your changes with clear, descriptive commit messages.
3. Push to your branch and open a Pull Request against `main`.
4. Provide a summary of the changes, any screenshots for UI adjustments, and test coverage details.

---

## 📄 License

By contributing to MyPlayer, you agree that your contributions will be licensed under the project's [MIT License](LICENSE).
