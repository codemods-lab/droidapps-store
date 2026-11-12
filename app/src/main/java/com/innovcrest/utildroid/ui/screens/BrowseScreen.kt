package com.innovcrest.utildroid.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.innovcrest.utildroid.ui.AppStatus
import com.innovcrest.utildroid.ui.AppUi
import com.innovcrest.utildroid.ui.StoreUi
import com.innovcrest.utildroid.ui.StoreViewModel
import com.innovcrest.utildroid.ui.common.AppRow
import com.innovcrest.utildroid.ui.common.EmptyState
import com.innovcrest.utildroid.ui.common.GroupGap
import com.innovcrest.utildroid.ui.common.ScreenTitle
import com.innovcrest.utildroid.ui.common.StoreBanner
import com.innovcrest.utildroid.ui.common.StoreGlyph
import com.innovcrest.utildroid.ui.common.groupedShape
import com.innovcrest.utildroid.ui.common.openInstallPermissionSettings
import com.innovcrest.utildroid.ui.common.relativeTime

private val BlockGap = 16.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(vm: StoreViewModel, onOpen: (AppUi) -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    val visible = ui.apps.filter { category == null || it.app.category == category }
    val context = LocalContext.current

    PullToRefreshBox(isRefreshing = ui.refreshing, onRefresh = vm::refresh, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item { BrowseHeader(subtitle = headerSubtitle(ui), onRefresh = vm::refresh) }

            if (!ui.canInstall) {
                item {
                    StoreBanner(
                        icon = Icons.Outlined.Shield,
                        title = "Allow installs from DroidApps",
                        subtitle = "Android needs this once before the store can install apps",
                        action = "Allow",
                        onAction = { openInstallPermissionSettings(context) },
                        modifier = Modifier.padding(bottom = BlockGap),
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
                        modifier = Modifier.padding(bottom = BlockGap),
                    )
                }
            }

            ui.error?.let { message -> item { ErrorStrip(message, Modifier.padding(bottom = BlockGap)) } }

            if (ui.categories.size > 1) {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = BlockGap),
                    ) {
                        item { CategoryChip("All", category == null) { category = null } }
                        items(ui.categories) { cat -> CategoryChip(cat, category == cat) { category = cat } }
                    }
                }
            }

            when {
                !ui.loaded && ui.refreshing -> item { EmptyState("Loading catalog…", null) }
                !ui.loaded -> item {
                    EmptyState(
                        "Couldn't load the catalog",
                        "Check your connection, or the catalog URL in Settings.",
                        action = "Try again",
                        onAction = vm::refresh,
                    )
                }
                visible.isEmpty() -> item {
                    EmptyState("No apps yet", "Add an app to apps.yaml in the droidapps-store repo and push.")
                }
            }

            itemsIndexed(visible, key = { _, item -> item.app.id }) { index, item ->
                AppRow(
                    item,
                    shape = groupedShape(index, visible.size),
                    onClick = { onOpen(item) },
                    onAction = { vm.primaryAction(item) },
                    modifier = Modifier.padding(top = if (index > 0) GroupGap else 0.dp),
                )
            }
        }
    }
}

private fun headerSubtitle(ui: StoreUi): String = when {
    ui.loaded -> {
        val n = ui.apps.size
        "$n app${if (n == 1) "" else "s"} · catalog updated ${relativeTime(ui.lastRefreshedMillis)}"
    }
    ui.refreshing -> "Loading catalog…"
    else -> "Catalog not loaded"
}

@Composable
private fun BrowseHeader(subtitle: String, onRefresh: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            StoreGlyph(40.dp, container = colors.primary, content = colors.onPrimary)
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
        ScreenTitle("DroidApps", subtitle = subtitle, modifier = Modifier.padding(top = 8.dp))
    }
}

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

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium) },
        leadingIcon = if (selected) {
            { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else null,
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = colors.onSurfaceVariant,
            selectedContainerColor = colors.secondaryContainer,
            selectedLabelColor = colors.onSecondaryContainer,
            selectedLeadingIconColor = colors.onSecondaryContainer,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = colors.outlineVariant,
        ),
        modifier = Modifier.height(36.dp),
    )
}
