package com.innovcrest.utildroid.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.innovcrest.utildroid.MainActivity
import com.innovcrest.utildroid.R
import com.innovcrest.utildroid.data.CatalogApp

object Notifier {
    private const val CHANNEL_UPDATES = "updates"
    private const val ID_UPDATES = 1001
    private const val ID_CONFIRM_BASE = 2000

    fun createChannels(context: Context) {
        val channel = NotificationChannel(CHANNEL_UPDATES, "App updates", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "New versions of your apps"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun showUpdates(context: Context, apps: List<CatalogApp>) {
        if (!canPost(context) || apps.isEmpty()) return
        val title = if (apps.size == 1) "Update ready for ${apps.first().name}" else "${apps.size} updates available"
        val text = apps.joinToString(", ") { it.name }
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN, MainActivity.OPEN_UPDATES)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_stat_update)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        notify(context, ID_UPDATES, notification)
    }

    fun cancelUpdates(context: Context) = NotificationManagerCompat.from(context).cancel(ID_UPDATES)

    fun showConfirmInstall(context: Context, app: CatalogApp, confirmIntent: Intent) {
        if (!canPost(context)) return
        val id = ID_CONFIRM_BASE + (app.packageName.hashCode() and 0xFFF)
        val pending = PendingIntent.getActivity(
            context, id, confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_stat_update)
            .setContentTitle("Tap to finish updating ${app.name}")
            .setContentText("Android needs a one-time confirmation")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        notify(context, id, notification)
    }

    private fun canPost(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)

    @android.annotation.SuppressLint("MissingPermission")
    private fun notify(context: Context, id: Int, notification: android.app.Notification) {
        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
