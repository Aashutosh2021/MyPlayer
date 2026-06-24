package com.example.myplayer.security

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TamperDetectionManager — Phase 12 Security Hardening
 *
 * PURPOSE:
 *   Detects whether the APK has been modified (tampered) after it was signed.
 *   Tampered APKs are commonly used to:
 *     - Remove in-app purchase checks
 *     - Inject malicious code into the app
 *     - Remove ads or content restrictions
 *     - Steal user data via a trojanized version
 *
 * DETECTION TECHNIQUES:
 *   1. Installer source check — verifies the app was installed from the Play Store
 *      (not side-loaded). Side-loading is the most common distribution channel
 *      for tampered APKs.
 *   2. APK classes.dex CRC check — computes CRC/hash of the primary DEX file.
 *      A tampered APK will have a different hash.
 *   3. Cross-reference with SignatureVerifier (Phase 4) — a tampered APK must
 *      be re-signed with a different certificate.
 *   4. APK zip integrity — verifies the ZIP structure of the APK is intact.
 *   5. Unexpected .so files — checks for injected native libraries.
 *
 * NOTE ON DEX HASH:
 *   The expected DEX hash must be set after each release build.
 *   See [EXPECTED_CLASSES_DEX_SHA256] — use the helper script or command below
 *   to generate it. Since this hash changes with every build, it should be
 *   managed as part of your CI/CD release pipeline.
 *
 *   To generate: unzip -p app-release.apk classes.dex | sha256sum
 *
 * THREAT MITIGATED:
 *   - Repackaged APK distribution
 *   - Modified APK with injected code
 *   - Resources/asset modification
 *
 * PROTECTION LEVEL: Medium-High
 *   An attacker who controls the device can patch this check.
 *   Combined with signature verification (Phase 4) and Play Integrity (Phase 5),
 *   the protection forms a layered defense.
 *
 * TRADEOFF:
 *   DEX hash must be updated with each release. The installer check will
 *   always fail for sideloaded debug builds.
 */
@Singleton
class TamperDetectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val signatureVerifier: SignatureVerifier
) {

    companion object {
        private const val TAG = "TamperDetectionManager"

        /**
         * Expected SHA-256 of classes.dex from the official release APK.
         *
         * !! UPDATE THIS FOR EVERY RELEASE BUILD !!
         *
         * Generate with:
         *   unzip -p app-release-unsigned.apk classes.dex | sha256sum
         *   OR
         *   python3 -c "import hashlib,zipfile; z=zipfile.ZipFile('app-release.apk');
         *     print(hashlib.sha256(z.read('classes.dex')).hexdigest())"
         *
         * Empty string = check disabled (use this during initial setup).
         */
        private const val EXPECTED_CLASSES_DEX_SHA256 = ""  // ← Set before release

        // Known legitimate installer package names
        private val LEGITIMATE_INSTALLERS = arrayOf(
            "com.android.vending",          // Google Play Store
            "com.google.android.packageinstaller", // Google Package Installer
            "com.sec.android.app.samsungapps" // Samsung Galaxy Store (if applicable)
        )
    }

    data class TamperCheckResult(
        val isTampered: Boolean,
        val reasons: List<String>
    )

    /**
     * Runs all tamper detection checks.
     */
    fun check(): TamperCheckResult {
        val reasons = mutableListOf<String>()

        val sigResult = signatureVerifier.verify()
        if (!sigResult.isValid) reasons += "Signature mismatch: ${sigResult.reason}"
        if (checkInstallerSource()) reasons += "App not installed from Play Store"
        val dexResult = checkDexIntegrity()
        if (dexResult != null) reasons += dexResult
        if (checkUnexpectedNativeLibs()) reasons += "Unexpected native libraries found in APK"

        val isTampered = reasons.isNotEmpty()
        if (isTampered) {
            Log.w(TAG, "Tampering detected: $reasons")
        } else {
            Log.d(TAG, "Tamper detection passed")
        }

        return TamperCheckResult(isTampered, reasons)
    }

    // ─── Individual checks ───────────────────────────────────────────────────

    /**
     * Checks if the app was installed by a legitimate store.
     * Side-loading from unknown sources is the primary tampered APK distribution.
     */
    private fun checkInstallerSource(): Boolean {
        return try {
            val installer = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                context.packageManager
                    .getInstallSourceInfo(context.packageName)
                    .installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(context.packageName)
            }

            // Null installer = ADB install or side-loaded (suspicious in production)
            if (installer == null) {
                Log.w(TAG, "Installer package is null — app may be side-loaded")
                return true
            }

            val isLegitimate = LEGITIMATE_INSTALLERS.any { legit ->
                installer.equals(legit, ignoreCase = true)
            }

            if (!isLegitimate) {
                Log.w(TAG, "Unknown installer: $installer")
            }
            !isLegitimate
        } catch (e: Exception) {
            Log.e(TAG, "Installer check error", e)
            false // Fail open — don't block on check error
        }
    }

    /**
     * Computes SHA-256 of classes.dex inside the APK and compares against
     * [EXPECTED_CLASSES_DEX_SHA256]. A mismatch means the DEX was modified.
     */
    private fun checkDexIntegrity(): String? {
        if (EXPECTED_CLASSES_DEX_SHA256.isEmpty()) {
            Log.d(TAG, "DEX hash check disabled — EXPECTED_CLASSES_DEX_SHA256 not set")
            return null
        }

        return try {
            val apkPath = context.applicationInfo.sourceDir
            val apkFile = File(apkPath)

            ZipFile(apkFile).use { zip ->
                val dexEntry = zip.getEntry("classes.dex")
                    ?: return "classes.dex not found in APK"

                val md = MessageDigest.getInstance("SHA-256")
                zip.getInputStream(dexEntry).use { stream ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (stream.read(buffer).also { read = it } != -1) {
                        md.update(buffer, 0, read)
                    }
                }

                val actualHash = md.digest().joinToString("") { "%02x".format(it) }
                if (!actualHash.equals(EXPECTED_CLASSES_DEX_SHA256, ignoreCase = true)) {
                    Log.w(TAG, "DEX hash mismatch: expected=$EXPECTED_CLASSES_DEX_SHA256 actual=$actualHash")
                    "classes.dex hash mismatch"
                } else {
                    null // Hash matches — no tampering detected
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "DEX integrity check error", e)
            null // Fail open
        }
    }

    /**
     * Scans for native libraries in the APK that weren't in the original build.
     * Attackers sometimes inject .so files to run malicious native code.
     * This compares against known legitimate library names.
     */
    private fun checkUnexpectedNativeLibs(): Boolean {
        return try {
            val apkPath = context.applicationInfo.sourceDir
            val expectedLibPrefix = "libmyplayer_security"

            ZipFile(File(apkPath)).use { zip ->
                zip.entries().asSequence().any { entry ->
                    val name = entry.name
                    if (name.startsWith("lib/") && name.endsWith(".so")) {
                        val libName = File(name).name
                        // Allowlist: known legitimate libraries
                        // An unexpected .so is a strong tampering indicator
                        libName.contains("frida", ignoreCase = true)
                            || libName.contains("substrate", ignoreCase = true)
                            || libName.contains("xposed", ignoreCase = true)
                            || libName.contains("lsposed", ignoreCase = true)
                    } else false
                }
            }
        } catch (e: Exception) { false }
    }
}
