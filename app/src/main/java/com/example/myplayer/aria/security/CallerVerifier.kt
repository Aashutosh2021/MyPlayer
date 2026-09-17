package com.example.myplayer.aria.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.util.Log
import com.example.myplayer.security.SignatureVerifier
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallerVerifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val signatureVerifier: SignatureVerifier
) {
    companion object {
        private const val TAG = "CallerVerifier"
    }

    /**
     * Checks if the calling UID is trusted.
     * Dual-Layer Security Verification:
     * 1. Requires caller to hold the signature-restricted custom permission com.example.myplayer.permission.ARIA_CONNECT.
     * 2. Checks package name against the allowed list and verifies its SHA-256 fingerprint.
     * 3. For debug builds, allows debug-signed packages dynamically.
     */
    fun isCallerTrusted(): Boolean {
        val callingUid = Binder.getCallingUid()
        val ownUid = android.os.Process.myUid()

        // Local process calls are always trusted
        if (callingUid == ownUid) {
            return true
        }

        val pm = context.packageManager
        val callingPackages = pm.getPackagesForUid(callingUid) ?: return false

        // Check Layer 1: signature permission check
        val permissionGranted = context.checkPermission(
            "com.example.myplayer.permission.ARIA_CONNECT",
            Binder.getCallingPid(),
            callingUid
        ) == PackageManager.PERMISSION_GRANTED

        if (permissionGranted) {
            Log.d(TAG, "Caller verified successfully via custom Signature Permission (UID: $callingUid)")
            return true
        }

        // Check Layer 2: Trusted package list + SHA256 fingerprint verification
        val isHostDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

        for (packageName in callingPackages) {
            try {
                val callerFingerprint = getSigningCertificateSHA256(packageName)

                // If host is debuggable, allow debug-signed calling packages
                if (isHostDebuggable) {
                    val callingAppInfo = pm.getApplicationInfo(packageName, 0)
                    if ((callingAppInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                        Log.w(TAG, "Trusting debug-signed caller $packageName because host is running a debug build.")
                        return true
                    }
                }

                // Verify fingerprint in static allowlist
                val trustedHashes = AriaSecurityConfig.TRUSTED_PACKAGES[packageName]
                if (trustedHashes != null) {
                    for (allowedHash in trustedHashes) {
                        if (allowedHash.equals(callerFingerprint, ignoreCase = true)) {
                            Log.i(TAG, "Caller verified via secure allowlist: $packageName (UID: $callingUid)")
                            return true
                        }
                    }
                }

                // Verify placeholder cert during testing
                val ownFingerprint = signatureVerifier.getSigningCertificateSHA256()
                if (ownFingerprint.startsWith("AA:BB:CC") && callerFingerprint.startsWith("AA:BB:CC")) {
                    Log.w(TAG, "Trusted placeholder cert for test/development build caller: $packageName")
                    return true
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error resolving signature credentials for package: $packageName", e)
            }
        }

        Log.e(TAG, "Access Denied: Caller UID $callingUid (packages: ${callingPackages.joinToString()}) does not hold custom permission or certificate verification mismatch.")
        return false
    }

    /**
     * Helper to compute SHA-256 certificate fingerprint for a given package name.
     */
    @Suppress("DEPRECATION")
    private fun getSigningCertificateSHA256(packageName: String): String {
        val signingInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val pm = context.packageManager
            val pkgInfo = pm.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNING_CERTIFICATES
            )
            pkgInfo.signingInfo?.apkContentsSigners
        } else {
            val pkgInfo = context.packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNATURES
            )
            @Suppress("DEPRECATION")
            pkgInfo.signatures
        }

        val cert = signingInfo?.firstOrNull()
            ?: throw SecurityException("No signing certificate found for package: $packageName")

        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(cert.toByteArray())
        return digest.joinToString(":") { "%02X".format(it) }
    }
}
