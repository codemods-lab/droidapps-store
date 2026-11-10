package com.innovcrest.utildroid.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.innovcrest.utildroid.ui.StoreViewModel
import com.innovcrest.utildroid.ui.common.SectionLabel
import com.innovcrest.utildroid.ui.common.openInstallPermissionSettings
import com.innovcrest.utildroid.ui.common.relativeTime
import com.innovcrest.utildroid.ui.theme.Brand

private val intervals = listOf(6 to "6 h", 12 to "12 h", 24 to "24 h", 0 to "Off")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: StoreViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    var url by rememberSaveable(settings.catalogUrl) { mutableStateOf(settings.catalogUrl) }
    val urlChanged = url.trim() != settings.catalogUrl

    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 32.dp),
    ) {
        Text("Settings", fontSize = 26.sp, fontWeight = FontWeight.Bold)

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel("Catalog")
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Catalog URL") },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    vm.setCatalogUrl(url)
                    focus.clearFocus()
                }),
                trailingIcon = if (urlChanged) {
                    {
                        IconButton(onClick = {
                            vm.setCatalogUrl(url)
                            focus.clearFocus()
                        }) { Icon(Icons.Outlined.Check, contentDescription = "Save catalog URL") }
                    }
                } else null,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Last refreshed ${relativeTime(ui.lastRefreshedMillis)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(onClick = vm::refresh) { Text("Refresh now", fontWeight = FontWeight.SemiBold) }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SectionLabel("Updates", Modifier.padding(bottom = 6.dp))
            Text("Check for updates every", fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                intervals.forEachIndexed { index, (hours, label) ->
                    SegmentedButton(
                        selected = settings.checkIntervalHours == hours,
                        onClick = { vm.setCheckInterval(hours) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = intervals.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = Brand.Accent,
                            activeContentColor = Brand.OnAccent,
                        ),
                        icon = {},
                    ) { Text(label) }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SwitchRow("Wi-Fi only", "Background downloads wait for unmetered network", settings.wifiOnly, vm::setWifiOnly)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SwitchRow(
                "Install updates silently",
                if (Build.VERSION.SDK_INT >= 31) "No confirm tap for apps this store installed" else "Needs Android 12 or later",
                settings.silentUpdates,
                vm::setSilentUpdates,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SwitchRow("Notify about updates", "One notification per check", settings.notify, vm::setNotify)
        }

        if (!ui.canInstall) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Permissions")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Installing apps is not allowed yet", fontSize = 14.sp, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { openInstallPermissionSettings(context) }) { Text("Allow") }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel("About")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("UtilDroid Store ${vm.ownVersion}", fontSize = 14.sp)
                    val self = ui.self
                    if (self != null && self.app.versionName.isNotBlank()) {
                        Text("Latest in catalog: ${self.app.versionName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                OutlinedButton(onClick = vm::refresh) { Text("Check update", fontWeight = FontWeight.SemiBold) }
            }
            if (vm.ownCertFingerprint.isNotBlank()) {
                Text("Signing key (SHA-256), for apps.yaml", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SelectionContainer {
                    Text(vm.ownCertFingerprint, fontSize = 11.sp, lineHeight = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 14.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Brand.Accent, checkedThumbColor = Brand.OnAccent),
        )
    }
}
