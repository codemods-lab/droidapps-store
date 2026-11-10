package com.innovcrest.utildroid.installer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.core.content.IntentCompat
import com.innovcrest.utildroid.UtilDroidApp

/** Receives PackageInstaller session results and hands them to InstallManager. */
class InstallResultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getIntExtra(EXTRA_OUR_SESSION, intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1))
        if (sessionId == -1) return
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        val confirm = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)
        val app = context.applicationContext as UtilDroidApp
        app.container.installManager.onSessionResult(sessionId, status, message, confirm)
    }

    companion object {
        const val EXTRA_OUR_SESSION = "com.innovcrest.utildroid.SESSION_ID"
    }
}
