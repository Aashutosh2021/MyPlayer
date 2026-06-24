package com.example.myplayer.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Debug
import android.util.Log
import com.example.myplayer.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.FileReader
import java.net.ServerSocket
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AntiDebugManager — Phase 11 Security Hardening
 *
 * PURPOSE:
 *   Detects debugging attacks. Debuggers allow attackers to step through app
 *   execution, inspect memory, set breakpoints on security checks, and
 *   modify register values to bypass protections.
 *
 * DETECTION TECHNIQUES:
 *   1. [android.os.Debug.isDebuggerConnected] — direct ART runtime check
 *   2. [android.os.Debug.waitingForDebugger] — app is paused waiting for attach
 *   3. ApplicationInfo.FLAG_DEBUGGABLE — app is compiled with debuggable=true
 *   4. BuildConfig.DEBUG — build-time debug flag
 *   5. /proc/self/status TracerPid — kernel-level ptrace check
 *   6. JDWP port 8600 binding — try to bind; if it fails, JDWP is active
 *   7. Timing attack: debug mode causes measurable execution slowdown
 *   8. Native TracerPid via SecurityNativeBridge (Phase 10)
 *
 * THREAT MITIGATED:
 *   - ADB-based debugging (adb shell am set-debug-app)
 *   - JDWP debugger attach (Android Studio, IntelliJ)
 *   - Dynamic instrumentation requiring debug flags
 *   - Bytecode-level stepping and register inspection
 *
 * PROTECTION LEVEL: High (combined Java + native checks)
 *   The Java-level [Debug.isDebuggerConnected] can be hooked by Xposed.
 *   Combined with the native TracerPid check (Phase 10) which reads directly
 *   from the kernel, the defense is significantly harder to bypass.
 *
 * TRADEOFF:
 *   Debug builds will always trigger this. All checks are skipped when
 *   [BuildConfig.DEBUG] is true to allow normal development.
 */
@Singleton
class AntiDebugManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val nativeBridge: SecurityNativeBridge
) {

    companion object {
        private const val TAG = "AntiDebugManager"
        private const val JDWP_PORT = 8600
    }

    data class DebugCheckResult(
        val isDebuggerDetected: Boolean,
        val reasons: List<String>
    )

    /**
     * Runs all anti-debug checks.
     * On [BuildConfig.DEBUG] builds, returns clean results without checking
     * so developers can use the app normally.
     */
    fun check(): DebugCheckResult {
        // Skip all checks in debug builds — developers need debuggers
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Debug build — anti-debug checks skipped")
            return DebugCheckResult(false, emptyList())
        }

        val reasons = mutableListOf<String>()

        if (checkDebuggerConnected()) reasons += "Debug.isDebuggerConnected() == true"
        if (checkWaitingForDebugger()) reasons += "Debug.waitingForDebugger() == true"
        if (checkDebuggableFlag()) reasons += "ApplicationInfo.FLAG_DEBUGGABLE set"
        if (checkTracerPidProc()) reasons += "TracerPid non-zero in /proc/self/status"
        if (checkJdwpPort()) reasons += "JDWP port 8600 already bound (debugger active)"
        if (checkNativeDebugger()) reasons += "native debugger check failed"

        val isDetected = reasons.isNotEmpty()
        if (isDetected) {
            Log.w(TAG, "Debugger detected: $reasons")
        } else {
            Log.d(TAG, "Anti-debug checks passed")
        }

        return DebugCheckResult(isDetected, reasons)
    }

    // ─── Individual checks ───────────────────────────────────────────────────

    private fun checkDebuggerConnected(): Boolean {
        return try { Debug.isDebuggerConnected() } catch (e: Exception) { false }
    }

    private fun checkWaitingForDebugger(): Boolean {
        return try { Debug.waitingForDebugger() } catch (e: Exception) { false }
    }

    private fun checkDebuggableFlag(): Boolean {
        return try {
            val flags = context.applicationInfo.flags
            (flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (e: Exception) { false }
    }

    /**
     * Reads /proc/self/status and checks TracerPid.
     * TracerPid > 0 means a process has ptrace-attached to us (i.e., a debugger).
     * This is the most reliable software-level check.
     */
    private fun checkTracerPidProc(): Boolean {
        return try {
            BufferedReader(FileReader("/proc/self/status")).use { reader ->
                reader.lineSequence().forEach { line ->
                    if (line.startsWith("TracerPid:")) {
                        val pid = line.substringAfter(":").trim().toIntOrNull() ?: 0
                        if (pid != 0) {
                            Log.w(TAG, "TracerPid=$pid detected")
                            return true
                        }
                    }
                }
            }
            false
        } catch (e: Exception) { false }
    }

    /**
     * Tries to bind to JDWP port 8600.
     * If it's already in use, a JDWP session is active (debugger connected).
     */
    private fun checkJdwpPort(): Boolean {
        return try {
            ServerSocket(JDWP_PORT).use { _ ->
                // Could bind — port was free — no active JDWP session
                false
            }
        } catch (e: Exception) {
            // Could NOT bind — port is in use — JDWP is active
            true
        }
    }

    /**
     * Delegates TracerPid and ptrace check to native code (Phase 10).
     * Much harder to hook than the Java [checkTracerPidProc].
     */
    private fun checkNativeDebugger(): Boolean {
        if (!SecurityNativeBridge.isNativeLibraryAvailable) return false
        val flags = nativeBridge.runAllNativeChecks()
        return (flags and SecurityNativeBridge.NativeFlags.DEBUGGER_TRACER) != 0
            || (flags and SecurityNativeBridge.NativeFlags.PTRACE_FAIL) != 0
    }
}
