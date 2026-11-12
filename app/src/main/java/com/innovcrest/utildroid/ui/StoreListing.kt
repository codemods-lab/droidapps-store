package com.innovcrest.utildroid.ui

import java.time.Duration
import java.time.Instant

// Pure listing rules for the store screens: what counts as new, how search and sort work,
// and which sections the home screen shows. No Android or Compose types, so it is unit-tested.

const val GAMES_CATEGORY = "Games"
private const val NEW_WINDOW_DAYS = 30L
private const val MAX_NEW_APPS = 4

/** "New in the store": not on this phone yet and released within the last 30 days. */
fun AppUi.isNew(now: Instant = Instant.now()): Boolean {
    if (installed != null || !app.hasRelease) return false
    val released = runCatching { Instant.parse(app.releasedAt) }.getOrNull() ?: return false
    return Duration.between(released, now).toDays() < NEW_WINDOW_DAYS
}

val AppUi.isGame: Boolean get() = app.category.equals(GAMES_CATEGORY, ignoreCase = true)

/** Case-insensitive match on name, summary or category. A blank query matches nothing. */
fun AppUi.matches(query: String): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return false
    return app.name.contains(q, ignoreCase = true) ||
        app.summary.contains(q, ignoreCase = true) ||
        app.category.contains(q, ignoreCase = true)
}

enum class SortOrder(val label: String) {
    /** The order of catalog/apps.yaml. */
    Featured("Featured"),
    Recent("Recently updated"),
    Name("Name (A–Z)"),
}

/** [category] null means every category. */
fun List<AppUi>.inCategory(category: String?): List<AppUi> =
    if (category == null) this else filter { it.app.category == category }

fun List<AppUi>.sortedBy(order: SortOrder): List<AppUi> = when (order) {
    SortOrder.Featured -> this
    SortOrder.Recent -> sortedByDescending { it.app.releasedAt }
    SortOrder.Name -> sortedBy { it.app.name.lowercase() }
}

data class CategorySummary(
    val name: String,
    val appCount: Int,
    /** Every app in the category is still waiting for its first release. */
    val comingSoon: Boolean,
)

/** The home screen's sections, in catalog order unless stated otherwise. */
data class HomeSections(
    val pendingUpdates: List<AppUi>,
    /** Newest release first. */
    val newApps: List<AppUi>,
    val apps: List<AppUi>,
    val games: List<AppUi>,
    val categories: List<CategorySummary>,
)

fun StoreUi.homeSections(now: Instant = Instant.now()): HomeSections = HomeSections(
    pendingUpdates = apps.filter { it.status is AppStatus.UpdateAvailable },
    newApps = apps.filter { it.isNew(now) }.sortedByDescending { it.app.releasedAt }.take(MAX_NEW_APPS),
    apps = apps.filter { !it.isGame },
    games = apps.filter { it.isGame },
    categories = apps.groupBy { it.app.category }.map { (name, items) ->
        CategorySummary(name, items.size, comingSoon = items.all { it.status is AppStatus.Unavailable })
    },
)
