package com.innovcrest.utildroid.ui

import com.innovcrest.utildroid.data.CatalogApp
import com.innovcrest.utildroid.data.InstalledInfo
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreListingTest {
    private val now: Instant = Instant.parse("2026-11-12T00:00:00Z")

    private fun app(
        id: String,
        name: String = id,
        category: String = "Tools",
        summary: String = "",
        releasedAt: String = "2026-11-01T00:00:00Z",
        released: Boolean = true,
        status: AppStatus = AppStatus.NotInstalled,
        installed: Boolean = false,
    ) = AppUi(
        app = CatalogApp(
            id = id,
            name = name,
            packageName = "com.example.$id",
            summary = summary,
            category = category,
            versionCode = if (released) 1 else 0,
            apkUrl = if (released) "https://example.com/$id.apk" else "",
            releasedAt = if (released) releasedAt else "",
        ),
        status = status,
        installed = if (installed) InstalledInfo(1, "1.0.0", 0) else null,
        isSelf = false,
    )

    @Test
    fun `an app released within 30 days and not installed is new`() {
        assertTrue(app("a", releasedAt = "2026-10-14T00:00:00Z").isNew(now))
    }

    @Test
    fun `an app released 30 or more days ago is not new`() {
        assertFalse(app("a", releasedAt = "2026-10-13T00:00:00Z").isNew(now))
    }

    @Test
    fun `installed, unreleased and undated apps are never new`() {
        assertFalse(app("a", installed = true).isNew(now))
        assertFalse(app("a", released = false).isNew(now))
        assertFalse(app("a", releasedAt = "not a date").isNew(now))
    }

    @Test
    fun `search matches name, summary and category, ignoring case and outer spaces`() {
        val ludo = app("ludo", name = "Ludo", category = "Games", summary = "Board game for four")
        assertTrue(ludo.matches(" lud "))
        assertTrue(ludo.matches("BOARD"))
        assertTrue(ludo.matches("games"))
        assertFalse(ludo.matches("widgets"))
    }

    @Test
    fun `a blank search matches nothing`() {
        assertFalse(app("a").matches("   "))
    }

    @Test
    fun `category filter keeps everything for null and only that category otherwise`() {
        val apps = listOf(app("a"), app("b", category = "Games"), app("c"))
        assertEquals(apps, apps.inCategory(null))
        assertEquals(listOf("b"), apps.inCategory("Games").ids())
    }

    @Test
    fun `sort orders`() {
        val apps = listOf(
            app("b", name = "beta", releasedAt = "2026-11-01T00:00:00Z"),
            app("c", name = "Charlie", releasedAt = "2026-11-10T00:00:00Z"),
            app("a", name = "Alpha", releasedAt = "2026-10-01T00:00:00Z"),
        )
        assertEquals(listOf("b", "c", "a"), apps.sortedBy(SortOrder.Featured).ids())
        assertEquals(listOf("c", "b", "a"), apps.sortedBy(SortOrder.Recent).ids())
        assertEquals(listOf("a", "b", "c"), apps.sortedBy(SortOrder.Name).ids())
    }

    @Test
    fun `home sections split apps, games, updates and new apps`() {
        val ui = StoreUi(
            apps = listOf(
                app("tool", installed = true, status = AppStatus.UpdateAvailable("1.0.0")),
                app("old", releasedAt = "2026-01-01T00:00:00Z"),
                app("fresh", releasedAt = "2026-11-05T00:00:00Z"),
                app("game", category = "Games", releasedAt = "2026-11-10T00:00:00Z"),
            ),
        )
        val home = ui.homeSections(now)
        assertEquals(listOf("tool"), home.pendingUpdates.ids())
        assertEquals(listOf("game", "fresh"), home.newApps.ids())
        assertEquals(listOf("tool", "old", "fresh"), home.apps.ids())
        assertEquals(listOf("game"), home.games.ids())
    }

    @Test
    fun `home shows at most four new apps`() {
        val ui = StoreUi(apps = (1..6).map { app("app$it") })
        assertEquals(4, ui.homeSections(now).newApps.size)
    }

    @Test
    fun `categories keep catalog order and flag ones with nothing released`() {
        val ui = StoreUi(
            apps = listOf(
                app("a", category = "Tools"),
                app("g", category = "Games", released = false, status = AppStatus.Unavailable),
                app("b", category = "Tools", released = false, status = AppStatus.Unavailable),
            ),
        )
        assertEquals(
            listOf(
                CategorySummary("Tools", appCount = 2, comingSoon = false),
                CategorySummary("Games", appCount = 1, comingSoon = true),
            ),
            ui.homeSections(now).categories,
        )
    }

    private fun List<AppUi>.ids() = map { it.app.id }
}
