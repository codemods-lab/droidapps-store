package com.innovcrest.utildroid.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovcrest.utildroid.installer.InstallPhase
import com.innovcrest.utildroid.ui.AppStatus
import com.innovcrest.utildroid.ui.AppUi
import com.innovcrest.utildroid.ui.StoreUi
import com.innovcrest.utildroid.ui.StoreViewModel

private data class SheetButton(
    val label: String,
    val style: PillStyle,
    val icon: ImageVector? = null,
    val enabled: Boolean = true,
    val weight: Float = 1f,
    val onClick: () -> Unit,
)

private fun sheetButtonShape(index: Int, count: Int): Shape {
    val start = if (index == 0) 28.dp else 8.dp
    val end = if (index == count - 1) 28.dp else 8.dp
    return RoundedCornerShape(topStart = start, bottomStart = start, topEnd = end, bottomEnd = end)
}

/**
 * Bottom sheet shown when an app is tapped: what it is, the actions it allows right now,
 * the first lines of the release notes, and a way into the full details page.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppSheet(
    item: AppUi?,
    canInstall: Boolean,
    onPrimary: (AppUi) -> Unit,
    onOpen: (AppUi) -> Unit,
    onUninstall: (AppUi) -> Unit,
    onDetails: (AppUi) -> Unit,
    onDismiss: () -> Unit,
) {
    // The app can vanish from the catalog while the sheet is up.
    if (item == null) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val app = item.app
    val status = item.status
    val phase = (status as? AppStatus.Busy)?.phase
    val versionChip = when (status) {
        is AppStatus.UpdateAvailable -> "${status.installedVersionName} → ${app.versionName}"
        else -> app.versionName
    }
    val chips = listOf(app.category, versionChip, formatSize(app.sizeBytes)).filter { it.isNotBlank() && it != "—" }
    val needsPermission = !canInstall && (status is AppStatus.NotInstalled || status is AppStatus.UpdateAvailable)

    val buttons = buildList {
        when {
            needsPermission -> add(
                SheetButton("Allow installs", PillStyle.Filled, Icons.Outlined.Shield, weight = 2f) {
                    openInstallPermissionSettings(context)
                },
            )
            status is AppStatus.NotInstalled ->
                add(SheetButton("Install", PillStyle.Filled, Icons.Rounded.FileDownload, weight = 2f) { onPrimary(item) })
            status is AppStatus.UpdateAvailable ->
                add(SheetButton("Update", PillStyle.Filled, Icons.Rounded.FileDownload, weight = 2f) { onPrimary(item) })
            status is AppStatus.Unavailable ->
                add(SheetButton("Coming soon", PillStyle.Neutral, enabled = false) {})
            phase is InstallPhase.Downloading ->
                add(SheetButton("Cancel", PillStyle.Neutral, Icons.Rounded.Stop) { onPrimary(item) })
            phase is InstallPhase.Failed ->
                add(SheetButton("Retry", PillStyle.Filled, Icons.Rounded.Refresh, weight = 2f) { onPrimary(item) })
            phase != null -> add(SheetButton("Installing…", PillStyle.Neutral, enabled = false) {})
        }
        if (item.installed != null && !item.isSelf) {
            val onlyButton = isEmpty()
            add(SheetButton("Open", if (onlyButton) PillStyle.Filled else PillStyle.Tonal) { onOpen(item) })
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = colors.background,
        contentColor = colors.onBackground,
        dragHandle = {
            Surface(
                shape = CircleShape,
                color = colors.outline,
                modifier = Modifier.padding(top = 10.dp, bottom = 8.dp).size(width = 32.dp, height = 4.dp),
            ) {}
        },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 28.dp).navigationBarsPadding(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(horizontal = 4.dp),
            ) {
                AppIcon(app, 72.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        app.name,
                        fontSize = 26.sp,
                        lineHeight = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (app.summary.isNotBlank()) {
                        Text(app.summary, fontSize = 14.sp, lineHeight = 20.sp, color = colors.onSurfaceVariant)
                    }
                }
            }

            if (chips.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 4.dp),
                ) {
                    chips.forEach { chip ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = colors.surfaceContainerHigh,
                            contentColor = colors.onSurface,
                            modifier = Modifier.height(30.dp),
                        ) {
                            Box(Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                                Text(chip, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            if (phase != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 4.dp)) {
                    downloadProgress(item)?.let { WavyProgress(it) }
                    Text(
                        statusLine(item),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (phase is InstallPhase.Failed) colors.error else colors.primary,
                    )
                }
            }

            if (buttons.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    buttons.forEachIndexed { index, button ->
                        PillButton(
                            button.label,
                            onClick = button.onClick,
                            style = button.style,
                            enabled = button.enabled,
                            icon = button.icon,
                            height = 56.dp,
                            fontSize = 16.sp,
                            shape = sheetButtonShape(index, buttons.size),
                            modifier = Modifier.weight(button.weight),
                        )
                    }
                }
            }

            if (app.changelog.isNotBlank()) {
                Surface(shape = RoundedCornerShape(20.dp), color = colors.surface, modifier = Modifier.fillMaxWidth()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    ) {
                        Text("What's new in ${app.versionName}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(
                            app.changelog.trim(),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = colors.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            val canUninstall = item.installed != null && !item.isSelf
            val rows = if (canUninstall) 2 else 1
            Column(verticalArrangement = Arrangement.spacedBy(GroupGap)) {
                SheetRow(
                    "View details",
                    Icons.Outlined.Info,
                    tint = colors.onSurface,
                    iconTint = colors.onSurfaceVariant,
                    shape = sheetRowShape(0, rows),
                    chevron = true,
                    onClick = { onDetails(item) },
                )
                if (canUninstall) {
                    SheetRow(
                        "Uninstall",
                        Icons.Outlined.Delete,
                        tint = colors.error,
                        iconTint = colors.error,
                        shape = sheetRowShape(1, rows),
                        chevron = false,
                        onClick = { onUninstall(item) },
                    )
                }
            }
        }
    }
}

private fun sheetRowShape(index: Int, count: Int): Shape {
    val top = if (index == 0) 20.dp else 6.dp
    val bottom = if (index == count - 1) 20.dp else 6.dp
    return RoundedCornerShape(topStart = top, topEnd = top, bottomEnd = bottom, bottomStart = bottom)
}

@Composable
private fun SheetRow(
    label: String,
    icon: ImageVector,
    tint: Color,
    iconTint: Color,
    shape: Shape,
    chevron: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(onClick = onClick, shape = shape, color = colors.surface, contentColor = tint, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.heightIn(min = 60.dp).padding(horizontal = 16.dp),
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (chevron) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** Shows the sheet for [appId], if there is one, and wires its actions to the view model. */
@Composable
fun AppSheetHost(
    appId: String?,
    ui: StoreUi,
    vm: StoreViewModel,
    onDetails: (AppUi) -> Unit,
    onDismiss: () -> Unit,
) {
    if (appId == null) return
    AppSheet(
        item = ui.apps.firstOrNull { it.app.id == appId },
        canInstall = ui.canInstall,
        onPrimary = vm::primaryAction,
        onOpen = vm::open,
        onUninstall = {
            onDismiss()
            vm.uninstall(it)
        },
        onDetails = {
            onDismiss()
            onDetails(it)
        },
        onDismiss = onDismiss,
    )
}
