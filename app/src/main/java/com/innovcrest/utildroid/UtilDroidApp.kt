package com.innovcrest.utildroid

import android.app.Application
import com.innovcrest.utildroid.work.Notifier
import com.innovcrest.utildroid.work.UpdateCheckWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class UtilDroidApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifier.createChannels(this)
        container.installedApps.register()

        // Keep installed-version tracking in step with the catalog.
        appScope.launch {
            container.catalogRepository.state
                .map { it.catalog?.apps?.map { app -> app.packageName }?.toSet() }
                .filterNotNull()
                .distinctUntilChanged()
                .collect { container.installedApps.track(it) }
        }

        // Reschedule background checks whenever their settings change.
        appScope.launch {
            container.settings.settings
                .map { it.checkIntervalHours to it.wifiOnly }
                .distinctUntilChanged()
                .collect { (hours, wifiOnly) -> UpdateCheckWorker.schedule(this@UtilDroidApp, hours, wifiOnly) }
        }
    }
}
