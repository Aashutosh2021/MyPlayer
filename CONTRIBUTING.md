# Contributing to MyPlayer

Thank you for your interest in contributing to MyPlayer! We are building a premium, hybrid local/online music player and welcome improvements from the community.

## Development Rules

To maintain the high quality and performance standards established through the RC phases, please adhere to the following rules when contributing code:

### 1. Performance First
- **No Main Thread Blocking:** Never perform Database (Room), Network (OkHttp), or SharedPreferences/DataStore operations on `Dispatchers.Main`. Always wrap IO-bound operations in `withContext(Dispatchers.IO)`.
- **MediaController Contracts:** `MediaController` APIs *must* be called from the Main thread (`Dispatchers.Main`). Do not call `controller.play()` or `controller.currentPosition` from a worker thread.
- **Compose State:** Use `collectAsStateWithLifecycle()` exclusively when collecting `StateFlow` in Jetpack Compose to avoid draining the battery in the background.
- **UI Jank:** `LazyColumn` and `LazyRow` items must have explicit height modifiers when nested to prevent double-pass measurements.

### 2. Architecture
- Follow the MVVM (Model-View-ViewModel) pattern strictly.
- Views (Composables) should be stateless where possible and receive data/events from the ViewModel.
- Do not introduce massive rewrites or new architectural paradigms without prior discussion in an Issue.

### 3. Aesthetics
- MyPlayer uses a bespoke "Clay/Glassmorphism" design system (`claySurface`, `clayConcave`). Do not replace this with standard Material design cards unless specifically required.
- Do not use generic RGB colors. Always refer to the tokens defined in `Theme.kt` / `Color.kt`.

## Submission Process

1. Fork the repository.
2. Create a feature branch (`git checkout -b feature/your-feature-name`).
3. Commit your changes.
4. Ensure the project builds successfully (`./gradlew assembleDebug testDebugUnitTest`).
5. Open a Pull Request against the `main` branch. Provide a detailed summary of your changes.
