package com.innovcrest.utildroid.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.innovcrest.utildroid.BuildConfig
import com.innovcrest.utildroid.UtilDroidApp
import com.innovcrest.utildroid.data.CatalogApp
import com.innovcrest.utildroid.data.CatalogState
import com.innovcrest.utildroid.data.InstalledInfo
import com.innovcrest.utildroid.data.Settings
import com.innovcrest.utildroid.installer.ApkVerifier
import com.innovcrest.utildroid.installer.InstallPhase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AppStatus {
    data object Unavailable : AppStatus
    data object NotInstalled : AppStatus
    data class Installed(val versionName: String) : AppStatus
    data class UpdateAvailable(val installedVersionName: String) : AppStatus
    data class Busy(val phase: InstallPhase) : AppStatus
}

data class AppUi(
    val app: CatalogApp,
    val status: AppStatus,
    val installed: InstalledInfo?,
    val isSelf: Boolean,
)

data class StoreUi(
    val loaded: Boolean = false,
    /** Catalog apps for this phone, without the store itself. */
    val apps: List<AppUi> = emptyList(),
    /** The store's own catalog entry, for self-update. */
    val self: AppUi? = null,
    val refreshing: Boolean = false,
    val lastRefreshedMillis: Long = 0,
    val error: String? = null,
    val signingCert: String = "",
    val canInstall: Boolean = true,
) {
    val all: List<AppUi> get() = apps + listOfNotNull(self)
    val categories: List<String> get() = apps.map { it.app.category }.distinct().sorted()

    /** Installed apps with a newer version, including ones mid-update. */
    val updates: List<AppUi>
        get() = all.filter { it.installed != null && (it.status is AppStatus.UpdateAvailable || it.status is AppStatus.Busy) }

    val pendingUpdateCount: Int get() = all.count { it.status is AppStatus.UpdateAvailable }
}

class StoreViewModel(application: Application) : AndroidViewModel(application) {

    private val c = (application as UtilDroidApp).container
    private val ownPackage = application.packageName
    private val canInstall = MutableStateFlow(c.installManager.canInstallPackages())

    val ownVersion: String = BuildConfig.VERSION_NAME
    val ownCertFingerprint: String = ApkVerifier.ownCertFingerprint(application)

    val ui: StateFlow<StoreUi> = combine(
        c.catalogRepository.state,
        c.installedApps.installed,
        c.installManager.phases,
        canInstall,
    ) { catalogState, installed, phases, can -> build(catalogState, installed, phases, can) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, StoreUi())

    val settings: StateFlow<Settings> =
        c.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    init {
        refresh()
    }

    private fun build(
        cs: CatalogState,
        installed: Map<String, InstalledInfo>,
        phases: Map<String, InstallPhase>,
        can: Boolean,
    ): StoreUi {
        val items = cs.catalog?.apps.orEmpty().map { app ->
            val have = installed[app.packageName]
            val phase = phases[app.packageName]
            val status = when {
                phase != null -> AppStatus.Busy(phase)
                !app.hasRelease -> AppStatus.Unavailable
                have == null -> AppStatus.NotInstalled
                have.versionCode < app.versionCode -> AppStatus.UpdateAvailable(have.versionName)
                else -> AppStatus.Installed(have.versionName)
            }
            AppUi(app, status, have, isSelf = app.packageName == ownPackage)
        }
        return StoreUi(
            loaded = cs.catalog != null,
            apps = items.filter { !it.isSelf && it.app.supportsPhone },
            self = items.firstOrNull { it.isSelf },
            refreshing = cs.refreshing,
            lastRefreshedMillis = cs.lastRefreshedMillis,
            error = cs.error,
            signingCert = cs.catalog?.store?.signingCertSha256.orEmpty(),
            canInstall = can,
        )
    }

    fun refresh() {
        viewModelScope.launch { c.catalogRepository.refresh() }
    }

    /** Re-read things the user may have changed in system settings. */
    fun onResume() {
        canInstall.value = c.installManager.canInstallPackages()
        c.installedApps.refresh()
    }

    /** The one button on every card: Install / Update / Open / Cancel / Retry. */
    fun primaryAction(item: AppUi) {
        when (val s = item.status) {
            AppStatus.NotInstalled, is AppStatus.UpdateAvailable -> install(item)
            is AppStatus.Installed -> open(item)
            is AppStatus.Busy -> when (s.phase) {
                is InstallPhase.Downloading -> cancel(item)
                is InstallPhase.Failed -> install(item)
                else -> Unit
            }
            AppStatus.Unavailable -> Unit
        }
    }

    fun install(item: AppUi) {
        if (!canInstall.value) return
        c.installManager.clearFailure(item.app.packageName)
        c.installManager.start(item.app, ui.value.signingCert)
    }

    /** Updates every app except the store itself, which restarts the process when it updates. */
    fun updateAll() {
        ui.value.updates.filter { it.status is AppStatus.UpdateAvailable && !it.isSelf }.forEach { install(it) }
    }

    fun cancel(item: AppUi) = c.installManager.cancel(item.app.packageName)
    fun open(item: AppUi) {
        c.installManager.launch(item.app.packageName)
    }
    fun uninstall(item: AppUi) = c.installManager.uninstall(item.app.packageName)

    fun setCatalogUrl(url: String) {
        viewModelScope.launch {
            c.settings.setCatalogUrl(url)
            c.catalogRepository.refresh()
        }
    }

    fun setCheckInterval(hours: Int) = viewModelScope.launch { c.settings.setCheckInterval(hours) }
    fun setWifiOnly(value: Boolean) = viewModelScope.launch { c.settings.setWifiOnly(value) }
    fun setSilentUpdates(value: Boolean) = viewModelScope.launch { c.settings.setSilentUpdates(value) }
    fun setNotify(value: Boolean) = viewModelScope.launch { c.settings.setNotify(value) }
}
