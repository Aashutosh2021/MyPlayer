package com.example.myplayer.security

import android.content.Context
import android.util.Log
import com.example.myplayer.BuildConfig
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * IntegrityManager — Phase 5 Security Hardening
 *
 * PURPOSE:
 *   Uses the Google Play Integrity API to obtain a cryptographically signed
 *   verdict from Google's servers that attests:
 *     - APP_RECOGNIZED: The APK was distributed via the Play Store
 *     - MEETS_DEVICE_INTEGRITY: The device passes Android CTS / Play Protect checks
 *     - MEETS_BASIC_INTEGRITY: The device has not been tampered with at the OS level
 *
 * SETUP REQUIRED:
 *   1. In Google Play Console → your app → Setup → App Integrity
 *      Enable Standard API requests.
 *   2. In local.properties (NOT committed to VCS), add:
 *         play.integrity.projectNumber=<YOUR_GCP_PROJECT_NUMBER>
 *      The project number is found in Google Cloud Console → Project Settings.
 *   3. Gradle reads this into BuildConfig.PLAY_INTEGRITY_PROJECT_NUMBER (see build.gradle.kts).
 *
 * ARCHITECTURE:
 *   - Uses the Standard API (not the Classic API) for lower latency.
 *   - The token is verified SERVER-SIDE ideally. For this music player (no backend),
 *     we perform client-side verdict evaluation, which is weaker but still useful
 *     as a layered defence.
 *
 * THREAT MITIGATED:
 *   - APKs side-loaded or distributed outside Play Store
 *   - Rooted devices with high confidence (MEETS_DEVICE_INTEGRITY will fail)
 *   - Emulators (MEETS_BASIC_INTEGRITY often fails on unregistered emulators)
 *
 * PROTECTION LEVEL: High (when verified server-side) / Medium (client-side only)
 *
 * TRADEOFF:
 *   - Requires network connectivity for the first token request.
 *   - Verdict tokens expire; warmup must be called periodically.
 *   - Google may rate-limit requests (Standard API: 10k req/day free tier).
 */
@Singleton
class IntegrityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "IntegrityManager"

        // Verdict labels from the Play Integrity API response
        private const val VERDICT_APP_RECOGNIZED = "PLAY_RECOGNIZED"
        private const val VERDICT_MEETS_DEVICE = "MEETS_DEVICE_INTEGRITY"
        private const val VERDICT_MEETS_BASIC = "MEETS_BASIC_INTEGRITY"
        private const val VERDICT_MEETS_STRONG = "MEETS_STRONG_INTEGRITY"
    }

    data class IntegrityVerdict(
        val appRecognized: Boolean,
        val meetsDeviceIntegrity: Boolean,
        val meetsBasicIntegrity: Boolean,
        val meetsStrongIntegrity: Boolean,
        val isAvailable: Boolean,
        val error: String? = null
    ) {
        val isTrusted: Boolean
            get() = isAvailable && meetsBasicIntegrity && meetsDeviceIntegrity

        val isFullyTrusted: Boolean
            get() = isTrusted && appRecognized && meetsStrongIntegrity
    }

    private var standardIntegrityManager: StandardIntegrityManager? = null
    private var tokenProvider: StandardIntegrityManager.StandardIntegrityTokenProvider? = null

    /**
     * Warms up the Play Integrity token provider.
     * Call this early in application startup (e.g., Application.onCreate or MainActivity.onStart)
     * to minimize latency when [requestVerdict] is called later.
     *
     * Safe to call multiple times — subsequent calls are no-ops.
     */
    fun warmup() {
        val projectNumber = BuildConfig.PLAY_INTEGRITY_PROJECT_NUMBER.toLongOrNull()
        if (projectNumber == null || projectNumber == 0L) {
            Log.w(TAG, "Play Integrity project number not configured. Set play.integrity.projectNumber in local.properties.")
            return
        }

        try {
            val manager = IntegrityManagerFactory.createStandard(context)
            standardIntegrityManager = manager

            val request = StandardIntegrityManager.PrepareIntegrityTokenRequest.builder()
                .setCloudProjectNumber(projectNumber)
                .build()

            manager.prepareIntegrityToken(request)
                .addOnSuccessListener { provider ->
                    tokenProvider = provider
                    Log.d(TAG, "Play Integrity token provider ready")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to prepare integrity token provider", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Play Integrity warmup failed", e)
        }
    }

    /**
     * Requests a Play Integrity verdict.
     *
     * @param requestHash An optional hash of the action being protected (e.g., SHA-256 of
     *                    a user action). Binds the token to a specific request.
     *                    Max 500 bytes, Base64-encoded.
     *
     * @return [IntegrityVerdict] with the parsed verdict fields.
     *
     * NOTE: For production with a backend server, send the token string to your
     *       server and call the Play Integrity API there to avoid token replay attacks.
     *       The token contains the verdict signed by Google — server-side verification
     *       is the only truly tamper-proof approach.
     */
    suspend fun requestVerdict(requestHash: String? = null): IntegrityVerdict {
        val provider = tokenProvider
        if (provider == null) {
            return IntegrityVerdict(
                appRecognized = false,
                meetsDeviceIntegrity = false,
                meetsBasicIntegrity = false,
                meetsStrongIntegrity = false,
                isAvailable = false,
                error = "Token provider not ready. Call warmup() first."
            )
        }

        return suspendCancellableCoroutine { continuation ->
            try {
                val requestBuilder = StandardIntegrityManager.StandardIntegrityTokenRequest.builder()
                if (requestHash != null) {
                    requestBuilder.setRequestHash(requestHash)
                }

                provider.request(requestBuilder.build())
                    .addOnSuccessListener { response ->
                        val token = response.token()
                        Log.d(TAG, "Integrity token obtained (length=${token.length})")
                        // In a real production app: send token to your backend for server-side verification.
                        // Here we do lightweight client-side parsing for the music player use case.
                        val verdict = parseTokenClientSide(token)
                        continuation.resume(verdict)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Integrity token request failed", e)
                        continuation.resume(
                            IntegrityVerdict(
                                appRecognized = false,
                                meetsDeviceIntegrity = false,
                                meetsBasicIntegrity = false,
                                meetsStrongIntegrity = false,
                                isAvailable = true,
                                error = e.message
                            )
                        )
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception requesting integrity verdict", e)
                continuation.resume(
                    IntegrityVerdict(
                        appRecognized = false,
                        meetsDeviceIntegrity = false,
                        meetsBasicIntegrity = false,
                        meetsStrongIntegrity = false,
                        isAvailable = false,
                        error = e.message
                    )
                )
            }
        }
    }

    /**
     * Lightweight client-side token parsing.
     *
     * IMPORTANT: This is a FALLBACK for apps without a backend server.
     * For production, decode the token JWT on your server and verify with
     * Google's Play Integrity API: https://developer.android.com/google/play/integrity/verdict
     *
     * The token is a JWT: header.payload.signature (Base64url encoded).
     * The payload contains the verdict JSON.
     */
    private fun parseTokenClientSide(token: String): IntegrityVerdict {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) {
                return IntegrityVerdict(false, false, false, false, true, "Invalid token format")
            }
            // Base64url decode the payload
            val payload = String(
                android.util.Base64.decode(
                    parts[1].replace('-', '+').replace('_', '/'),
                    android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
                ),
                Charsets.UTF_8
            )

            val appRecognized = payload.contains(VERDICT_APP_RECOGNIZED)
            val meetsDevice = payload.contains(VERDICT_MEETS_DEVICE)
            val meetsBasic = payload.contains(VERDICT_MEETS_BASIC)
            val meetsStrong = payload.contains(VERDICT_MEETS_STRONG)

            Log.d(TAG, "Integrity verdict — app=$appRecognized device=$meetsDevice basic=$meetsBasic strong=$meetsStrong")

            IntegrityVerdict(
                appRecognized = appRecognized,
                meetsDeviceIntegrity = meetsDevice,
                meetsBasicIntegrity = meetsBasic,
                meetsStrongIntegrity = meetsStrong,
                isAvailable = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse integrity token payload", e)
            IntegrityVerdict(false, false, false, false, true, "Parse error: ${e.message}")
        }
    }
}
