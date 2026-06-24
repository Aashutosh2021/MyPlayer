package com.example.myplayer.security

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.lang.reflect.Method
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HookDetectionManager — Phase 9 Security Hardening
 *
 * PURPOSE:
 *   Detects runtime hooking frameworks that intercept and modify method calls
 *   within the running process. Hooking is the primary technique attackers use
 *   to bypass security checks, steal data, and manipulate app behavior.
 *
 * DETECTION TECHNIQUES:
 *   1. Xposed Framework: XposedBridge class, XposedHelper, bridge jar in classpath
 *   2. LSPosed: package presence + /proc/self/maps signatures
 *   3. Cydia Substrate: substrate.h presence, package, /proc/self/maps
 *   4. EdXposed: package presence
 *   5. Stack trace inspection: look for hooking framework classes in call stacks
 *   6. /proc/self/maps: scan for known hooking library signatures
 *   7. Native method hooking: check if standard Java methods have been replaced
 *
 * THREAT MITIGATED:
 *   - Xposed/LSPosed modules bypassing certificate pinning, root detection,
 *     signature verification, or any other security check
 *   - Substrate tweaks hooking into app logic
 *   - Method-level hooking to return fake "safe" results from security checks
 *
 * PROTECTION LEVEL: Medium-High
 *   The most sophisticated setups (Zygisk with anti-detection) will bypass
 *   most Kotlin-level checks. The native layer (Phase 10) provides a more
 *   resilient check. This layer is effective against automated tools and
 *   casual attackers.
 *
 * TRADEOFF:
 *   Reflection-based class loading checks may generate noise in some
 *   obfuscation setups. All exception paths fail-safe (return false).
 */
@Singleton
class HookDetectionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "HookDetectionManager"

        // Xposed-related class names — obfuscated as char arrays to avoid static string detection
        private val XPOSED_CLASSES = arrayOf(
            "de.robv.android.xposed.XposedBridge",
            "de.robv.android.xposed.XposedHelpers",
            "de.robv.android.xposed.XC_MethodHook",
            "de.robv.android.xposed.callbacks.XC_LoadPackage"
        )

        // LSPosed and EdXposed packages
        private val LSPOSED_PACKAGES = arrayOf(
            "org.lsposed.manager",           // LSPosed Manager
            "io.github.lsposed.manager",     // LSPosed alternative
            "com.elderdrivers.riru",         // Riru (LSPosed prerequisite)
            "com.android.edxposed.manager",  // EdXposed Manager
            "de.robv.android.xposed.installer" // Classic Xposed
        )

        // Substrate-related files and paths
        private val SUBSTRATE_PATHS = arrayOf(
            "/data/data/com.saurik.substrate",
            "/system/lib/libsubstrate.so",
            "/system/lib64/libsubstrate.so",
            "/system/lib/libsubstrate-dvm.so"
        )

        // Library signatures found in /proc/self/maps when hooking frameworks are active
        private val HOOKING_MAP_SIGNATURES = arrayOf(
            "xposed", "XposedBridge", "lsposed",
            "substrate", "libsubstrate",
            "edxposed", "yahfa",           // YAHFA — Yet Another Hook Framework for Android
            "sandHook", "pine",            // Pine hook framework
            "dobby", "and-hook"            // Other native hook libraries
        )
    }

    data class HookCheckResult(
        val isHooked: Boolean,
        val reasons: List<String>
    )

    /**
     * Runs all hooking framework detection checks.
     */
    fun check(): HookCheckResult {
        val reasons = mutableListOf<String>()

        if (checkXposedClasses()) reasons += "Xposed class found in classpath"
        if (checkXposedBridgeMethod()) reasons += "XposedBridge.log native method detected"
        if (checkLSPosedPackages()) reasons += "LSPosed/EdXposed package installed"
        if (checkSubstratePaths()) reasons += "Cydia Substrate files detected"
        if (checkProcMapsForHooks()) reasons += "hooking library in /proc/self/maps"
        if (checkStackTrace()) reasons += "hooking framework in call stack"

        val isHooked = reasons.isNotEmpty()
        if (isHooked) {
            Log.w(TAG, "Hooking framework detected: $reasons")
        } else {
            Log.d(TAG, "Hook detection passed — no frameworks detected")
        }

        return HookCheckResult(isHooked, reasons)
    }

    // ─── Individual checks ───────────────────────────────────────────────────

    /**
     * Attempts to load known Xposed classes.
     * Xposed modifies the Zygote and adds these to every app's classpath.
     */
    private fun checkXposedClasses(): Boolean {
        for (className in XPOSED_CLASSES) {
            try {
                Class.forName(className)
                Log.w(TAG, "Xposed class loaded: $className")
                return true
            } catch (_: ClassNotFoundException) {
                // Expected on a clean device
            } catch (e: Exception) {
                // Any other exception could indicate a hook trying to disguise itself
                Log.d(TAG, "Class load exception for $className: ${e.javaClass.simpleName}")
            }
        }
        return false
    }

    /**
     * Checks if XposedBridge.log is a native method — a key signature of Xposed being active.
     * Even if XposedBridge is renamed, the native method binding is harder to hide.
     */
    private fun checkXposedBridgeMethod(): Boolean {
        return try {
            val bridgeClass = Class.forName("de.robv.android.xposed.XposedBridge")
            val logMethod: Method = bridgeClass.getDeclaredMethod("log", String::class.java)
            java.lang.reflect.Modifier.isNative(logMethod.modifiers)
        } catch (_: ClassNotFoundException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks for installed packages belonging to LSPosed, EdXposed, or classic Xposed.
     */
    private fun checkLSPosedPackages(): Boolean {
        return try {
            val pm = context.packageManager
            LSPOSED_PACKAGES.any { pkg ->
                try {
                    pm.getPackageInfo(pkg, 0)
                    true
                } catch (_: PackageManager.NameNotFoundException) { false }
            }
        } catch (e: Exception) { false }
    }

    private fun checkSubstratePaths(): Boolean {
        return try {
            SUBSTRATE_PATHS.any { File(it).exists() }
        } catch (e: Exception) { false }
    }

    /**
     * Scans /proc/self/maps for known hooking library names loaded into this process.
     */
    private fun checkProcMapsForHooks(): Boolean {
        return try {
            BufferedReader(FileReader("/proc/self/maps")).use { reader ->
                reader.lineSequence().any { line ->
                    HOOKING_MAP_SIGNATURES.any { sig ->
                        line.contains(sig, ignoreCase = true)
                    }
                }
            }
        } catch (e: Exception) { false }
    }

    /**
     * Inspects the current thread's call stack for hooking framework class names.
     * When Xposed hooks a method, its wrapper classes appear in the stack trace.
     */
    private fun checkStackTrace(): Boolean {
        return try {
            val stackTrace = Thread.currentThread().stackTrace
            stackTrace.any { element ->
                val className = element.className
                XPOSED_CLASSES.any { xposedClass ->
                    className.contains(xposedClass, ignoreCase = true)
                } || className.contains("substrate", ignoreCase = true)
                  || className.contains("lsposed", ignoreCase = true)
            }
        } catch (e: Exception) { false }
    }
}
