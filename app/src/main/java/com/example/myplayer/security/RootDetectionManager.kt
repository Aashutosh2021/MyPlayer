package com.example.myplayer.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RootDetectionManager — Phase 6 Security Hardening
 *
 * PURPOSE:
 *   Detects whether the device is rooted. Rooted devices give attackers
 *   elevated privileges to inspect memory, bypass security checks, and
 *   extract secrets from the app's private storage.
 *
 * DETECTION TECHNIQUES:
 *   1. `su` binary presence in common PATH locations
 *   2. Known root management app packages (Magisk, SuperSU, KingRoot, etc.)
 *   3. Build tag check (`test-keys` = AOSP debug/engineering build)
 *   4. Writable system partition check
 *   5. Magisk-specific paths and socket files
 *   6. Root cloaking check — attempt to execute `su` even if hidden
 *   7. Dangerous system properties set by root frameworks
 *
 * THREAT MITIGATED:
 *   - Root-based memory dumping (e.g., `adb root`, `objection`)
 *   - Key extraction from Android Keystore on older rooted devices
 *   - Runtime hook injection (Frida, Xposed)
 *
 * PROTECTION LEVEL: Medium
 *   Magisk's DenyList and Shamiko module can defeat most software-based checks.
 *   Root detection is most effective when combined with Play Integrity (Phase 5),
 *   which uses hardware attestation and is much harder to bypass.
 *
 * TRADEOFF:
 *   Developers working on the app with ADB access on a rooted device will see
 *   false positives. Check [BuildConfig.DEBUG] before acting on the result.
 */
@Singleton
class RootDetectionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "RootDetectionManager"

        // su binary locations checked across major root implementations
        private val SU_PATHS = arrayOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su", "/su/bin/su",
            "/vendor/bin/su", "/system/su", "/system/bin/.ext/.su",
            "/system/usr/we-need-root/su-backup", "/system/xbin/mu",
            "/data/local/xbin/su", "/data/local/bin/su", "/data/local/su",
            "/data/adb/su", "/dev/com.koushikdutta.superuser.daemon/"
        )

        // Known root management application package names
        private val ROOT_PACKAGES = arrayOf(
            "com.noshufou.android.su",          // SuperUser
            "com.noshufou.android.su.elite",    // SuperUser Elite
            "eu.chainfire.supersu",              // SuperSU
            "eu.chainfire.supersu.pro",          // SuperSU Pro
            "com.koushikdutta.superuser",        // ClockworkMod SuperUser
            "com.thirdparty.superuser",
            "com.yellowes.su",
            "com.topjohnwu.magisk",              // Magisk (primary)
            "io.github.vvb2060.magisk",          // Magisk variants
            "io.github.huskydg.magisk",          // Magisk Delta
            "com.kingroot.kinguser",             // KingRoot
            "com.kingo.root",                    // KingoRoot
            "com.smedialink.oneclickroot",
            "com.zhiqupk.root.global",
            "com.alephzain.framaroot",
            "com.koushikdutta.rommanager",
            "com.koushikdutta.rommanager.license",
            "com.dimonvideo.luckypatcher",       // Lucky Patcher
            "com.chelpus.lackypatch",
            "com.ramdroid.appquarantine",
            "com.ramdroid.appquarantinepro",
            "com.devadvance.rootcloak",          // Root Cloak (meta-detection!)
            "com.devadvance.rootcloakplus",
            "de.robv.android.xposed.installer",  // Xposed (also Phase 9)
            "com.saurik.substrate",              // Cydia Substrate (also Phase 9)
            "com.zachspong.temprootremovejb",
            "com.amphoras.hidemyroot",
            "com.formyhm.hiderootPremium",
            "com.amphoras.hidemyrootadfree",
            "com.zachspong.temprootremovejb",
            "com.ramdroid.appquarantine"
        )

        // Magisk-specific paths and files
        private val MAGISK_PATHS = arrayOf(
            "/sbin/.magisk", "/sbin/.core/mirror", "/sbin/.core/img",
            "/sbin/.core/db-0/magisk.db", "/data/adb/magisk",
            "/data/adb/magisk.img", "/cache/magisk.log",
            "/data/adb/ksu",          // KernelSU
            "/data/adb/apd",          // APatch
        )
    }

    data class RootCheckResult(
        val isRooted: Boolean,
        val reasons: List<String>
    )

    /**
     * Runs all root detection checks and returns an aggregated result.
     * Individual checks are isolated — one exception won't block others.
     */
    fun check(): RootCheckResult {
        val reasons = mutableListOf<String>()

        if (checkSuBinary()) reasons += "su binary found"
        if (checkRootPackages()) reasons += "root management app installed"
        if (checkBuildTags()) reasons += "test-keys build tag"
        if (checkWritableSystem()) reasons += "system partition is writable"
        if (checkMagiskPaths()) reasons += "Magisk/KernelSU paths detected"
        if (checkDangerousProperties()) reasons += "dangerous system properties detected"
        if (checkRootCloaking()) reasons += "su execution succeeded"

        val isRooted = reasons.isNotEmpty()
        if (isRooted) {
            Log.w(TAG, "Root detected: $reasons")
        } else {
            Log.d(TAG, "Root check passed — no indicators found")
        }

        return RootCheckResult(isRooted, reasons)
    }

    // ─── Individual checks ───────────────────────────────────────────────────

    private fun checkSuBinary(): Boolean {
        return try {
            SU_PATHS.any { File(it).exists() }
        } catch (e: Exception) { false }
    }

    private fun checkRootPackages(): Boolean {
        return try {
            val pm = context.packageManager
            ROOT_PACKAGES.any { packageName ->
                try {
                    pm.getPackageInfo(packageName, 0)
                    true
                } catch (_: PackageManager.NameNotFoundException) { false }
            }
        } catch (e: Exception) { false }
    }

    private fun checkBuildTags(): Boolean {
        return try {
            val tags = Build.TAGS
            tags != null && tags.contains("test-keys")
        } catch (e: Exception) { false }
    }

    private fun checkWritableSystem(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val exitCode = process.waitFor()
            process.destroy()
            exitCode == 0
        } catch (e: Exception) {
            // If /system/xbin/which su returns 0 OR the system partition is mounted rw
            try {
                val file = File("/system")
                file.canWrite()
            } catch (_: Exception) { false }
        }
    }

    private fun checkMagiskPaths(): Boolean {
        return try {
            MAGISK_PATHS.any { File(it).exists() }
        } catch (e: Exception) { false }
    }

    private fun checkDangerousProperties(): Boolean {
        return try {
            val dangerousProps = mapOf(
                "ro.debuggable" to "1",
                "ro.secure" to "0",
                "service.adb.root" to "1"
            )
            dangerousProps.any { (key, dangerousValue) ->
                getSystemProperty(key) == dangerousValue
            }
        } catch (e: Exception) { false }
    }

    /**
     * Attempts to actually execute `su` — catches root cloaking tools that
     * hide the binary from file system checks but still allow execution.
     */
    private fun checkRootCloaking(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val result = process.inputStream.bufferedReader().readText()
            process.destroy()
            result.contains("uid=0") // uid=0 means root
        } catch (e: Exception) {
            false // Exception = su doesn't exist or is properly blocked
        }
    }

    private fun getSystemProperty(key: String): String? {
        return try {
            @Suppress("PrivateApi")
            val clazz = Class.forName("android.os.SystemProperties")
            val method = clazz.getMethod("get", String::class.java)
            method.invoke(null, key) as? String
        } catch (e: Exception) { null }
    }
}
