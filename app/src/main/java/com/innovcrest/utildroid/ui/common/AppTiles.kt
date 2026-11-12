package com.innovcrest.utildroid.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovcrest.utildroid.installer.InstallPhase
import com.innovcrest.utildroid.ui.AppStatus
import com.innovcrest.utildroid.ui.AppUi
import com.innovcrest.utildroid.ui.isNew
import com.innovcrest.utildroid.ui.theme.Type

/** Category line under a card or row, with what matters most about this app right now. */
fun metaLine(item: AppUi): String {
    val detail = when (val s = item.status) {
        is AppStatus.UpdateAvailable -> "${s.installedVersionName} → ${item.app.versionName}"
        is AppStatus.Installed -> "Installed"
        is AppStatus.Busy -> statusLine(item, compact = true)
        AppStatus.Unavailable -> null
        AppStatus.NotInstalled -> if (item.isNew()) "New" else null
    }
    return listOfNotNull(item.app.category, detail).joinToString(" · ")
}

/** Small state label on tiles and grid cards: Get / Update / Installed / Soon / progress. */
@Composable
fun StatusPill(item: AppUi, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val label: String
    var container = Color.Transparent
    var content = colors.onSurface
    var border: BorderStroke? = null
    var weight = FontWeight.Bold
    when (val s = item.status) {
        AppStatus.NotInstalled -> {
            label = "Get"
            border = BorderStroke(1.dp, colors.outlineVariant)
            weight = Type.Emphasis
        }
        is AppStatus.UpdateAvailable -> {
            label = "Update"
            container = colors.primaryContainer
            content = colors.onPrimaryContainer
        }
        is AppStatus.Installed -> {
            label = "Installed"
            container = colors.surfaceContainerHigh
            content = colors.onSurfaceVariant
            weight = Type.Emphasis
        }
        AppStatus.Unavailable -> {
            label = "Soon"
            container = SoonContainer
            content = SoonContent
        }
        is AppStatus.Busy -> when (val p = s.phase) {
            is InstallPhase.Downloading -> {
                label = "${(p.progress * 100).toInt()}%"
                container = colors.primaryContainer
                content = colors.onPrimaryContainer
            }
            is InstallPhase.Failed -> {
                label = "Retry"
                container = colors.errorContainer
                content = colors.onErrorContainer
            }
            InstallPhase.WaitingForUser -> {
                label = "Confirm"
                container = colors.primaryContainer
                content = colors.onPrimaryContainer
            }
            else -> {
                label = "Installing"
                container = colors.primaryContainer
                content = colors.onPrimaryContainer
            }
        }
    }
    Surface(shape = RoundedCornerShape(12.dp), color = container, contentColor = content, border = border, modifier = modifier.height(24.dp)) {
        Box(Modifier.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
            Text(label, fontSize = 12.sp, fontWeight = weight, maxLines = 1)
        }
    }
}

/** Square-ish tile for the home grids: icon, name and a status pill. */
@Composable
fun AppTile(item: AppUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.height(156.dp),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 14.dp)) {
            AppIcon(item.app, 52.dp)
            Text(
                item.app.name,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )
            Spacer(Modifier.weight(1f))
            StatusPill(item)
        }
    }
}

/** Lays [items] out in rows of [columns] equal cells; short rows are padded with empty cells. */
@Composable
fun <T> FixedGrid(
    items: List<T>,
    columns: Int,
    modifier: Modifier = Modifier,
    gap: Dp = 8.dp,
    cell: @Composable (T, Modifier) -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(gap)) {
        items.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                row.forEach { cell(it, Modifier.weight(1f)) }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
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

/** Trailing button on an "All apps" list row. */
@Composable
fun ListAction(item: AppUi, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    when (item.status) {
        AppStatus.NotInstalled -> Surface(
            onClick = onClick,
            shape = CircleShape,
            color = Color.Transparent,
            contentColor = colors.primary,
            border = BorderStroke(1.dp, colors.outline),
            modifier = Modifier.height(36.dp),
        ) {
            Box(Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                Text("Get", fontSize = 14.sp, fontWeight = Type.Emphasis)
            }
        }
        is AppStatus.UpdateAvailable -> PillButton("Update", onClick, height = 36.dp)
        is AppStatus.Installed -> PillButton("Open", onClick, style = PillStyle.Tonal, height = 36.dp)
        AppStatus.Unavailable -> Surface(
            shape = CircleShape,
            color = SoonContainer,
            contentColor = SoonContent,
            modifier = Modifier.height(36.dp),
        ) {
            Box(Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                Text("Soon", fontSize = 14.sp, fontWeight = Type.Emphasis)
            }
        }
        is AppStatus.Busy -> RowAction(item, onClick)
    }
}
