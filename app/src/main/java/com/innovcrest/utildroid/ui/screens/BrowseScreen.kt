package com.innovcrest.utildroid.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.innovcrest.utildroid.ui.AppStatus
import com.innovcrest.utildroid.ui.AppUi
import com.innovcrest.utildroid.ui.StoreViewModel
import com.innovcrest.utildroid.ui.common.AppIcon
import com.innovcrest.utildroid.ui.common.DownloadBar
import com.innovcrest.utildroid.ui.common.PrimaryActionButton
import com.innovcrest.utildroid.ui.common.StoreCard
import com.innovcrest.utildroid.ui.common.isFailure
import com.innovcrest.utildroid.ui.common.openInstallPermissionSettings
import com.innovcrest.utildroid.ui.common.relativeTime
import com.innovcrest.utildroid.ui.common.statusLine
import com.innovcrest.utildroid.ui.theme.Brand

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    vm: StoreViewModel,
    onOpen: (AppUi) -> Unit,
    onShowUpdates: () -> Unit,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    val visible = ui.apps.filter { category == null || it.app.category == category }
    val context = LocalContext.current

    PullToRefreshBox(isRefreshing = ui.refreshing, onRefresh = vm::refresh, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item { BrowseHeader(onRefresh = vm::refresh) }

            if (!ui.canInstall) {
                item {
                    Banner(
                        title = "Allow installs from UtilDroid",
                        subtitle = "Android needs this once before the store can install apps",
                        action = "Allow",
                        onAction = { openInstallPermissionSettings(context) },
                    )
                }
            }

            ui.self?.takeIf { it.status is AppStatus.UpdateAvailable || it.status is AppStatus.Busy }?.let { self ->
                item {
                    Banner(
                        title = "Store update ready",
                        subtitle = "UtilDroid Store ${self.app.versionName}",
                        action = if (self.status is AppStatus.Busy) "Updating…" else "Update",
                        onAction = { vm.primaryAction(self) },
                    )
                }
            }

            ui.error?.let { message -> item { InfoStrip(message) } }

            if (ui.loaded) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "${ui.apps.size} apps · catalog updated ${relativeTime(ui.lastRefreshedMillis)}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        if (ui.pendingUpdateCount > 0) {
                            TextButton(onClick = onShowUpdates) {
                                Text(
                                    "${ui.pendingUpdateCount} update${if (ui.pendingUpdateCount > 1) "s" else ""}",
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }

            if (ui.categories.size > 1) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    EmptyState("No apps yet", "Add an app to apps.yaml in the utildroid-store repo and push.")
                }
            }

            items(visible, key = { it.app.id }) { item ->
                AppCard(item, onClick = { onOpen(item) }, onAction = { vm.primaryAction(item) })
            }
        }
    }
}

@Composable
private fun BrowseHeader(onRefresh: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
    ) {
        Box(
            Modifier.size(36.dp).background(Brand.Accent, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("U", color = Brand.OnAccent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Spacer(Modifier.size(10.dp))
        Text("UtilDroid", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        OutlinedIconButton(
            onClick = onRefresh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.size(44.dp),
        ) {
            Icon(Icons.Outlined.Refresh, contentDescription = "Refresh catalog")
        }
    }
}

@Composable
fun AppCard(item: AppUi, onClick: () -> Unit, onAction: () -> Unit) {
    StoreCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                AppIcon(item.app, 56.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(item.app.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (item.app.summary.isNotBlank()) {
                        Text(
                            item.app.summary,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        statusLine(item),
                        fontSize = 11.sp,
                        color = if (isFailure(item)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                PrimaryActionButton(item, onClick = onAction)
            }
            DownloadBar(item)
        }
    }
}

@Composable
fun Banner(title: String, subtitle: String, action: String, onAction: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        border = BorderStroke(1.dp, Brand.Accent.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = Brand.Accent, contentColor = Brand.OnAccent),
                modifier = Modifier.height(44.dp),
            ) { Text(action, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun InfoStrip(message: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(message, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) },
        shape = RoundedCornerShape(18.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.onSurface,
            selectedLabelColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

@Composable
fun EmptyState(title: String, body: String?, action: String? = null, onAction: () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 16.dp),
    ) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        if (body != null) Text(body, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (action != null) TextButton(onClick = onAction) { Text(action) }
    }
}
