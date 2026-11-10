package com.innovcrest.utildroid.data

import kotlinx.serialization.Serializable

/** Highest catalog.json format this build understands. */
const val SUPPORTED_SCHEMA_VERSION = 1

@Serializable
data class Catalog(
    val schemaVersion: Int = 1,
    val generatedAt: String = "",
    val store: StoreInfo = StoreInfo(),
    val apps: List<CatalogApp> = emptyList(),
)

@Serializable
data class StoreInfo(
    val name: String = "UtilDroid Store",
    /** SHA-256 of the release signing certificate, "AB:CD:..." form. Blank skips the check. */
    val signingCertSha256: String = "",
)

@Serializable
data class CatalogApp(
    val id: String,
    val name: String,
    val packageName: String,
    val summary: String = "",
    val description: String = "",
    val category: String = "Tools",
    val platforms: List<String> = listOf("phone"),
    val versionName: String = "",
    val versionCode: Long = 0,
    val apkUrl: String = "",
    val sizeBytes: Long = 0,
    val sha256: String = "",
    val minSdk: Int = 0,
    val changelog: String = "",
    val releasedAt: String = "",
    val iconUrl: String = "",
    val screenshots: List<String> = emptyList(),
    val repo: String = "",
) {
    val supportsTv: Boolean get() = "tv" in platforms
    val supportsPhone: Boolean get() = "phone" in platforms
    val hasRelease: Boolean get() = apkUrl.isNotBlank() && versionCode > 0
}
