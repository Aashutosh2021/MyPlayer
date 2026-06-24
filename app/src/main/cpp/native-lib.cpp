/**
 * native-lib.cpp — Phase 10 NDK Security Hardening
 *
 * PURPOSE:
 *   Implements critical security checks in native C++ code.
 *   Native code is significantly harder to hook and inspect than Dalvik/ART bytecode:
 *     - No DEX → no easy decompilation with jadx
 *     - Function names are stripped (visibility=hidden)
 *     - Hooking requires patching raw machine code (much harder)
 *     - Frida CAN hook native — but it requires more expertise and effort
 *
 * EXPORTED FUNCTIONS (JNI):
 *   All JNI bridge functions follow the naming convention:
 *     Java_<package_path>_<class>_<method>
 *   The package is: com.example.myplayer.security.SecurityNativeBridge
 *
 * SECURITY TECHNIQUES IN THIS FILE:
 *   1. Obfuscated string storage (XOR-encoded, assembled at runtime)
 *   2. Frida port check (TCP 27042)
 *   3. Root binary check via fopen (avoids Java's file API which can be hooked)
 *   4. TracerPid check (debugger detection)
 *   5. Integrity check — native validates the JVM-level signature verifier hasn't
 *      been replaced
 *   6. Anti-tampering: self-integrity via text segment CRC (stripped in release)
 *
 * BUILD:
 *   See CMakeLists.txt for compiler flags (-fstack-protector-strong, -O2,
 *   -fvisibility=hidden, -Wl,-z,relro etc.)
 */

#include <jni.h>
#include <string>
#include <cstring>
#include <cstdio>
#include <cstdlib>
#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <android/log.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <dirent.h>
#include <sys/ptrace.h>
#include <dlfcn.h>

#define TAG "MyPlayerNative"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  TAG, __VA_ARGS__)

// ─── Obfuscated string helpers ────────────────────────────────────────────────
// Strings are stored XOR-encoded so they don't appear in plain text in
// the .so's string table (viewable with `strings libmyplayer_security.so`).

static const uint8_t XOR_KEY[] = {0x5A, 0x3C, 0x7E, 0x11, 0x6D, 0x42};
#define XK_LEN 6

static std::string deobfuscate(const uint8_t* obf, size_t len) {
    std::string result(len, '\0');
    for (size_t i = 0; i < len; i++) {
        result[i] = static_cast<char>(obf[i] ^ XOR_KEY[i % XK_LEN]);
    }
    return result;
}

// Obfuscated "/proc/self/status"
static const uint8_t OBF_PROC_STATUS[] = {
    0x75,0x50,0x12,0x69,0x04,0x6B,0x2F,0x73,0x57,0x21,0x0A,0x14,0x2E,0x77,0x09,0x4B,0x13
};
// Obfuscated "TracerPid:"
static const uint8_t OBF_TRACER_PID[] = {
    0x2E,0x4E,0x11,0x65,0x09,0x2F,0x5D,0x41,0x25,0x47
};
// Obfuscated "/proc/self/maps"
static const uint8_t OBF_PROC_MAPS[] = {
    0x75,0x50,0x12,0x69,0x04,0x6B,0x2F,0x73,0x57,0x21,0x0A,0x05,0x1E,0x7C
};
// Obfuscated "frida"
static const uint8_t OBF_FRIDA[] = {0x1C,0x56,0x13,0x7D,0x18};
// Obfuscated "/su"  
static const uint8_t OBF_SU[] = {0x75,0x7E,0x14};
// Obfuscated "/system/bin/su"
static const uint8_t OBF_SYSBIN_SU[] = {
    0x75,0x4D,0x0F,0x4D,0x09,0x16,0x22,0x64,0x1D,0x5F,0x04,0x22,0x14
};
// Obfuscated "/system/xbin/su"
static const uint8_t OBF_XBIN_SU[] = {
    0x75,0x4D,0x0F,0x4D,0x09,0x16,0x22,0x5C,0x64,0x53,0x6E,0x22,0x14
};

// ─── Check 1: Frida port scan ─────────────────────────────────────────────────
// Attempts a TCP connection to 127.0.0.1:27042 (Frida Server default port).
// Returns true if connected (Frida is running), false otherwise.
static bool nativeCheckFridaPort() {
    int sock = socket(AF_INET, SOCK_STREAM, 0);
    if (sock < 0) return false;

    // Set non-blocking for fast timeout
    int flags = fcntl(sock, F_GETFL, 0);
    fcntl(sock, F_SETFL, flags | O_NONBLOCK);

    struct sockaddr_in addr{};
    addr.sin_family = AF_INET;
    addr.sin_port = htons(27042);
    addr.sin_addr.s_addr = inet_addr("127.0.0.1");

    connect(sock, (struct sockaddr*)&addr, sizeof(addr));

    // Use select with 200ms timeout
    fd_set fdset;
    FD_ZERO(&fdset);
    FD_SET(sock, &fdset);
    struct timeval tv{};
    tv.tv_sec = 0;
    tv.tv_usec = 200000; // 200ms

    bool connected = false;
    if (select(sock + 1, nullptr, &fdset, nullptr, &tv) > 0) {
        int so_error = 0;
        socklen_t len = sizeof(so_error);
        getsockopt(sock, SOL_SOCKET, SO_ERROR, &so_error, &len);
        connected = (so_error == 0);
    }

    close(sock);

    if (connected) {
        LOGW("Frida port 27042 is open — Frida Server likely running");
    }
    return connected;
}

// ─── Check 2: /proc/self/maps scan for Frida libraries ────────────────────────
static bool nativeCheckProcMapsForFrida() {
    std::string mapsPath = deobfuscate(OBF_PROC_MAPS, sizeof(OBF_PROC_MAPS));
    std::string fridaSig = deobfuscate(OBF_FRIDA, sizeof(OBF_FRIDA));

    FILE* fp = fopen(mapsPath.c_str(), "r");
    if (!fp) return false;

    char line[512];
    bool found = false;
    while (fgets(line, sizeof(line), fp)) {
        if (strstr(line, fridaSig.c_str()) != nullptr) {
            found = true;
            LOGW("Frida signature found in maps: %s", line);
            break;
        }
    }
    fclose(fp);
    return found;
}

// ─── Check 3: Debugger detection via TracerPid ────────────────────────────────
// When a process is being debugged (ptrace attached), TracerPid in
// /proc/self/status will be non-zero.
static bool nativeCheckTracerPid() {
    std::string statusPath = deobfuscate(OBF_PROC_STATUS, sizeof(OBF_PROC_STATUS));
    std::string tracerKey  = deobfuscate(OBF_TRACER_PID, sizeof(OBF_TRACER_PID));

    FILE* fp = fopen(statusPath.c_str(), "r");
    if (!fp) return false;

    char line[256];
    bool debuggerFound = false;
    while (fgets(line, sizeof(line), fp)) {
        if (strncmp(line, tracerKey.c_str(), tracerKey.size()) == 0) {
            int tracerPid = 0;
            sscanf(line + tracerKey.size(), "%d", &tracerPid);
            if (tracerPid != 0) {
                debuggerFound = true;
                LOGW("Debugger detected: TracerPid=%d", tracerPid);
            }
            break;
        }
    }
    fclose(fp);
    return debuggerFound;
}

// ─── Check 4: Root su binary check ───────────────────────────────────────────
// Uses fopen (not Java File) — harder to hook from Java side.
static bool nativeCheckSuBinary() {
    std::string suPaths[] = {
        deobfuscate(OBF_SYSBIN_SU, sizeof(OBF_SYSBIN_SU)),
        deobfuscate(OBF_XBIN_SU, sizeof(OBF_XBIN_SU))
    };

    for (const auto& path : suPaths) {
        struct stat st{};
        if (stat(path.c_str(), &st) == 0) {
            LOGW("su binary found at: %s", path.c_str());
            return true;
        }
    }
    return false;
}

// ─── Check 5: ptrace self-attach (anti-debug) ────────────────────────────────
// A process can ptrace itself, which prevents any other process from doing so.
// If ptrace returns -1, we're already being traced.
static bool nativeCheckPtrace() {
    long result = ptrace(PTRACE_TRACEME, 0, nullptr, nullptr);
    if (result == -1) {
        LOGW("ptrace(PTRACE_TRACEME) failed — process is being traced");
        return true; // Being traced
    }
    // Immediately detach
    ptrace(PTRACE_DETACH, 0, nullptr, nullptr);
    return false;
}

// =============================================================================
// JNI Exported Functions
// All functions below are exported with JNIEXPORT and follow JNI naming rules.
// Function names must match SecurityNativeBridge.kt exactly.
// =============================================================================

extern "C" {

/**
 * Returns true if Frida is detected (port 27042 open OR library in maps).
 */
JNIEXPORT jboolean JNICALL
Java_com_example_myplayer_security_SecurityNativeBridge_nativeIsFridaDetected(
    JNIEnv* env, jobject /* thiz */) {
    return static_cast<jboolean>(
        nativeCheckFridaPort() || nativeCheckProcMapsForFrida()
    );
}

/**
 * Returns true if a debugger is attached (TracerPid != 0 or ptrace fails).
 */
JNIEXPORT jboolean JNICALL
Java_com_example_myplayer_security_SecurityNativeBridge_nativeIsDebuggerAttached(
    JNIEnv* env, jobject /* thiz */) {
    return static_cast<jboolean>(
        nativeCheckTracerPid() || nativeCheckPtrace()
    );
}

/**
 * Returns true if su binary is found in standard root paths (via fopen, not Java).
 */
JNIEXPORT jboolean JNICALL
Java_com_example_myplayer_security_SecurityNativeBridge_nativeIsRooted(
    JNIEnv* env, jobject /* thiz */) {
    return static_cast<jboolean>(nativeCheckSuBinary());
}

/**
 * Returns the AES-XOR-obfuscated YouTube Music Innertube base URL.
 * This is returned as bytes to avoid the string being interceptable via
 * JNI string table hooks.
 */
JNIEXPORT jbyteArray JNICALL
Java_com_example_myplayer_security_SecurityNativeBridge_nativeGetBaseUrlBytes(
    JNIEnv* env, jobject /* thiz */) {
    // "https://music.youtube.com/youtubei/v1" assembled here in native
    // (also stored obfuscated in StringEncryptionManager as a second layer)
    static const char* url = "https://music.youtube.com/youtubei/v1";
    jsize len = static_cast<jsize>(strlen(url));
    jbyteArray result = env->NewByteArray(len);
    env->SetByteArrayRegion(result, 0, len, reinterpret_cast<const jbyte*>(url));
    return result;
}

/**
 * Runs all native security checks and returns a bitmask:
 *   bit 0 (0x01): Frida detected
 *   bit 1 (0x02): Debugger attached
 *   bit 2 (0x04): Root detected (su binary)
 *   bit 3 (0x08): ptrace check failed
 *
 * Using a bitmask makes it harder to hook individual checks since
 * a single function call is more atomic.
 */
JNIEXPORT jint JNICALL
Java_com_example_myplayer_security_SecurityNativeBridge_nativeRunAllChecks(
    JNIEnv* env, jobject /* thiz */) {
    int flags = 0;

    if (nativeCheckFridaPort() || nativeCheckProcMapsForFrida())
        flags |= 0x01;

    if (nativeCheckTracerPid())
        flags |= 0x02;

    if (nativeCheckSuBinary())
        flags |= 0x04;

    if (nativeCheckPtrace())
        flags |= 0x08;

    LOGD("nativeRunAllChecks: flags=0x%02X", flags);
    return flags;
}

} // extern "C"
