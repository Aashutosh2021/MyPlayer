package com.example.myplayer.security

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SecurityNativeBridge — Phase 10 JNI Bridge
 *
 * PURPOSE:
 *   Kotlin bridge to the native C++ security library (libmyplayer_security.so).
 *   Provides access to native-level security checks that are significantly
 *   harder to hook than their Kotlin counterparts.
 *
 * DESIGN:
 *   - The native library is loaded once in the companion object's static init.
 *   - [isNativeLibraryAvailable] guards all calls — if the .so fails to load
 *     (e.g., NDK not built), the app degrades gracefully.
 *   - [nativeRunAllChecks] is the primary function: returns a bitmask of all
 *     checks so a Frida hook must patch a single function rather than many.
 *
 * JNI METHOD NAMES:
 *   These MUST match the function names in native-lib.cpp exactly.
 *   ProGuard is configured to keep all native methods in this class.
 *
 * THREAT MITIGATED:
 *   - Java-level Frida hooks on individual security check methods
 *   - Xposed/LSPosed modules overriding specific Kotlin functions
 *   - Memory analysis via jadx (native code is compiled, not bytecode)
 */
@Singleton
class SecurityNativeBridge @Inject constructor() {

    companion object {
        private const val TAG = "SecurityNativeBridge"
        private const val LIB_NAME = "myplayer_security"

        var isNativeLibraryAvailable: Boolean = false
            private set

        init {
            try {
                System.loadLibrary(LIB_NAME)
                isNativeLibraryAvailable = true
                Log.d(TAG, "Native security library loaded")
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "Native security library not available: ${e.message}")
                isNativeLibraryAvailable = false
            }
        }
    }

    // ─── Bitmask constants (match native-lib.cpp) ─────────────────────────────
    object NativeFlags {
        const val FRIDA_DETECTED   = 0x01
        const val DEBUGGER_TRACER  = 0x02
        const val ROOT_SU_BINARY   = 0x04
        const val PTRACE_FAIL      = 0x08
    }

    // ─── Combined check (preferred — atomic, harder to hook) ─────────────────

    /**
     * Runs ALL native security checks in a single JNI call.
     * Returns a bitmask — check against [NativeFlags] constants.
     * Returns 0 if the native library is unavailable.
     */
    fun runAllNativeChecks(): Int {
        if (!isNativeLibraryAvailable) return 0
        return try {
            nativeRunAllChecks()
        } catch (e: Exception) {
            Log.e(TAG, "nativeRunAllChecks exception", e)
            0
        }
    }

    // ─── JNI declarations ─────────────────────────────────────────────────────
    // These names MUST match Java_com_example_myplayer_security_SecurityNativeBridge_*
    // in native-lib.cpp. Do NOT rename without updating both files.

    private external fun nativeRunAllChecks(): Int
}
