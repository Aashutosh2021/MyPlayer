package com.example.myplayer.security

import android.util.Log
import com.example.myplayer.BuildConfig
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SecurityManager — Phase 15 Security Orchestrator
 *
 * PURPOSE:
 *   Central orchestrator that runs all security checks on application startup
 *   and provides a reactive [SecurityStatus] observable to the rest of the app.
 *
 * DESIGN:
 *   - All checks run on [Dispatchers.IO] to not block the main thread.
 *   - Results are aggregated into a [SecurityStatus] sealed class.
 *   - The app uses a SOFT FAIL policy: suspicious environments are logged and
 *     reported but the app continues to function. This is appropriate for a
 *     music player where there is no financial risk.
 *
 * SECURITY POLICY (SOFT FAIL):
 *   - [SecurityStatus.Trusted]     → Normal operation
 *   - [SecurityStatus.Suspicious]  → Log, report (no functionality blocked)
 *   - [SecurityStatus.Compromised] → Log, report (could optionally block features)
 *
 *   To switch to HARD FAIL (exit app): implement [SecurityEventHandler] and
 *   call `android.os.Process.killProcess(android.os.Process.myPid())` on
 *   [SecurityStatus.Compromised].
 *
 * INTEGRATION:
 *   Inject SecurityManager into MainActivity or MyPlayerApplication and call
 *   [initialize]. Collect [securityStatus] to react to results.
 */
@Singleton
class SecurityManager @Inject constructor(
    private val signatureVerifier: SignatureVerifier,
    private val rootDetection: RootDetectionManager,
    private val fridaDetection: FridaDetectionManager,
    private val emulatorDetection: EmulatorDetectionManager,
    private val hookDetection: HookDetectionManager,
    private val antiDebug: AntiDebugManager,
    private val tamperDetection: TamperDetectionManager,
    private val nativeBridge: SecurityNativeBridge,
    private val integrityManager: IntegrityManager
) {

    companion object {
        private const val TAG = "SecurityManager"
    }

    sealed class SecurityStatus {
        /** Device and app environment appear clean. */
        object Trusted : SecurityStatus()

        /**
         * Some indicators were found but they might be false positives
         * (e.g., emulator detected but no Frida/root).
         */
        data class Suspicious(val reasons: List<String>) : SecurityStatus()

        /**
         * Multiple strong indicators found. High confidence of attack or
         * reverse engineering attempt.
         */
        data class Compromised(val reasons: List<String>) : SecurityStatus()

        /** Initial state before checks complete. */
        object Pending : SecurityStatus()
    }

    private val _securityStatus = MutableStateFlow<SecurityStatus>(SecurityStatus.Pending)
    val securityStatus: StateFlow<SecurityStatus> = _securityStatus.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Starts all security checks asynchronously.
     * Call from [MyPlayerApplication.onCreate] or [MainActivity.onStart].
     *
     * Results are published to [securityStatus] and logged.
     */
    fun initialize() {
        // Warm up Play Integrity token provider
        integrityManager.warmup()

        scope.launch {
            runAllChecks()
        }
    }

    private suspend fun runAllChecks() {
        Log.d(TAG, "Starting security checks...")
        val allReasons = mutableListOf<String>()
        var highSeverityCount = 0

        // ─── Phase 4: Signature Verification ─────────────────────────────────
        runSafe("SignatureVerifier") {
            val result = signatureVerifier.verify()
            if (!result.isValid) {
                allReasons += "Signature: ${result.reason}"
                highSeverityCount++ // Repackaged APK = high severity
            }
        }

        // ─── Phase 6: Root Detection ──────────────────────────────────────────
        runSafe("RootDetection") {
            val result = rootDetection.check()
            if (result.isRooted) {
                allReasons.addAll(result.reasons.map { "Root: $it" })
                highSeverityCount++
            }
        }

        // ─── Phase 7: Frida Detection (Java) ─────────────────────────────────
        runSafe("FridaDetection") {
            val result = fridaDetection.check()
            if (result.isFridaDetected) {
                allReasons.addAll(result.reasons.map { "Frida: $it" })
                highSeverityCount++
            }
        }

        // ─── Phase 8: Emulator Detection ─────────────────────────────────────
        runSafe("EmulatorDetection") {
            val result = emulatorDetection.check()
            if (result.isEmulator) {
                allReasons.addAll(result.reasons.map { "Emulator: $it" })
                // Emulator alone is medium severity (could be legitimate tester)
            }
        }

        // ─── Phase 9: Hook Detection ──────────────────────────────────────────
        runSafe("HookDetection") {
            val result = hookDetection.check()
            if (result.isHooked) {
                allReasons.addAll(result.reasons.map { "Hook: $it" })
                highSeverityCount++
            }
        }

        // ─── Phase 10: Native Security Checks ────────────────────────────────
        runSafe("NativeChecks") {
            val flags = nativeBridge.runAllNativeChecks()
            if (flags and SecurityNativeBridge.NativeFlags.FRIDA_DETECTED != 0) {
                allReasons += "Native: Frida port/maps detected"
                highSeverityCount++
            }
            if (flags and SecurityNativeBridge.NativeFlags.DEBUGGER_TRACER != 0) {
                allReasons += "Native: debugger TracerPid detected"
                highSeverityCount++
            }
            if (flags and SecurityNativeBridge.NativeFlags.ROOT_SU_BINARY != 0) {
                allReasons += "Native: su binary found"
                // Already counted in root check
            }
            if (flags and SecurityNativeBridge.NativeFlags.PTRACE_FAIL != 0) {
                allReasons += "Native: ptrace self-test failed (already traced)"
                highSeverityCount++
            }
        }

        // ─── Phase 11: Anti-Debug ─────────────────────────────────────────────
        runSafe("AntiDebug") {
            val result = antiDebug.check()
            if (result.isDebuggerDetected) {
                allReasons.addAll(result.reasons.map { "Debug: $it" })
                if (!BuildConfig.DEBUG) highSeverityCount++
            }
        }

        // ─── Phase 12: Tamper Detection ───────────────────────────────────────
        runSafe("TamperDetection") {
            val result = tamperDetection.check()
            if (result.isTampered) {
                allReasons.addAll(result.reasons.map { "Tamper: $it" })
                highSeverityCount++
            }
        }

        // ─── Aggregate result ─────────────────────────────────────────────────
        val status = when {
            allReasons.isEmpty() -> SecurityStatus.Trusted
            highSeverityCount >= 2 -> SecurityStatus.Compromised(allReasons)
            else -> SecurityStatus.Suspicious(allReasons)
        }

        _securityStatus.value = status

        when (status) {
            SecurityStatus.Trusted -> Log.i(TAG, "Security checks complete — TRUSTED")
            is SecurityStatus.Suspicious -> Log.w(TAG, "Security checks complete — SUSPICIOUS: ${status.reasons}")
            is SecurityStatus.Compromised -> Log.e(TAG, "Security checks complete — COMPROMISED: ${status.reasons}")
            SecurityStatus.Pending -> { /* Should not happen */ }
        }
    }

    private inline fun runSafe(name: String, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            Log.e(TAG, "Security check $name threw exception", e)
        }
    }
}
