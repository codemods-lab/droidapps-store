package com.innovcrest.utildroid.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.innovcrest.utildroid.data.CatalogApp
import com.innovcrest.utildroid.installer.InstallPhase
import com.innovcrest.utildroid.ui.AppStatus
import com.innovcrest.utildroid.ui.AppUi
import com.innovcrest.utildroid.ui.StoreViewModel
import com.innovcrest.utildroid.ui.common.AppIcon
import com.innovcrest.utildroid.ui.common.EmptyState
import com.innovcrest.utildroid.ui.common.GroupGap
import com.innovcrest.utildroid.ui.common.PillButton
import com.innovcrest.utildroid.ui.common.PillStyle
import com.innovcrest.utildroid.ui.common.StoreShapes
import com.innovcrest.utildroid.ui.common.WavyProgress
import com.innovcrest.utildroid.ui.common.androidVersionName
import com.innovcrest.utildroid.ui.common.formatSize
import com.innovcrest.utildroid.ui.common.groupedShape
import com.innovcrest.utildroid.ui.theme.Type

@Composable
fun DetailScreen(vm: StoreViewModel, appId: String, onBack: () -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val item = ui.all.firstOrNull { it.app.id == appId }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(Modifier.height(64.dp).padding(horizontal = 8.dp), contentAlignment = Alignment.CenterStart) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to browse")
            }
        }
        if (item == null) {
            EmptyState("App not found", "It may have been removed from the catalog.")
            return@Column
        }
        DetailBody(item, vm)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailBody(item: AppUi, vm: StoreViewModel) {
    val app = item.app
    val colors = MaterialTheme.colorScheme
    val versionChip = when (val s = item.status) {
        is AppStatus.UpdateAvailable -> "${s.installedVersionName} → ${app.versionName}"
        else -> app.versionName
    }
    val chips = listOf(app.category, versionChip, formatSize(app.sizeBytes)).filter { it.isNotBlank() && it != "—" }

    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(horizontal = 4.dp)) {
            AppIcon(app, 96.dp)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(app.name, fontSize = 36.sp, lineHeight = 44.sp, fontWeight = Type.Display, letterSpacing = (-0.4).sp)
                if (app.summary.isNotBlank()) {
                    Text(app.summary, fontSize = 15.sp, lineHeight = 22.sp, color = colors.onSurfaceVariant)
                }
            }
            if (chips.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    chips.forEach { InfoChip(it) }
                }
            }
        }

        ActionCard(item, vm)

        if (app.screenshots.isNotEmpty()) Screenshots(app)

        if (app.changelog.isNotBlank()) TextCard("What's new in ${app.versionName}", app.changelog)
        if (app.description.isNotBlank()) TextCard("About", app.description)

        InfoGroup(item)

        if (item.installed != null && !item.isSelf) {
            Surface(
                onClick = { vm.uninstall(item) },
                shape = CircleShape,
                color = colors.background,
                contentColor = colors.error,
                border = BorderStroke(1.dp, colors.error.copy(alpha = 0.32f)),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Uninstall", fontSize = 15.sp, fontWeight = Type.Emphasis)
                }
            }
        }
    }
}

@Composable
private fun InfoChip(label: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.height(32.dp),
    ) {
        Box(Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

private data class CardButton(
    val label: String,
    val icon: ImageVector,
    val style: PillStyle,
    val onClick: () -> Unit,
)

/** State of this app plus every action it allows, as one connected button group. */
@Composable
private fun ActionCard(item: AppUi, vm: StoreViewModel) {
    val app = item.app
    val colors = MaterialTheme.colorScheme
    val status = item.status
    val phase = (status as? AppStatus.Busy)?.phase
    val downloading = phase as? InstallPhase.Downloading
    val isInstalled = item.installed != null

    val title = when (status) {
        AppStatus.NotInstalled -> "Ready to install"
        is AppStatus.UpdateAvailable -> "Update available"
        is AppStatus.Installed -> "Up to date"
        AppStatus.Unavailable -> "No release yet"
        is AppStatus.Busy -> when (status.phase) {
            is InstallPhase.Downloading -> if (isInstalled) "Downloading update" else "Downloading"
            InstallPhase.Verifying -> "Verifying"
            InstallPhase.Installing -> "Installing"
            InstallPhase.WaitingForUser -> "Waiting for you"
            is InstallPhase.Failed -> "Couldn't install"
        }
    }

    val caption = when (status) {
        is AppStatus.Busy -> when (val p = status.phase) {
            is InstallPhase.Downloading -> {
                val done = formatSize((app.sizeBytes * p.progress.coerceIn(0f, 1f)).toLong())
                "$done of ${formatSize(app.sizeBytes)} · then checks SHA-256 and signature, and installs"
            }
            InstallPhase.Verifying -> "Checking SHA-256 and signing certificate"
            InstallPhase.Installing -> "Handing the verified APK to Android"
            InstallPhase.WaitingForUser -> "Confirm the install on the Android prompt"
            is InstallPhase.Failed -> p.message
        }
        is AppStatus.Installed -> "Version ${status.versionName} is the latest in the catalog"
        AppStatus.Unavailable -> "No GitHub release published yet"
        else -> "Checked with SHA-256 and signature before it installs"
    }

    val open = if (isInstalled) {
        val version = item.installed?.versionName.orEmpty()
        val label = if (status is AppStatus.Installed || version.isBlank()) "Open" else "Open $version"
        CardButton(label, Icons.AutoMirrored.Rounded.OpenInNew, PillStyle.Tonal) { vm.open(item) }
    } else null

    val buttons = buildList {
        when (status) {
            AppStatus.NotInstalled -> add(CardButton("Install", Icons.Outlined.FileDownload, PillStyle.Filled) { vm.install(item) })
            is AppStatus.UpdateAvailable -> add(CardButton("Update", Icons.Outlined.FileDownload, PillStyle.Filled) { vm.install(item) })
            is AppStatus.Busy -> if (status.phase is InstallPhase.Failed) {
                add(CardButton("Retry", Icons.Rounded.Refresh, PillStyle.Filled) { vm.install(item) })
            }
            else -> Unit
        }
        if (open != null) add(open)
        if (downloading != null) add(CardButton("Cancel", Icons.Rounded.Stop, PillStyle.Neutral) { vm.cancel(item) })
    }

    Surface(shape = StoreShapes.Card, color = colors.surface, modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(title, fontSize = 17.sp, fontWeight = Type.Emphasis, modifier = Modifier.weight(1f).alignByBaseline())
                if (downloading != null) {
                    Text(
                        "${(downloading.progress * 100).toInt()}%",
                        fontSize = 24.sp,
                        fontWeight = Type.Display,
                        color = colors.primary,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
            }
            if (downloading != null) WavyProgress(downloading.progress)
            Text(
                caption,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = if (phase is InstallPhase.Failed) colors.error else colors.onSurfaceVariant,
            )
            if (buttons.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 4.dp)) {
                    buttons.forEachIndexed { index, button ->
                        PillButton(
                            button.label,
                            onClick = button.onClick,
                            style = button.style,
                            icon = button.icon,
                            height = 52.dp,
                            fontSize = 15.sp,
                            shape = connectedShape(index, buttons.size),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

private fun connectedShape(index: Int, count: Int): Shape {
    val start = if (index == 0) 26.dp else 8.dp
    val end = if (index == count - 1) 26.dp else 8.dp
    return RoundedCornerShape(topStart = start, bottomStart = start, topEnd = end, bottomEnd = end)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Screenshots(app: CatalogApp) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Screenshots", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
        HorizontalMultiBrowseCarousel(
            state = rememberCarouselState { app.screenshots.size },
            preferredItemWidth = 200.dp,
            itemSpacing = 8.dp,
            modifier = Modifier.fillMaxWidth().height(236.dp),
        ) { index ->
            AsyncImage(
                model = app.screenshots[index],
                contentDescription = "Screenshot ${index + 1} of ${app.name}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .maskClip(StoreShapes.Card)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            )
        }
    }
}

@Composable
private fun TextCard(title: String, body: String) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = StoreShapes.Card, color = colors.surface, modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(20.dp)) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(body, fontSize = 14.sp, lineHeight = 21.sp, color = colors.onSurfaceVariant)
        }
    }
}

private data class InfoLine(val label: String, val value: String, val url: String? = null)

@Composable
private fun InfoGroup(item: AppUi) {
    val app = item.app
    val colors = MaterialTheme.colorScheme
    val uri = LocalUriHandler.current
    val lines = buildList {
        add(InfoLine("Package", app.packageName))
        add(InfoLine("Requires", androidVersionName(app.minSdk)))
        if (app.releasedAt.isNotBlank()) add(InfoLine("Released", app.releasedAt.take(10)))
        item.installed?.let { add(InfoLine("Installed", it.versionName.ifBlank { "Yes" })) }
        if (app.repo.isNotBlank()) add(InfoLine("Source", app.repo, url = "https://github.com/${app.repo}"))
    }
    Column(verticalArrangement = Arrangement.spacedBy(GroupGap)) {
        lines.forEachIndexed { index, line ->
            Surface(shape = groupedShape(index, lines.size), color = colors.surface, modifier = Modifier.fillMaxWidth()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .then(if (line.url != null) Modifier.clickable { uri.openUri(line.url) } else Modifier)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                ) {
                    Text(line.label, fontSize = 14.sp, color = colors.onSurfaceVariant)
                    Text(
                        line.value,
                        fontSize = 14.sp,
                        textAlign = TextAlign.End,
                        fontWeight = if (line.url != null) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (line.url != null) colors.primary else colors.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
