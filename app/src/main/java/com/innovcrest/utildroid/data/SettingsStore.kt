package com.innovcrest.utildroid.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.innovcrest.utildroid.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ThemeMode { Light, Dark, System }

data class Settings(
    val catalogUrl: String = BuildConfig.DEFAULT_CATALOG_URL,
    /** 0 means background checks are off. */
    val checkIntervalHours: Int = 12,
    val wifiOnly: Boolean = true,
    val silentUpdates: Boolean = true,
    val notify: Boolean = true,
    /** Light unless the user picks otherwise; System follows the phone. */
    val themeMode: ThemeMode = ThemeMode.Light,
)

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    private object Keys {
        val catalogUrl = stringPreferencesKey("catalog_url")
        val interval = intPreferencesKey("check_interval_hours")
        val wifiOnly = booleanPreferencesKey("wifi_only")
        val silent = booleanPreferencesKey("silent_updates")
        val notify = booleanPreferencesKey("notify")
        val theme = stringPreferencesKey("theme_mode")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        val defaults = Settings()
        Settings(
            catalogUrl = p[Keys.catalogUrl] ?: defaults.catalogUrl,
            checkIntervalHours = p[Keys.interval] ?: defaults.checkIntervalHours,
            wifiOnly = p[Keys.wifiOnly] ?: defaults.wifiOnly,
            silentUpdates = p[Keys.silent] ?: defaults.silentUpdates,
            notify = p[Keys.notify] ?: defaults.notify,
            themeMode = p[Keys.theme]?.let { saved -> ThemeMode.entries.firstOrNull { it.name == saved } } ?: defaults.themeMode,
        )
    }

    suspend fun current(): Settings = settings.first()

    suspend fun setCheckInterval(hours: Int) = context.dataStore.edit { it[Keys.interval] = hours }
    suspend fun setWifiOnly(value: Boolean) = context.dataStore.edit { it[Keys.wifiOnly] = value }
    suspend fun setSilentUpdates(value: Boolean) = context.dataStore.edit { it[Keys.silent] = value }
    suspend fun setNotify(value: Boolean) = context.dataStore.edit { it[Keys.notify] = value }
    suspend fun setThemeMode(mode: ThemeMode) = context.dataStore.edit { it[Keys.theme] = mode.name }
}
