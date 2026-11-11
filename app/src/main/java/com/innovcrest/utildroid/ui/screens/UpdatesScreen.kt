package com.innovcrest.utildroid.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.innovcrest.utildroid.ui.AppStatus
import com.innovcrest.utildroid.ui.AppUi
import com.innovcrest.utildroid.ui.StoreViewModel
import com.innovcrest.utildroid.ui.common.AppIcon
import com.innovcrest.utildroid.ui.common.AppRow
import com.innovcrest.utildroid.ui.common.GroupGap
import com.innovcrest.utildroid.ui.common.PillButton
import com.innovcrest.utildroid.ui.common.ScreenTitle
import com.innovcrest.utildroid.ui.common.SectionHeader
import com.innovcrest.utildroid.ui.common.StoreShapes
import com.innovcrest.utildroid.ui.common.formatSize
import com.innovcrest.utildroid.ui.common.groupedShape
import com.innovcrest.utildroid.ui.common.relativeTime
import com.innovcrest.utildroid.ui.theme.Type

@Composable
fun UpdatesScreen(vm: StoreViewModel, onOpen: (AppUi) -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val updates = ui.updates
    val pending = ui.pendingUpdateCount
    val pendingSize = updates.filter { it.status is AppStatus.UpdateAvailable }.sumOf { it.app.sizeBytes }
    val recent = ui.all
        .filter { it.installed != null && it.status is AppStatus.Installed }
        .sortedByDescending { it.installed?.lastUpdateTime ?: 0 }
        .take(5)
    val schedule = if (settings.checkIntervalHours > 0) "checks every ${settings.checkIntervalHours} h" else "background checks off"

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            ScreenTitle(
                "Updates",
                subtitle = "Checked ${relativeTime(ui.lastRefreshedMillis)} · $schedule",
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 40.dp, bottom = 20.dp),
            )
        }

        item {
            if (pending > 0) {
                SummaryCard(
                    title = "$pending update${if (pending > 1) "s" else ""}",
                    subtitle = formatSize(pendingSize) + if (settings.wifiOnly) " · auto-updates on Wi-Fi only" else "",
                ) {
                    PillButton(
                        "Update all",
                        onClick = vm::updateAll,
                        height = 56.dp,
                        icon = Icons.Outlined.FileDownload,
                        fontSize = 16.sp,
                    )
                }
            } else {
                SummaryCard(
                    title = if (updates.isEmpty()) "Up to date" else "Updating…",
                    subtitle = if (updates.isEmpty()) "Every installed app is on its latest version" else "Finishing the updates in progress",
                ) {
                    PillButton("Check now", onClick = vm::refresh, height = 48.dp, enabled = !ui.refreshing)
                }
            }
        }

        if (updates.isNotEmpty()) {
            item { SectionHeader("Pending", Modifier.padding(top = 24.dp, bottom = 12.dp)) }
            itemsIndexed(updates, key = { _, item -> "u-" + item.app.id }) { index, item ->
                AppRow(
                    item,
                    shape = groupedShape(index, updates.size),
                    onClick = { onOpen(item) },
                    onAction = { vm.primaryAction(item) },
                    compact = true,
                    modifier = Modifier.padding(top = if (index > 0) GroupGap else 0.dp),
                )
            }
        }

        if (recent.isNotEmpty()) {
            item { SectionHeader("Recently updated", Modifier.padding(top = 24.dp, bottom = 12.dp)) }
            itemsIndexed(recent, key = { _, item -> "r-" + item.app.id }) { index, item ->
                RecentRow(
                    item,
                    shape = groupedShape(index, recent.size),
                    onClick = { onOpen(item) },
                    modifier = Modifier.padding(top = if (index > 0) GroupGap else 0.dp),
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(title: String, subtitle: String, action: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = StoreShapes.Card,
        color = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(20.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontSize = 26.sp, lineHeight = 32.sp, fontWeight = Type.Display)
                Text(subtitle, fontSize = 14.sp, lineHeight = 20.sp, color = colors.onPrimaryContainer.copy(alpha = 0.78f))
            }
            action()
        }
    }
}

@Composable
private fun RecentRow(item: AppUi, shape: Shape, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val version = item.installed?.versionName.orEmpty()
    val updated = relativeTime(item.installed?.lastUpdateTime ?: 0)
    Surface(onClick = onClick, shape = shape, color = colors.surface, modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            AppIcon(item.app, 40.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.app.name, fontSize = 15.sp, fontWeight = Type.Emphasis, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (version.isBlank()) "Updated $updated" else "Updated to $version · $updated",
                    fontSize = 13.sp,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
