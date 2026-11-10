package com.innovcrest.utildroid.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class InstalledInfo(
    val versionCode: Long,
    val versionName: String,
    val lastUpdateTime: Long,
)

/** Tracks which catalog packages are installed, live, via package broadcasts. */
class InstalledApps(private val context: Context) {

    private val _installed = MutableStateFlow<Map<String, InstalledInfo>>(emptyMap())
    val installed: StateFlow<Map<String, InstalledInfo>> = _installed.asStateFlow()

    @Volatile
    private var tracked: Set<String> = setOf(context.packageName)

    fun track(packages: Set<String>) {
        tracked = packages + context.packageName
        refresh()
    }

    fun refresh() {
        _installed.value = tracked.mapNotNull { pkg ->
            packageInfo(pkg)?.let { pi ->
                pkg to InstalledInfo(
                    versionCode = PackageInfoCompat.getLongVersionCode(pi),
                    versionName = pi.versionName ?: "",
                    lastUpdateTime = pi.lastUpdateTime,
                )
            }
        }.toMap()
    }

    fun register() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) = refresh()
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
    }

    private fun packageInfo(pkg: String): PackageInfo? = try {
        val pm = context.packageManager
        if (Build.VERSION.SDK_INT >= 33) {
            pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(pkg, 0)
        }
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }
}
