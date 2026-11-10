package com.innovcrest.utildroid.installer

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/** The two checks every APK passes before it reaches PackageInstaller. */
object ApkVerifier {

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun verifyHash(file: File, expected: String) {
        if (expected.isBlank()) return
        val actual = sha256(file)
        if (!actual.equals(expected.trim(), ignoreCase = true)) {
            throw IOException("Download corrupted (checksum mismatch), retry")
        }
    }

    /**
     * Checks the APK's package name, and that its signing certificate matches the
     * store's release key. A blank [expectedCert] skips the certificate check.
     */
    fun verifyArchive(context: Context, file: File, expectedPackage: String, expectedCert: String) {
        val info = archiveInfo(context, file) ?: throw IOException("Not a valid APK")
        if (info.packageName != expectedPackage) {
            throw IOException("APK is ${info.packageName}, expected $expectedPackage")
        }
        if (expectedCert.isBlank()) return
        val fingerprints = signatures(info).map { fingerprint(it) }
        if (fingerprints.isEmpty()) throw IOException("APK is not signed")
        if (fingerprints.none { normalize(it) == normalize(expectedCert) }) {
            throw IOException("APK is signed with an unknown key, install blocked")
        }
    }

    /** SHA-256 of this app's own signing cert, shown in Settings for copying into apps.yaml. */
    fun ownCertFingerprint(context: Context): String = runCatching {
        val pm = context.packageManager
        val info = if (Build.VERSION.SDK_INT >= 28) {
            if (Build.VERSION.SDK_INT >= 33) {
                pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()))
            } else {
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            }
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
        }
        signatures(info).firstOrNull()?.let { fingerprint(it) } ?: ""
    }.getOrDefault("")

    private fun archiveInfo(context: Context, file: File): PackageInfo? {
        val pm = context.packageManager
        return when {
            Build.VERSION.SDK_INT >= 33 -> pm.getPackageArchiveInfo(
                file.absolutePath,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()),
            )
            Build.VERSION.SDK_INT >= 28 -> pm.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
            else -> @Suppress("DEPRECATION") pm.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_SIGNATURES)
        }
    }

    private fun signatures(info: PackageInfo): List<Signature> {
        if (Build.VERSION.SDK_INT >= 28) {
            val signing = info.signingInfo ?: return emptyList()
            val sigs = if (signing.hasMultipleSigners()) signing.apkContentsSigners else signing.signingCertificateHistory
            return sigs?.toList() ?: emptyList()
        }
        @Suppress("DEPRECATION")
        return info.signatures?.toList() ?: emptyList()
    }

    private fun fingerprint(sig: Signature): String =
        MessageDigest.getInstance("SHA-256").digest(sig.toByteArray()).joinToString(":") { "%02X".format(it) }

    private fun normalize(value: String) = value.replace(":", "").trim().uppercase()
}
