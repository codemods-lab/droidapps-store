package com.innovcrest.utildroid.ui.screens

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.innovcrest.utildroid.ui.AppStatus
import com.innovcrest.utildroid.ui.AppUi
import com.innovcrest.utildroid.ui.SortOrder
import com.innovcrest.utildroid.ui.inCategory
import com.innovcrest.utildroid.ui.isNew
import com.innovcrest.utildroid.ui.sortedBy
import com.innovcrest.utildroid.ui.StoreViewModel
import com.innovcrest.utildroid.ui.common.AppIcon
import com.innovcrest.utildroid.ui.common.AppSheetHost
import com.innovcrest.utildroid.ui.common.CategoryChip
import com.innovcrest.utildroid.ui.common.EmptyState
import com.innovcrest.utildroid.ui.common.GroupGap
import com.innovcrest.utildroid.ui.common.ListAction
import com.innovcrest.utildroid.ui.common.StatusPill
import com.innovcrest.utildroid.ui.common.WavyProgress
import com.innovcrest.utildroid.ui.common.downloadProgress
import com.innovcrest.utildroid.ui.common.groupedShape
import com.innovcrest.utildroid.ui.common.metaLine
import com.innovcrest.utildroid.ui.theme.Type

/** Every app in the store, filtered by category, as a two-column grid or a list. */
@Composable
fun AllAppsScreen(
    vm: StoreViewModel,
    initialCategory: String?,
    onBack: () -> Unit,
    onDetails: (AppUi) -> Unit,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var category by rememberSaveable { mutableStateOf(initialCategory) }
    var sortName by rememberSaveable { mutableStateOf(SortOrder.Featured.name) }
    var grid by rememberSaveable { mutableStateOf(true) }
    var sheetAppId by rememberSaveable { mutableStateOf<String?>(null) }
    val sort = SortOrder.valueOf(sortName)

    val visible = ui.apps.inCategory(category).sortedBy(sort)
    val openSheet: (AppUi) -> Unit = { sheetAppId = it.app.id }

    Column(Modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
        ) {
            IconButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp).size(48.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(horizontal = 20.dp),
            ) {
                Text(
                    "All apps",
                    fontSize = 34.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    "${visible.size} app${if (visible.size == 1) "" else "s"}",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.alignByBaseline(),
                )
            }
            if (ui.categories.size > 1) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item { CategoryChip("All", category == null) { category = null } }
                    items(ui.categories) { cat -> CategoryChip(cat, category == cat) { category = cat } }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 16.dp),
            ) {
                SortMenu(sort, onSelect = { sortName = it.name })
                LayoutToggle(grid, onChange = { grid = it })
            }
        }

        when {
            visible.isEmpty() -> EmptyState("Nothing here yet", "No apps in this category.")
            grid -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(visible, key = { it.app.id }) { item -> GridCard(item, onClick = { openSheet(item) }) }
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(GroupGap),
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(visible, key = { _, item -> item.app.id }) { index, item ->
                    ListRow(
                        item,
                        shape = groupedShape(index, visible.size),
                        onClick = { openSheet(item) },
                        onAction = { vm.primaryAction(item) },
                    )
                }
            }
        }
    }

    AppSheetHost(sheetAppId, ui, vm, onDetails = onDetails, onDismiss = { sheetAppId = null })
}

@Composable
private fun SortMenu(sort: SortOrder, onSelect: (SortOrder) -> Unit) {
    val colors = MaterialTheme.colorScheme
    var open by rememberSaveable { mutableStateOf(false) }
    Box {
        Surface(
            onClick = { open = true },
            shape = CircleShape,
            color = Color.Transparent,
            contentColor = colors.onSurface,
            modifier = Modifier.height(40.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(start = 14.dp, end = 12.dp),
            ) {
                Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = "Sort by", modifier = Modifier.size(18.dp))
                Text(sort.label, fontSize = 14.sp, fontWeight = Type.Emphasis)
                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            SortOrder.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label, fontWeight = if (option == sort) FontWeight.Bold else FontWeight.Normal) },
                    onClick = {
                        open = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

/** Grid / list switch: the selected side goes fully round, the other keeps a tight inner corner. */
@Composable
private fun LayoutToggle(grid: Boolean, onChange: (Boolean) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.selectableGroup()) {
        ToggleSide(
            Icons.Rounded.GridView,
            "Grid",
            selected = grid,
            restingShape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 8.dp, bottomEnd = 8.dp),
            onClick = { onChange(true) },
        )
        ToggleSide(
            Icons.AutoMirrored.Rounded.ViewList,
            "List",
            selected = !grid,
            restingShape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 20.dp, bottomEnd = 20.dp),
            onClick = { onChange(false) },
        )
    }
}

@Composable
private fun ToggleSide(icon: ImageVector, label: String, selected: Boolean, restingShape: Shape, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        selected = selected,
        onClick = onClick,
        shape = if (selected) CircleShape else restingShape,
        color = if (selected) colors.primary else colors.surfaceContainerHigh,
        contentColor = if (selected) colors.onPrimary else colors.onSurface,
        modifier = Modifier.size(width = 52.dp, height = 40.dp).semantics { role = Role.RadioButton },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun GridCard(item: AppUi, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val highlight = item.status is AppStatus.NotInstalled && item.isNew() || item.status is AppStatus.Busy
    Surface(onClick = onClick, shape = RoundedCornerShape(28.dp), color = colors.surface, modifier = Modifier.height(196.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                AppIcon(item.app, 56.dp)
                StatusPill(item)
            }
            Text(
                item.app.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 12.dp),
            )
            if (item.app.summary.isNotBlank()) {
                Text(
                    item.app.summary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                metaLine(item),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (highlight) colors.primary else colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ListRow(item: AppUi, shape: Shape, onClick: () -> Unit, onAction: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val highlight = item.status is AppStatus.NotInstalled && item.isNew() || item.status is AppStatus.Busy
    Surface(onClick = onClick, shape = shape, color = colors.surface, modifier = Modifier.fillMaxWidth()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(start = 12.dp, top = 12.dp, end = 14.dp, bottom = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                AppIcon(item.app, 48.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(item.app.name, fontSize = 16.sp, fontWeight = Type.Emphasis, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (item.app.summary.isNotBlank()) {
                        Text(
                            item.app.summary,
                            fontSize = 13.sp,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        metaLine(item),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (highlight) colors.primary else colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                ListAction(item, onAction)
            }
            downloadProgress(item)?.let { WavyProgress(it) }
        }
    }
}
