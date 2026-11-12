package com.innovcrest.utildroid.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

data class CatalogState(
    val catalog: Catalog? = null,
    val lastRefreshedMillis: Long = 0,
    val refreshing: Boolean = false,
    val error: String? = null,
)

/**
 * Fetches catalog.json, keeps the last good copy on disk so the store works offline,
 * and uses the ETag so an unchanged catalog costs one tiny request.
 */
class CatalogRepository(
    context: Context,
    private val http: OkHttpClient,
    private val json: Json,
    private val settings: SettingsStore,
) {
    private val cacheFile = File(context.filesDir, "catalog.json")
    private val etagFile = File(context.filesDir, "catalog.etag")
    private val mutex = Mutex()

    private val _state = MutableStateFlow(loadCache())
    val state: StateFlow<CatalogState> = _state.asStateFlow()

    private fun loadCache(): CatalogState {
        if (!cacheFile.exists()) return CatalogState()
        return runCatching { json.decodeFromString<Catalog>(cacheFile.readText()) }
            .map { CatalogState(catalog = it, lastRefreshedMillis = cacheFile.lastModified()) }
            .getOrDefault(CatalogState())
    }

    suspend fun refresh(): Result<Catalog> = mutex.withLock {
        withContext(Dispatchers.IO) {
            _state.update { it.copy(refreshing = true, error = null) }
            try {
                val url = settings.current().catalogUrl
                val request = Request.Builder().url(url).apply {
                    if (cacheFile.exists() && etagFile.exists()) header("If-None-Match", etagFile.readText())
                }.build()
                val catalog = http.newCall(request).execute().use { resp ->
                    when {
                        resp.code == 304 -> json.decodeFromString<Catalog>(cacheFile.readText())
                        resp.isSuccessful -> {
                            val body = resp.body?.string() ?: throw IOException("Empty catalog")
                            val parsed = json.decodeFromString<Catalog>(body)
                            if (parsed.schemaVersion > SUPPORTED_SCHEMA_VERSION) {
                                throw IOException("Catalog format v${parsed.schemaVersion} needs a newer DroidApps Store")
                            }
                            cacheFile.writeText(body)
                            resp.header("ETag")?.let { etagFile.writeText(it) } ?: etagFile.delete()
                            parsed
                        }
                        resp.code == 404 -> throw IOException("Catalog not found. Check the catalog URL in Settings.")
                        else -> throw IOException("Catalog request failed (HTTP ${resp.code})")
                    }
                }
                val now = System.currentTimeMillis()
                cacheFile.setLastModified(now)
                _state.value = CatalogState(catalog = catalog, lastRefreshedMillis = now)
                Result.success(catalog)
            } catch (e: Exception) {
                val message = when (e) {
                    is java.net.UnknownHostException -> "Offline, showing cached catalog"
                    else -> e.message ?: "Couldn't refresh the catalog"
                }
                _state.update { it.copy(refreshing = false, error = message) }
                Result.failure(e)
            }
        }
    }
}
