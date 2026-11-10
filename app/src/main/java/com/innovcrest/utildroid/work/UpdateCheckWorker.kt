package com.innovcrest.utildroid.work

import android.content.Context
import android.os.Build
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.innovcrest.utildroid.UtilDroidApp
import com.innovcrest.utildroid.installer.InstallResult
import java.util.concurrent.TimeUnit

/**
 * Periodic check: refresh the catalog, find newer versionCodes, then either
 * update silently (Android 12+, setting on) or post one notification.
 */
class UpdateCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val c = (applicationContext as UtilDroidApp).container
        val catalog = c.catalogRepository.refresh().getOrElse { return Result.retry() }
        c.installedApps.track(catalog.apps.map { it.packageName }.toSet())
        val installed = c.installedApps.installed.value

        val updates = catalog.apps.filter { app ->
            val have = installed[app.packageName] ?: return@filter false
            app.hasRelease && app.versionCode > have.versionCode
        }
        if (updates.isEmpty()) {
            Notifier.cancelUpdates(applicationContext)
            return Result.success()
        }

        val settings = c.settings.current()
        val silentPossible = settings.silentUpdates && Build.VERSION.SDK_INT >= 31 && c.installManager.canInstallPackages()

        // Install the store itself last: updating it ends this process.
        val ordered = updates.sortedBy { it.packageName == applicationContext.packageName }
        val notDone = if (silentPossible) {
            ordered.filterNot { app ->
                c.installManager.install(app, catalog.store.signingCertSha256, background = true) == InstallResult.Success
            }
        } else {
            ordered
        }

        if (notDone.isNotEmpty() && settings.notify) Notifier.showUpdates(applicationContext, notDone)
        else if (notDone.isEmpty()) Notifier.cancelUpdates(applicationContext)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "update-check"

        fun schedule(context: Context, intervalHours: Int, wifiOnly: Boolean) {
            val wm = WorkManager.getInstance(context)
            if (intervalHours <= 0) {
                wm.cancelUniqueWork(WORK_NAME)
                return
            }
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                .setRequiresStorageNotLow(true)
                .build()
            val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(intervalHours.toLong(), TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()
            wm.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }
}
