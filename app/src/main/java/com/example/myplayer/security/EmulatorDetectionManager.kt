package com.example.myplayer.security

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * EmulatorDetectionManager — Phase 8 Security Hardening
 *
 * PURPOSE:
 *   Detects whether the app is running inside an Android emulator or simulator.
 *   Emulators are commonly used for automated analysis, malware reverse-engineering,
 *   API scraping, and bypassing security controls.
 *
 * DETECTION TECHNIQUES:
 *   1. Build properties (manufacturer, model, hardware, fingerprint, product)
 *      → Covers Android Studio Emulator, Genymotion, QEMU
 *   2. BlueStacks-specific files and properties
 *   3. Nox Player specific paths
 *   4. LDPlayer specific indicators
 *   5. Telephony indicators (emulators use fake IMEI/operator data)
 *   6. Sensor presence (emulators often lack accelerometer/gyroscope)
 *   7. CPU ABI (x86/x86_64 on non-Intel physical devices)
 *   8. QEMU-specific system files and properties
 *
 * THREAT MITIGATED:
 *   - Automated reverse engineering via emulator+Frida setups
 *   - Bot/scraping attacks in apps with account systems
 *   - API key extraction via automated analysis pipelines
 *
 * PROTECTION LEVEL: Medium
 *   Modern emulators (Google Play Store-enabled AVDs, Genymotion Cloud) can
 *   spoof most Build.* properties. Combine with Play Integrity (Phase 5) which
 *   uses hardware attestation to detect virtualization at a deeper level.
 *
 * TRADEOFF:
 *   x86 CPU check may produce false positives on Intel Chromebooks running
 *   Android (they are real devices). The check is factored with a lower
 *   confidence weight in [isEmulator].
 */
@Singleton
class EmulatorDetectionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "EmulatorDetectionManager"

        // Known emulator manufacturers and hardware identifiers
        private val EMULATOR_MANUFACTURERS = arrayOf(
            "Genymotion", "unknown", "Google", "Andy", "MIT", "nox", "TiantianVM"
        )
        private val EMULATOR_MODELS = arrayOf(
            "sdk", "Emulator", "Android SDK built for x86",
            "Genymotion", "Andy", "TTPOD", "Benymotion",
            "virtual", "emulator", "droid4x"
        )
        private val EMULATOR_HARDWARE = arrayOf(
            "goldfish", "ranchu", "vbox86", "nox", "ttVM_hdragon",
            "droid4x", "andy", "x86"
        )
        private val EMULATOR_PRODUCTS = arrayOf(
            "sdk_google_phone_x86", "sdk_x86", "vbox86p", "emulator_arm",
            "nox", "andy", "droid4x", "sdk_gphone_x86", "sdk_gphone64_x86_64"
        )
        private val EMULATOR_FINGERPRINTS = arrayOf(
            "generic", "unknown", "google/sdk_gphone_x86",
            "Android/sdk_gphone_x86", "generic/vbox86p",
            ":userdebug/test-keys"
        )

        // BlueStacks-specific indicators
        private val BLUESTACKS_PATHS = arrayOf(
            "/data/app/com.bluestacks.home", "/data/data/com.bluestacks.home",
            "/system/priv-app/BlueStacksUI.apk",
            "/.bluestacks.prop"
        )

        // Nox Player indicators
        private val NOX_PATHS = arrayOf(
            "/system/lib/libnoxspeedup.so", "/system/priv-app/NoxLogin.apk"
        )

        // LDPlayer indicators
        private val LD_PLAYER_PATHS = arrayOf(
            "/system/lib/libldk.so", "/system/priv-app/LDPlayer.apk"
        )

        // QEMU-specific files
        private val QEMU_FILES = arrayOf(
            "/dev/socket/qemud", "/dev/qemu_trace", "/system/lib/libc_malloc_debug_qemu.so",
            "/sys/qemu_trace", "/system/bin/qemu-props"
        )
    }

    data class EmulatorCheckResult(
        val isEmulator: Boolean,
        val confidence: Float,  // 0.0 – 1.0
        val reasons: List<String>
    )

    /**
     * Runs all emulator detection checks.
     * Returns a confidence score (0.0 = definitely real device, 1.0 = definitely emulator).
     * Threshold of 0.4 or more is treated as a detected emulator.
     */
    fun check(): EmulatorCheckResult {
        val reasons = mutableListOf<String>()
        var score = 0f

        if (checkBuildProperties()) { reasons += "suspicious build properties"; score += 0.35f }
        if (checkBlueStacks()) { reasons += "BlueStacks files detected"; score += 0.5f }
        if (checkNox()) { reasons += "Nox Player files detected"; score += 0.5f }
        if (checkLDPlayer()) { reasons += "LDPlayer files detected"; score += 0.5f }
        if (checkQEMUFiles()) { reasons += "QEMU system files detected"; score += 0.4f }
        if (checkTelephony()) { reasons += "fake telephony data"; score += 0.25f }
        if (checkSensors()) { reasons += "missing physical sensors"; score += 0.2f }
        if (checkX86Architecture()) { reasons += "x86 CPU on non-Intel device"; score += 0.15f }
        if (checkFingerprint()) { reasons += "emulator build fingerprint"; score += 0.35f }

        val confidence = score.coerceIn(0f, 1f)
        val isEmulator = confidence >= 0.4f

        if (isEmulator) {
            Log.w(TAG, "Emulator detected (confidence=${confidence}): $reasons")
        } else {
            Log.d(TAG, "Emulator check passed (confidence=${confidence})")
        }

        return EmulatorCheckResult(isEmulator, confidence, reasons)
    }

    // ─── Individual checks ───────────────────────────────────────────────────

    private fun checkBuildProperties(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val model = Build.MODEL.lowercase()
        val hardware = Build.HARDWARE.lowercase()
        val product = Build.PRODUCT.lowercase()

        return EMULATOR_MANUFACTURERS.any { manufacturer.contains(it.lowercase()) }
            || EMULATOR_MODELS.any { model.contains(it.lowercase()) }
            || EMULATOR_HARDWARE.any { hardware.contains(it.lowercase()) }
            || EMULATOR_PRODUCTS.any { product.contains(it.lowercase()) }
    }

    private fun checkFingerprint(): Boolean {
        val fingerprint = Build.FINGERPRINT.lowercase()
        return EMULATOR_FINGERPRINTS.any { fingerprint.contains(it.lowercase()) }
    }

    private fun checkBlueStacks(): Boolean = BLUESTACKS_PATHS.any { File(it).exists() }
    private fun checkNox(): Boolean = NOX_PATHS.any { File(it).exists() }
    private fun checkLDPlayer(): Boolean = LD_PLAYER_PATHS.any { File(it).exists() }
    private fun checkQEMUFiles(): Boolean = QEMU_FILES.any { File(it).exists() }

    private fun checkTelephony(): Boolean {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                ?: return false
            val networkOperator = tm.networkOperatorName
            // Emulators often report "Android" as the network operator
            networkOperator.equals("Android", ignoreCase = true)
                || networkOperator.isBlank()
        } catch (e: Exception) { false }
    }

    /**
     * Most emulators don't implement physical sensors.
     * A real device will always have at least an accelerometer.
     */
    private fun checkSensors(): Boolean {
        return try {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
                ?: return true
            val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            accelerometer == null // No accelerometer = strong emulator indicator
        } catch (e: Exception) { false }
    }

    /**
     * Physical Android devices are almost universally ARM.
     * x86/x86_64 suggests an emulator (though Intel Chromebooks are an exception).
     */
    private fun checkX86Architecture(): Boolean {
        return try {
            Build.SUPPORTED_ABIS.any { abi ->
                abi.startsWith("x86") && !Build.HARDWARE.contains("fugu", ignoreCase = true)
            }
        } catch (e: Exception) { false }
    }
}
