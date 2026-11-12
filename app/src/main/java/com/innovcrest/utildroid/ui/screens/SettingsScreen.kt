package com.innovcrest.utildroid.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.innovcrest.utildroid.ui.AppStatus
import com.innovcrest.utildroid.ui.AppUi
import com.innovcrest.utildroid.ui.StoreViewModel
import com.innovcrest.utildroid.ui.common.GroupGap
import com.innovcrest.utildroid.ui.common.PillButton
import com.innovcrest.utildroid.ui.common.PillStyle
import com.innovcrest.utildroid.ui.common.ScreenTitle
import com.innovcrest.utildroid.ui.common.SectionHeader
import com.innovcrest.utildroid.ui.common.StoreGlyph
import com.innovcrest.utildroid.ui.common.groupedShape
import com.innovcrest.utildroid.ui.common.openInstallPermissionSettings
import com.innovcrest.utildroid.ui.common.relativeTime

private val intervals = listOf(6 to "6 h", 12 to "12 h", 24 to "24 h", 0 to "Off")

@Composable
fun SettingsScreen(vm: StoreViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
    ) {
        ScreenTitle("Settings", modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 40.dp, bottom = 20.dp))

        SectionHeader("Updates", Modifier.padding(bottom = 12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(GroupGap)) {
            Surface(shape = groupedShape(0, 4), color = colors.surface, modifier = Modifier.fillMaxWidth()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 16.dp),
                ) {
                    RowText("Check for updates", "Last checked ${relativeTime(ui.lastRefreshedMillis)}")
                    IntervalPicker(selectedHours = settings.checkIntervalHours, onSelect = { vm.setCheckInterval(it) })
                }
            }
            SwitchRow(
                "Wi-Fi only",
                "Wait for Wi-Fi before downloading updates in the background",
                settings.wifiOnly,
                { vm.setWifiOnly(it) },
                groupedShape(1, 4),
            )
            SwitchRow(
                "Update apps automatically",
                if (Build.VERSION.SDK_INT >= 31) "Installs in the background, no confirm tap" else "Needs Android 12 or later",
                settings.silentUpdates,
                { vm.setSilentUpdates(it) },
                groupedShape(2, 4),
            )
            SwitchRow(
                "Update notifications",
                "Tell me when updates are ready",
                settings.notify,
                { vm.setNotify(it) },
                groupedShape(3, 4),
            )
        }

        if (!ui.canInstall) {
            SectionHeader("Permissions", Modifier.padding(top = 24.dp, bottom = 12.dp))
            Surface(shape = groupedShape(0, 1), color = colors.surface, modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.heightIn(min = 76.dp).padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    RowText("Allow app installs", "Android needs this once before the store can install apps", Modifier.weight(1f))
                    PillButton("Allow", onClick = { openInstallPermissionSettings(context) })
                }
            }
        }

        SectionHeader("Catalog", Modifier.padding(top = 24.dp, bottom = 12.dp))
        CatalogUrlCard(saved = settings.catalogUrl, onSave = { vm.setCatalogUrl(it) })

        SectionHeader("About", Modifier.padding(top = 24.dp, bottom = 12.dp))
        val hasKey = vm.ownCertFingerprint.isNotBlank()
        val aboutRows = if (hasKey) 3 else 2
        Column(verticalArrangement = Arrangement.spacedBy(GroupGap)) {
            Surface(shape = groupedShape(0, aboutRows), color = colors.surface, modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(16.dp),
                ) {
                    StoreGlyph(40.dp, container = colors.primaryContainer, content = colors.onPrimaryContainer)
                    RowText("DroidApps Store", versionLine(vm.ownVersion, ui.self), Modifier.weight(1f))
                }
            }
            Surface(
                onClick = vm::refresh,
                shape = groupedShape(1, aboutRows),
                color = colors.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.heightIn(min = 64.dp).padding(horizontal = 16.dp),
                ) {
                    Text("Check for store update", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Icon(
                        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            if (hasKey) {
                Surface(shape = groupedShape(2, aboutRows), color = colors.surface, modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(16.dp)) {
                        RowText("Signing key", "SHA-256, for apps.yaml")
                        SelectionContainer {
                            Text(vm.ownCertFingerprint, fontSize = 12.sp, lineHeight = 18.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

private fun versionLine(ownVersion: String, self: AppUi?): String {
    val state = when (self?.status) {
        is AppStatus.Installed -> "up to date"
        is AppStatus.UpdateAvailable -> "${self.app.versionName} available"
        is AppStatus.Busy -> "updating"
        else -> null
    }
    return listOfNotNull("Version $ownVersion", state).joinToString(" · ")
}

@Composable
private fun RowText(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text(subtitle, fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Connected button group: the selected option goes fully round, the rest keep tight inner corners. */
@Composable
private fun IntervalPicker(selectedHours: Int, onSelect: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth().selectableGroup()) {
        intervals.forEachIndexed { index, (hours, label) ->
            val selected = selectedHours == hours
            val shape: Shape = when {
                selected -> CircleShape
                index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 8.dp, bottomEnd = 8.dp, bottomStart = 24.dp)
                index == intervals.lastIndex -> RoundedCornerShape(topStart = 8.dp, topEnd = 24.dp, bottomEnd = 24.dp, bottomStart = 8.dp)
                else -> RoundedCornerShape(8.dp)
            }
            Surface(
                selected = selected,
                onClick = { onSelect(hours) },
                shape = shape,
                color = if (selected) colors.primary else colors.surfaceContainerHigh,
                contentColor = if (selected) colors.onPrimary else colors.onSurface,
                modifier = Modifier.weight(1f).height(48.dp).semantics { role = Role.RadioButton },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(label, fontSize = 15.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit, shape: Shape) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = shape, color = colors.surface, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
                .heightIn(min = 76.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            RowText(title, subtitle, Modifier.weight(1f))
            Switch(
                checked = checked,
                onCheckedChange = null,
                thumbContent = if (checked) {
                    { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                } else null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.onPrimary,
                    checkedTrackColor = colors.primary,
                    checkedIconColor = colors.primary,
                    uncheckedThumbColor = colors.outline,
                    uncheckedTrackColor = colors.surfaceVariant,
                    uncheckedBorderColor = colors.outline,
                ),
            )
        }
    }
}

@Composable
private fun CatalogUrlCard(saved: String, onSave: (String) -> Unit) {
    val focus = LocalFocusManager.current
    var url by rememberSaveable(saved) { mutableStateOf(saved) }
    val changed = url.trim() != saved
    val save = {
        onSave(url)
        focus.clearFocus()
    }
    Surface(shape = groupedShape(0, 1), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
            RowText("Catalog URL", "Where the store reads its app list. Clear it to go back to the default.")
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { save() }),
                trailingIcon = if (changed) {
                    { IconButton(onClick = save) { Icon(Icons.Rounded.Check, contentDescription = "Save catalog URL") } }
                } else null,
                modifier = Modifier.fillMaxWidth(),
            )
            if (changed) PillButton("Save and refresh", onClick = save, style = PillStyle.Tonal)
        }
    }
}
