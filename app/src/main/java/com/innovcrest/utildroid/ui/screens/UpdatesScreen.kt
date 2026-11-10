package com.innovcrest.utildroid.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.innovcrest.utildroid.ui.AppStatus
import com.innovcrest.utildroid.ui.AppUi
import com.innovcrest.utildroid.ui.StoreViewModel
import com.innovcrest.utildroid.ui.common.AppIcon
import com.innovcrest.utildroid.ui.common.SectionLabel
import com.innovcrest.utildroid.ui.common.StoreCard
import com.innovcrest.utildroid.ui.common.formatSize
import com.innovcrest.utildroid.ui.common.relativeTime

@Composable
fun UpdatesScreen(vm: StoreViewModel, onOpen: (AppUi) -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val updates = ui.updates
    val pendingSize = updates.filter { it.status is AppStatus.UpdateAvailable }.sumOf { it.app.sizeBytes }
    val recent = ui.all
        .filter { it.installed != null && it.status is AppStatus.Installed }
        .sortedByDescending { it.installed?.lastUpdateTime ?: 0 }
        .take(5)

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text("Updates", fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        }

        item {
            if (ui.pendingUpdateCount == 0) {
                Banner(
                    title = if (updates.isEmpty()) "Everything is up to date" else "Updating…",
                    subtitle = "Checked ${relativeTime(ui.lastRefreshedMillis)}",
                    action = "Check now",
                    onAction = vm::refresh,
                )
            } else {
                val n = ui.pendingUpdateCount
                Banner(
                    title = "$n update${if (n > 1) "s" else ""} available",
                    subtitle = "${formatSize(pendingSize)} total",
                    action = "Update all",
                    onAction = vm::updateAll,
                )
            }
        }

        if (updates.isNotEmpty()) {
            item { SectionLabel("Pending", Modifier.padding(top = 8.dp)) }
            items(updates, key = { "u-" + it.app.id }) { item ->
                AppCard(item, onClick = { onOpen(item) }, onAction = { vm.primaryAction(item) })
            }
        }

        if (recent.isNotEmpty()) {
            item { SectionLabel("Installed", Modifier.padding(top = 8.dp)) }
            items(recent, key = { "r-" + it.app.id }) { item ->
                StoreCard(onClick = { onOpen(item) }, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        AppIcon(item.app, 40.dp)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(item.app.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${item.installed?.versionName.orEmpty()} · updated ${relativeTime(item.installed?.lastUpdateTime ?: 0)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
