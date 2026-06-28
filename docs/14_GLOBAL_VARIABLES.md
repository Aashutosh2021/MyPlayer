# Global State & Singletons Map - MyPlayer

This map catalogs the singletons, static fields, in-memory caches, and thread scopes that constitute the global runtime state of the **MyPlayer** application.

---

## 1. Application Singletons (Hilt-Managed)

The following components are instantiated once and persist throughout the application lifecycle:

### Class: `MusicController`
* **Inject Scope**: `@Singleton`
* **Thread Context**: Main Thread (`Dispatchers.Main` + `SupervisorJob()`).
* **Active State Variables**:
  * `_currentSong`: `MutableStateFlow<SongEntity?>`
  * `_currentOnlineSong`: `MutableStateFlow<OnlineSong?>`
  * `_isPlaying`: `MutableStateFlow<Boolean>`
  * `_currentPosition`: `MutableStateFlow<Long>`
  * `_currentDuration`: `MutableStateFlow<Long>`
  * `_sleepTimerRemainingSeconds`: `MutableStateFlow<Long>`
* **Lifecycle**: Created when first accessed by viewmodels; connects asynchronously to the player service.

### Class: `SecurityManager`
* **Inject Scope**: `@Singleton`
* **Thread Context**: Background thread pool (`Dispatchers.IO` + `SupervisorJob()`).
* **Active State Variables**:
  * `_securityStatus`: `MutableStateFlow<SecurityStatus>`
* **Lifecycle**: Spawned early in `MyPlayerApplication.onCreate()`, executing checks in parallel.

### Class: `AppDatabase`
* **Inject Scope**: `@Singleton` (Provided via `DatabaseModule.kt`).
* **Thread Context**: Database queries are automatically dispatched to background threads via Room's Flow engine.
* **Lifecycle**: Initialized on startup; handles connection pooling to the SQLite file.

### Class: `InnertubeApi`
* **Inject Scope**: `@Singleton`
* **Thread Context**: Non-blocking network queries.
* **Active State Variables**:
  * `newPipeInitialized`: `AtomicBoolean` static initializer flag. Ensures the NewPipe extractor downloader is configured exactly once.

### Class: `PreferencesManager`
* **Inject Scope**: `@Singleton`
* **Thread Context**: Thread-safe reads/writes via Okio DataStore.

---

## 2. Companion Constants & Static Arrays

The security framework uses split string arrays to evade static string analysis:

### Class: `SignatureVerifier`
* **Fields**:
  * `FINGERPRINT_PARTS`: `Array<String>` - Splits the expected SHA-256 cert fingerprint into four components.
  * `EXPECTED_SHA256_FINGERPRINT`: `String` - Assembled dynamically at runtime:
    ```kotlin
    FINGERPRINT_PARTS.joinToString(":")
    ```

### Class: `StringEncryptionManager`
* **Fields**:
  * `XOR_KEY`: `ByteArray` - Rotational XOR key:
    ```kotlin
    byteArrayOf(0x5A, 0x3C, 0x7E, 0x11, 0x6D, 0x42, 0x58, 0x70)
    ```
  * `BASE_URL_OBF`: `IntArray` - XOR-obfuscated byte representation of `"https://music.youtube.com/youtubei/v1"`.
  * `YTM_API_KEY_OBF`: `IntArray` - XOR-obfuscated byte representation of public API key.
  * `CLIENT_WEB_OBF` & `CLIENT_WEB_VER_OBF` & `CLIENT_VR_OBF`: Context headers byte tables.

### Class: `SecurityNativeBridge`
* **Fields**:
  * `isNativeLibraryAvailable`: `Boolean` - Initialized statically:
    ```kotlin
    try {
        System.loadLibrary("myplayer_security")
        isNativeLibraryAvailable = true
    } catch (e: UnsatisfiedLinkError) {
        isNativeLibraryAvailable = false
    }
    ```

---

## 3. In-Memory Decryption Caches

`StringEncryptionManager` maintains a private runtime cache map:
* **Field**: `cache: HashMap<String, String>` (Default initial capacity: `8`).
* **Purpose**: Prevents repeated XOR calculations when API requests fetch parameters. Decrypted strings (like keys and endpoints) are held in memory for fast retrieval and are cleared if the process terminates.

---

## 4. Native C++ Static States (`native-lib.cpp`)

The native binary allocates memory for key-matching:
* **XOR_KEY**: `uint8_t` array (6-byte sequence `0x5A, 0x3C, 0x7E, 0x11, 0x6D, 0x42`).
* **Static String Tables**: Obfuscated system pathways used to bypass Java hooking detection:
  * `OBF_PROC_STATUS`: `"/proc/self/status"`
  * `OBF_TRACER_PID`: `"TracerPid:"`
  * `OBF_PROC_MAPS`: `"/proc/self/maps"`
  * `OBF_FRIDA`: `"frida"`
  * `OBF_SU`: `"/su"`
  * `OBF_SYSBIN_SU`: `"/system/bin/su"`
  * `OBF_XBIN_SU`: `"/system/xbin/su"`
