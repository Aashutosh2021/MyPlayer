# Project Coding Rules - MyPlayer

This document outlines the coding rules, structural patterns, and development constraints that must be followed by future developers and AI models working on the **MyPlayer** project.

---

## 1. Architectural Rules

* **MVVM Architecture Only**:
  * ViewModels must never hold references to Android Views, Contexts, or Composable components.
  * Composables must never execute direct business logic, database queries, or network requests. Route all actions through ViewModels.
* **Hilt Dependency Injection Only**:
  * Never instantiate DAO databases, OkHttpClient instances, or singleton classes manually via standard constructors. Add new dependencies to Hilt modules (`DatabaseModule`, `NetworkModule`, `PlayerModule`, `CacheModule`) and inject them via constructor injection.
* **Music Playback Decoupling**:
  * `MusicController` is the **only** class allowed to interact with the Media3 `MediaController` and command the background service. Never create a separate MediaController instance or duplicate service wrappers.
* **Local-First Synchronization**:
  * Room database entities are the source of truth for library states. Any operation affecting local playlists, favorites, folders, or history must mutate the database first and let Room flow triggers propagate updates to the UI.

---

## 2. Hard Security Constraints

> [!WARNING]
> Security features are delicate. Do NOT bypass, rename, or strip structural checks under any circumstances.

* **Never Modify Native JNI Declarations**:
  * The methods in `SecurityNativeBridge.kt` (such as `nativeRunAllChecks`, `nativeIsFridaDetected`, etc.) are linked to JNI bindings in the compiled C++ library (`libmyplayer_security.so`). Modifying their names, arguments, package pathways, or return types will cause runtime link errors.
* **Do Not De-obfuscate Constants**:
  * URL and key parameters in `StringEncryptionManager.kt` are stored as XOR obfuscated integers. Never replace these arrays with plain-text String values. If new remote endpoints are added, encrypt them via XOR and add them as integer array tables.
* **Preserve Signature Verification**:
  * The APK certificate verification logic in `SignatureVerifier.kt` is a critical defense against modified/repackaged APK sideloading. Do not comment out, bypass, or return mock `VerificationResult(isValid = true)` stub values.
* **Maintain JNI Environment Checks**:
  * The native memory checks in `native-lib.cpp` (Frida TCP connection, ptrace tracer check, su binary file parsing, status TracerPid parsing) must remain intact.

---

## 3. UI Styling Standards

* **Tactile Claymorphic Aesthetic**:
  * Do not use standard Material 3 border/card layouts. Reusable UI components must utilize the custom `.claySurface()` and `.clayConcave()` modifiers from `Modifiers.kt`.
  * Convex widgets (ClayCard, ClayButton, ClayIconButton) represent interactive or floating elements.
  * Concave widgets (ClayProgressBar track, text input fields) represent sunken surfaces.
* **Harmonious Palette**:
  * Surface and background colors must utilize Cloud Blue (`#EEF3FF`), Surface Light (`#FAF9FF`), and associated Claymorphic shadows (`ClayShadowInnerLight`, `ClayShadowInnerDark`) to ensure shadows and highlights blend correctly.

---

## 4. Playback and Downloads Pipeline

* **NewPipeExtractor Integrity**:
  * The stream resolution logic in `InnertubeApi.kt` uses specific client-masquerading context headers to bypass YouTube bandwidth throttling. Never change client parameters without validating that stream downloads still work.
* **Expedited Background Downloads**:
  * Background download workers must be dispatched via WorkManager's `OneTimeWorkRequestBuilder` using `setExpedited` rules. This ensures downloads survive system thread suspension limits.
