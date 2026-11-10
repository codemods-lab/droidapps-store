package com.innovcrest.utildroid.installer

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.innovcrest.utildroid.data.CatalogApp
import com.innovcrest.utildroid.data.InstalledApps
import com.innovcrest.utildroid.data.SettingsStore
import com.innovcrest.utildroid.work.Notifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext

sealed interface InstallPhase {
    data class Downloading(val progress: Float) : InstallPhase
    data object Verifying : InstallPhase
    data object Installing : InstallPhase
    data object WaitingForUser : InstallPhase
    data class Failed(val message: String) : InstallPhase
}

sealed interface InstallResult {
    data object Success : InstallResult
    data object NeedsUser : InstallResult
    data class Failure(val message: String) : InstallResult
}

/**
 * One pipeline for every install and update:
 * download → SHA-256 check → package + signing-cert check → PackageInstaller session.
 */
class InstallManager(
    private val context: Context,
    private val http: OkHttpClient,
    private val installedApps: InstalledApps,
    private val settings: SettingsStore,
) {
    private val _phases = MutableStateFlow<Map<String, InstallPhase>>(emptyMap())
    val phases: StateFlow<Map<String, InstallPhase>> = _phases.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = ConcurrentHashMap<String, Job>()
    private val pending = ConcurrentHashMap<Int, PendingSession>()

    private class PendingSession(
        val app: CatalogApp,
        val background: Boolean,
        val result: CompletableDeferred<InstallResult> = CompletableDeferred(),
    )

    fun canInstallPackages(): Boolean =
        Build.VERSION.SDK_INT < 26 || context.packageManager.canRequestPackageInstalls()

    /** Fire-and-forget install from the UI. */
    fun start(app: CatalogApp, expectedCert: String) {
        if (jobs[app.packageName]?.isActive == true) return
        jobs[app.packageName] = scope.launch { install(app, expectedCert, background = false) }
    }

    fun cancel(packageName: String) {
        jobs.remove(packageName)?.cancel()
        setPhase(packageName, null)
    }

    fun clearFailure(packageName: String) {
        if (_phases.value[packageName] is InstallPhase.Failed) setPhase(packageName, null)
    }

    /** Suspends until the install finishes. Background callers get NeedsUser instead of a prompt. */
    suspend fun install(app: CatalogApp, expectedCert: String, background: Boolean): InstallResult {
        val pkg = app.packageName
        val apk = File(context.cacheDir, "apks/${app.id}-${app.versionCode}.apk").apply { parentFile?.mkdirs() }
        return try {
            setPhase(pkg, InstallPhase.Downloading(0f))
            download(app, apk)

            setPhase(pkg, InstallPhase.Verifying)
            ApkVerifier.verifyHash(apk, app.sha256)
            ApkVerifier.verifyArchive(context, apk, app.packageName, expectedCert)

            setPhase(pkg, InstallPhase.Installing)
            when (val result = commit(app, apk, background)) {
                InstallResult.Success -> {
                    installedApps.refresh()
                    setPhase(pkg, null)
                    result
                }
                InstallResult.NeedsUser -> {
                    setPhase(pkg, null)
                    result
                }
                is InstallResult.Failure -> {
                    setPhase(pkg, InstallPhase.Failed(result.message))
                    result
                }
            }
        } catch (e: CancellationException) {
            setPhase(pkg, null)
            throw e
        } catch (e: Exception) {
            val message = e.message ?: "Install failed"
            setPhase(pkg, InstallPhase.Failed(message))
            InstallResult.Failure(message)
        } finally {
            apk.delete()
            jobs.remove(pkg)
        }
    }

    private suspend fun download(app: CatalogApp, target: File) = withContext(Dispatchers.IO) {
        if (app.apkUrl.isBlank()) throw IOException("No release published yet")
        val request = Request.Builder().url(app.apkUrl).build()
        http.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("Download failed (HTTP ${resp.code})")
            val body = resp.body ?: throw IOException("Empty download")
            val total = body.contentLength().takeIf { it > 0 } ?: app.sizeBytes
            var done = 0L
            var lastReported = -1
            body.byteStream().use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        coroutineContext.ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        done += read
                        if (total > 0) {
                            val pct = ((done * 100) / total).toInt().coerceIn(0, 100)
                            if (pct != lastReported) {
                                lastReported = pct
                                setPhase(app.packageName, InstallPhase.Downloading(pct / 100f))
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun commit(app: CatalogApp, apk: File, background: Boolean): InstallResult {
        val installer = context.packageManager.packageInstaller
        val silent = settings.current().silentUpdates
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(app.packageName)
            setSize(apk.length())
            setInstallReason(PackageManager.INSTALL_REASON_USER)
            if (Build.VERSION.SDK_INT >= 31 && silent) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }
        val sessionId = installer.createSession(params)
        val session = PendingSession(app, background)
        pending[sessionId] = session
        try {
            installer.openSession(sessionId).use { s ->
                s.openWrite("base.apk", 0, apk.length()).use { out ->
                    apk.inputStream().use { it.copyTo(out) }
                    s.fsync(out)
                }
                val intent = Intent(context, InstallResultReceiver::class.java)
                    .setPackage(context.packageName)
                    .putExtra(InstallResultReceiver.EXTRA_OUR_SESSION, sessionId)
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
                val callback = PendingIntent.getBroadcast(context, sessionId, intent, flags)
                s.commit(callback.intentSender)
            }
            return session.result.await()
        } catch (e: Exception) {
            runCatching { installer.abandonSession(sessionId) }
            throw e
        } finally {
            pending.remove(sessionId)
        }
    }

    // Called by InstallResultReceiver.
    internal fun onSessionResult(sessionId: Int, status: Int, message: String?, confirmIntent: Intent?) {
        val session = pending[sessionId] ?: return
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                if (confirmIntent == null) {
                    session.result.complete(InstallResult.Failure("Android asked for confirmation but gave no screen"))
                    return
                }
                val inForeground = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                if (inForeground && !session.background) {
                    setPhase(session.app.packageName, InstallPhase.WaitingForUser)
                    context.startActivity(confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } else {
                    Notifier.showConfirmInstall(context, session.app, confirmIntent)
                    if (session.background) session.result.complete(InstallResult.NeedsUser)
                    else setPhase(session.app.packageName, InstallPhase.WaitingForUser)
                }
            }
            PackageInstaller.STATUS_SUCCESS -> session.result.complete(InstallResult.Success)
            PackageInstaller.STATUS_FAILURE_ABORTED -> session.result.complete(InstallResult.Failure("Install cancelled"))
            PackageInstaller.STATUS_FAILURE_CONFLICT ->
                session.result.complete(InstallResult.Failure("Signed with a different key than the installed app. Uninstall it first."))
            PackageInstaller.STATUS_FAILURE_STORAGE -> session.result.complete(InstallResult.Failure("Not enough storage"))
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE ->
                session.result.complete(InstallResult.Failure("Not compatible with this device"))
            else -> session.result.complete(InstallResult.Failure(message ?: "Install failed"))
        }
    }

    fun launch(packageName: String): Boolean {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(packageName) ?: return false
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return true
    }

    fun uninstall(packageName: String) {
        @Suppress("DEPRECATION")
        val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.parse("package:$packageName"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun setPhase(packageName: String, phase: InstallPhase?) {
        _phases.update { current -> if (phase == null) current - packageName else current + (packageName to phase) }
    }
}
