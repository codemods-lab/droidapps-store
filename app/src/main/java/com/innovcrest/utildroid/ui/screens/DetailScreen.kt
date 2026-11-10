package com.innovcrest.utildroid.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.innovcrest.utildroid.installer.InstallPhase
import com.innovcrest.utildroid.ui.AppStatus
import com.innovcrest.utildroid.ui.AppUi
import com.innovcrest.utildroid.ui.StoreViewModel
import com.innovcrest.utildroid.ui.common.AppIcon
import com.innovcrest.utildroid.ui.common.PrimaryActionButton
import com.innovcrest.utildroid.ui.common.androidVersionName
import com.innovcrest.utildroid.ui.common.formatSize

@Composable
fun DetailScreen(vm: StoreViewModel, appId: String, onBack: () -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val item = ui.all.firstOrNull { it.app.id == appId }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        IconButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp, top = 8.dp).size(44.dp)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
        }
        if (item == null) {
            EmptyState("App not found", "It may have been removed from the catalog.")
            return@Column
        }
        DetailBody(item, vm)
    }
}

@Composable
private fun DetailBody(item: AppUi, vm: StoreViewModel) {
    val app = item.app
    val uri = LocalUriHandler.current
    val versionLine = when (val s = item.status) {
        is AppStatus.UpdateAvailable -> "${s.installedVersionName} → ${app.versionName}"
        else -> app.versionName
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(app, 80.dp)
            Column(Modifier.padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(app.name, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(
                    listOf(app.category, versionLine, formatSize(app.sizeBytes)).filter { it.isNotBlank() }.joinToString(" · "),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryActionButton(item, onClick = { vm.primaryAction(item) }, large = true, modifier = Modifier.fillMaxWidth())
            Text(
                hint(item),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = if (item.status is AppStatus.Busy && (item.status.phase is InstallPhase.Failed)) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (app.screenshots.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(app.screenshots) { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = "Screenshot of ${app.name}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(132.dp)
                            .height(235.dp)
                            .clip(RoundedCornerShape(14.dp)),
                    )
                }
            }
        }

        if (app.changelog.isNotBlank()) {
            Section("What's new in ${app.versionName}", app.changelog)
        }
        if (app.description.isNotBlank()) {
            Section("About", app.description)
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column {
                InfoRow("Package", app.packageName)
                InfoRow("Requires", androidVersionName(app.minSdk))
                if (app.releasedAt.isNotBlank()) InfoRow("Released", app.releasedAt.take(10))
                item.installed?.let { InfoRow("Installed", it.versionName.ifBlank { "Yes" }) }
                if (app.repo.isNotBlank()) {
                    InfoRow("Source", app.repo, last = true, onClick = { uri.openUri("https://github.com/${app.repo}") })
                }
            }
        }

        if (item.installed != null && !item.isSelf) {
            OutlinedButton(
                onClick = { vm.uninstall(item) },
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth().height(44.dp),
            ) {
                Text("Uninstall", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun hint(item: AppUi): String = when (val s = item.status) {
    is AppStatus.Busy -> when (val p = s.phase) {
        is InstallPhase.Downloading -> "Checks SHA-256 and signature, then installs"
        InstallPhase.Verifying -> "Checking SHA-256 and signing certificate"
        InstallPhase.Installing -> "Installing"
        InstallPhase.WaitingForUser -> "Confirm the install on the Android prompt"
        is InstallPhase.Failed -> p.message
    }
    is AppStatus.Installed -> "Up to date"
    AppStatus.Unavailable -> "No GitHub release published yet"
    else -> "Checked with SHA-256 and signature before it installs"
}

@Composable
private fun Section(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Text(body, fontSize = 13.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InfoRow(label: String, value: String, last: Boolean = false, onClick: (() -> Unit)? = null) {
    Box {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (onClick != null) {
                Text(
                    value,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 16.dp).clickable(onClick = onClick),
                )
            } else {
                Text(value, fontSize = 13.sp, textAlign = TextAlign.End, modifier = Modifier.padding(start = 16.dp))
            }
        }
    }
    if (!last) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}
