package com.innovcrest.utildroid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.innovcrest.utildroid.ui.AppStatus
import com.innovcrest.utildroid.ui.AppUi
import com.innovcrest.utildroid.ui.CategorySummary
import com.innovcrest.utildroid.ui.homeSections
import com.innovcrest.utildroid.ui.matches
import com.innovcrest.utildroid.ui.StoreViewModel
import com.innovcrest.utildroid.ui.common.AppIcon
import com.innovcrest.utildroid.ui.common.AppSheetHost
import com.innovcrest.utildroid.ui.common.AppTile
import com.innovcrest.utildroid.ui.common.EmptyState
import com.innovcrest.utildroid.ui.common.FixedGrid
import com.innovcrest.utildroid.ui.common.PillButton
import com.innovcrest.utildroid.ui.common.PillStyle
import com.innovcrest.utildroid.ui.common.StoreBanner
import com.innovcrest.utildroid.ui.common.StoreGlyph
import com.innovcrest.utildroid.ui.common.StoreShapes
import com.innovcrest.utildroid.ui.common.categoryStyle
import com.innovcrest.utildroid.ui.common.openInstallPermissionSettings
import com.innovcrest.utildroid.ui.theme.Type

private val SectionGap = 24.dp
private const val HOME_APP_TILES = 6

/** Home: search, what needs attention, what's new, then apps, games and categories as tiles. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    vm: StoreViewModel,
    onDetails: (AppUi) -> Unit,
    onSeeAll: (category: String?) -> Unit,
    onOpenUpdates: () -> Unit,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var sheetAppId by rememberSaveable { mutableStateOf<String?>(null) }
    val openSheet: (AppUi) -> Unit = { sheetAppId = it.app.id }

    val searching = query.isNotBlank()
    val matches = ui.apps.filter { it.matches(query) }
    val home = ui.homeSections()

    PullToRefreshBox(isRefreshing = ui.refreshing, onRefresh = vm::refresh, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(SectionGap),
            modifier = Modifier.fillMaxSize(),
        ) {
            item { HomeHeader(query, onQueryChange = { query = it }, onRefresh = vm::refresh) }

            if (!ui.canInstall) {
                item {
                    StoreBanner(
                        icon = Icons.Outlined.Shield,
                        title = "Allow installs from DroidApps",
                        subtitle = "Android needs this once before the store can install apps",
                        action = "Allow",
                        onAction = { openInstallPermissionSettings(context) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            ui.self?.takeIf { it.status is AppStatus.UpdateAvailable || it.status is AppStatus.Busy }?.let { self ->
                val busy = self.status is AppStatus.Busy
                item {
                    StoreBanner(
                        icon = Icons.Outlined.FileDownload,
                        title = if (busy) "Updating the store" else "Store update ready",
                        subtitle = "DroidApps Store ${self.app.versionName}",
                        action = if (busy) "Updating…" else "Update",
                        onAction = { vm.primaryAction(self) },
                        actionEnabled = !busy,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            ui.error?.let { message -> item { ErrorStrip(message, Modifier.padding(horizontal = 16.dp)) } }

            when {
                !ui.loaded && ui.refreshing -> item { EmptyState("Loading catalog…", null) }
                !ui.loaded -> item {
                    EmptyState(
                        "Couldn't load the catalog",
                        "Check your connection and try again.",
                        action = "Try again",
                        onAction = vm::refresh,
                    )
                }
                ui.apps.isEmpty() -> item {
                    EmptyState("No apps yet", "Add an app to apps.yaml in the droidapps-store repo and push.")
                }
                searching && matches.isEmpty() -> item {
                    EmptyState("No matches", "Nothing in the store matches \"${query.trim()}\".")
                }
                searching -> item {
                    Section("Results") {
                        FixedGrid(matches, columns = 3) { item, cell -> AppTile(item, onClick = { openSheet(item) }, modifier = cell) }
                    }
                }
                else -> {
                    if (home.pendingUpdates.isNotEmpty()) {
                        item { UpdatesBanner(home.pendingUpdates, onUpdateAll = vm::updateAll, onClick = onOpenUpdates) }
                    }
                    if (home.newApps.isNotEmpty()) {
                        item { NewInStore(home.newApps, onClick = openSheet) }
                    }
                    if (home.apps.isNotEmpty()) {
                        item {
                            Section("Apps", action = "See all", onAction = { onSeeAll(null) }) {
                                FixedGrid(home.apps.take(HOME_APP_TILES), columns = 3) { item, cell ->
                                    AppTile(item, onClick = { openSheet(item) }, modifier = cell)
                                }
                            }
                        }
                    }
                    if (home.games.isNotEmpty()) {
                        item {
                            Section("Games") {
                                FixedGrid(home.games.take(HOME_APP_TILES), columns = 3) { item, cell ->
                                    AppTile(item, onClick = { openSheet(item) }, modifier = cell)
                                }
                            }
                        }
                    }
                    item { Categories(home.categories, totalApps = ui.apps.size, onSeeAll = onSeeAll) }
                }
            }
        }
    }

    AppSheetHost(sheetAppId, ui, vm, onDetails = onDetails, onDismiss = { sheetAppId = null })
}

@Composable
private fun HomeHeader(query: String, onQueryChange: (String) -> Unit, onRefresh: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val focus = LocalFocusManager.current
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            StoreGlyph(40.dp, container = colors.primary, content = colors.onPrimary)
            Text(
                "DroidApps",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.4).sp,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Surface(
                onClick = onRefresh,
                shape = CircleShape,
                color = colors.secondaryContainer,
                contentColor = colors.onSecondaryContainer,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Refresh catalog", modifier = Modifier.size(22.dp))
                }
            }
        }

        Surface(
            shape = CircleShape,
            color = colors.surfaceContainerHigh,
            contentColor = colors.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(start = 20.dp, end = 8.dp),
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(22.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text("Search apps and games", fontSize = 16.sp, color = colors.onSurfaceVariant, maxLines = 1)
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(color = colors.onSurface, fontSize = 16.sp),
                        cursorBrush = SolidColor(colors.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Search apps and games" },
                    )
                }
                if (query.isNotEmpty()) {
                    Surface(
                        onClick = {
                            onQueryChange("")
                            focus.clearFocus()
                        },
                        shape = CircleShape,
                        color = Color.Transparent,
                        contentColor = colors.onSurfaceVariant,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear search", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

/** Section title with an optional text action, then the content; all inset 16dp. */
@Composable
private fun Section(
    title: String,
    action: String? = null,
    onAction: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
        ) {
            Text(title, fontSize = 20.sp, fontWeight = Type.Display, modifier = Modifier.weight(1f))
            if (action != null) {
                Surface(
                    onClick = onAction,
                    shape = CircleShape,
                    color = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.height(40.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(start = 12.dp, end = 8.dp),
                    ) {
                        Text(action, fontSize = 14.sp, fontWeight = Type.Emphasis)
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        content()
    }
}

@Composable
private fun UpdatesBanner(pending: List<AppUi>, onUpdateAll: () -> Unit, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val n = pending.size
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = colors.tertiaryContainer,
        contentColor = colors.onTertiaryContainer,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(start = 18.dp, top = 14.dp, end = 14.dp, bottom = 14.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("$n update${if (n == 1) "" else "s"} ready", fontSize = 16.sp, fontWeight = Type.Emphasis)
                Text(
                    pending.joinToString { it.app.name },
                    fontSize = 13.sp,
                    color = colors.onTertiaryContainer.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            PillButton("Update all", onUpdateAll, style = PillStyle.Tertiary)
        }
    }
}

/** Card ground, decoration, title ink, badge, and summary ink. */
private data class NewCardColors(val ground: Color, val decoration: Color, val ink: Color, val badge: Color, val soft: Color)

private val newCardPalette = listOf(
    NewCardColors(Color(0xFFFFDCC2), Color(0xFFFFB68A), Color(0xFF2E1500), Color(0xFF6E3900), Color(0xFF5A2D00)),
    NewCardColors(Color(0xFFFFEFC2), Color(0xFFFFDF8E), Color(0xFF261A00), Color(0xFF5C4300), Color(0xFF4A3600)),
    NewCardColors(Color(0xFFD6E3FF), Color(0xFFADC7FF), Color(0xFF001B3E), Color(0xFF284777), Color(0xFF1F3A66)),
    NewCardColors(Color(0xFFEADDFF), Color(0xFFD0BCFF), Color(0xFF21005D), Color(0xFF4F378B), Color(0xFF3B2A6B)),
)

@Composable
private fun NewInStore(items: List<AppUi>, onClick: (AppUi) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("New in the store", fontSize = 20.sp, fontWeight = Type.Display, modifier = Modifier.padding(horizontal = 20.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(items, key = { _, item -> item.app.id }) { index, item ->
                val width = if (items.size == 1) Modifier.fillParentMaxWidth() else Modifier.width(290.dp)
                NewCard(item, newCardPalette[index % newCardPalette.size], onClick = { onClick(item) }, modifier = width)
            }
        }
    }
}

@Composable
private fun NewCard(item: AppUi, palette: NewCardColors, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(32.dp),
        color = palette.ground,
        contentColor = palette.ink,
        modifier = modifier.height(132.dp),
    ) {
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(32.dp))) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 26.dp, y = (-40).dp)
                    .size(112.dp)
                    .background(palette.decoration, StoreShapes.Cookie9),
            )
            AppIcon(item.app, 40.dp, Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 16.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            ) {
                Surface(shape = RoundedCornerShape(12.dp), color = palette.badge, contentColor = Color.White, modifier = Modifier.height(24.dp)) {
                    Box(Modifier.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                        Text("New", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    item.app.name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.3).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.app.summary.isNotBlank()) {
                    Text(
                        item.app.summary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = palette.soft,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private data class CategoryTileData(
    val label: String,
    val count: String,
    val container: Color,
    val content: Color,
    val icon: ImageVector,
    val category: String?,
)

@Composable
private fun Categories(categories: List<CategorySummary>, totalApps: Int, onSeeAll: (String?) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tiles = categories.map { category ->
        val style = categoryStyle(category.name)
        val count = if (category.comingSoon) "Coming soon" else countLabel(category.appCount)
        CategoryTileData(category.name, count, style.container, style.content, style.icon, category.name)
    } + CategoryTileData("All apps", countLabel(totalApps), colors.primary, colors.onPrimary, Icons.Rounded.GridView, null)

    Section("Categories") {
        FixedGrid(tiles, columns = 3) { tile, cell ->
            Surface(
                onClick = { onSeeAll(tile.category) },
                shape = RoundedCornerShape(24.dp),
                color = tile.container,
                contentColor = tile.content,
                modifier = cell.height(104.dp),
            ) {
                Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize().padding(14.dp)) {
                    Icon(tile.icon, contentDescription = null, modifier = Modifier.size(24.dp))
                    Column {
                        Text(tile.label, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(tile.count, fontSize = 12.sp, color = tile.content.copy(alpha = 0.8f), maxLines = 1)
                    }
                }
            }
        }
    }
}

private fun countLabel(n: Int): String = "$n app${if (n == 1) "" else "s"}"

@Composable
private fun ErrorStrip(message: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(message, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
    }
}
