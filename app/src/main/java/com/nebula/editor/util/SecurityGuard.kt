package com.nebula.editor.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Debug
import com.scottyab.rootbeer.RootBeer
import timber.log.Timber
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityGuard @Inject constructor() {

    fun runChecks(context: Context) {
        if (isDebuggerAttached()) {
            triggerDefense("Debugger detected")
        }
        if (isEmulatorSuspicious()) {
            Timber.w("Running on emulator — some features may be limited")
        }
        if (isRooted(context)) {
            Timber.w("Device appears rooted")
            // For stricter builds, call triggerDefense("Rooted device")
        }
        if (isSignatureTampered(context)) {
            triggerDefense("Signature mismatch — app tampered")
        }
    }

    // ── Checks ────────────────────────────────────────────────

    private fun isDebuggerAttached(): Boolean =
        Debug.isDebuggerConnected() || Debug.waitingForDebugger()

    private fun isEmulatorSuspicious(): Boolean {
        val indicators = listOf(
            Build.FINGERPRINT.startsWith("generic"),
            Build.FINGERPRINT.startsWith("unknown"),
            Build.MODEL.contains("google_sdk"),
            Build.MODEL.contains("Emulator"),
            Build.MODEL.contains("Android SDK built for x86"),
            Build.MANUFACTURER.contains("Genymotion"),
            (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")),
            "google_sdk" == Build.PRODUCT,
        )
        return indicators.count { it } >= 2
    }

    private fun isRooted(context: Context): Boolean =
        try { RootBeer(context).isRooted } catch (e: Exception) { false }

    private fun isSignatureTampered(context: Context): Boolean {
        return try {
            val pm = context.packageManager
            @Suppress("DEPRECATION")
            val info = pm.getPackageInfo(
                context.packageName,
                android.content.pm.PackageManager.GET_SIGNATURES
            )
            @Suppress("DEPRECATION")
            val sig = info.signatures[0]
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(sig.toByteArray())
            val hexSig = digest.joinToString("") { "%02x".format(it) }

            // In production, hardcode your release SHA-256 here:
            // val expectedSig = "YOUR_RELEASE_SHA256_HERE"
            // return hexSig != expectedSig

            // During development — always returns false
            false
        } catch (e: Exception) {
            Timber.e(e, "Signature check failed")
            false
        }
    }

    // ── Defense ───────────────────────────────────────────────

    private fun triggerDefense(reason: String) {
        Timber.e("SecurityGuard: $reason")
        // In production you can: throw RuntimeException() or android.os.Process.killProcess()
        // For now we just log — enable for release build
    }

    /** Anti-frida / anti-xposed check — scan loaded libs */
    fun checkForHooks(): Boolean {
        val suspiciousLibs = listOf("frida", "xposed", "substrate", "cydia")
        return try {
            val maps = File("/proc/self/maps").readText()
            suspiciousLibs.any { maps.contains(it, ignoreCase = true) }
        } catch (e: Exception) { false }
    }

    /** Detect if APK is running from a non-standard path (indicates repackaging) */
    fun checkApkPath(context: Context): Boolean {
        val sourceDir = context.applicationInfo.sourceDir
        return !sourceDir.startsWith("/data/app/")
    }
}
