package com.innovcrest.utildroid

import android.content.Context
import com.innovcrest.utildroid.data.CatalogRepository
import com.innovcrest.utildroid.data.InstalledApps
import com.innovcrest.utildroid.data.SettingsStore
import com.innovcrest.utildroid.installer.InstallManager
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Hand-wired dependencies; small enough that a DI framework isn't worth it. */
class AppContainer(context: Context) {
    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }
    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
    val settings = SettingsStore(context)
    val installedApps = InstalledApps(context)
    val catalogRepository = CatalogRepository(context, http, json, settings)
    val installManager = InstallManager(context, http, installedApps, settings)
}
