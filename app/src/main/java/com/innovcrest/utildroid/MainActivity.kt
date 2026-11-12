package com.innovcrest.utildroid

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.innovcrest.utildroid.data.ThemeMode
import com.innovcrest.utildroid.ui.StoreApp
import com.innovcrest.utildroid.ui.StoreViewModel
import com.innovcrest.utildroid.ui.theme.UtilDroidTheme
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    private val vm: StoreViewModel by viewModels()
    private var openUpdates by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        openUpdates = intent?.getStringExtra(EXTRA_OPEN) == OPEN_UPDATES
        // Read the saved theme before the first frame so a dark-theme user never sees a light flash.
        val savedTheme = runBlocking { (application as UtilDroidApp).container.settings.current().themeMode }
        setContent {
            val themeMode by remember { vm.settings.map { it.themeMode } }.collectAsStateWithLifecycle(savedTheme)
            val dark = when (themeMode) {
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
                ThemeMode.System -> isSystemInDarkTheme()
            }
            // Status and navigation bar icons follow the app theme, not the phone's.
            LaunchedEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            UtilDroidTheme(dark = dark) {
                NotificationPermissionRequest()
                StoreApp(vm, openUpdates = openUpdates, onUpdatesOpened = { openUpdates = false })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getStringExtra(EXTRA_OPEN) == OPEN_UPDATES) openUpdates = true
    }

    override fun onResume() {
        super.onResume()
        vm.onResume()
    }

    companion object {
        const val EXTRA_OPEN = "open"
        const val OPEN_UPDATES = "updates"
    }
}

/** Asks once for notification permission on Android 13+. */
@Composable
private fun NotificationPermissionRequest() {
    if (Build.VERSION.SDK_INT < 33) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
