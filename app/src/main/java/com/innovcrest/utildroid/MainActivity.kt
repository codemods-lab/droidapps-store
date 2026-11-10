package com.innovcrest.utildroid

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.innovcrest.utildroid.ui.StoreApp
import com.innovcrest.utildroid.ui.StoreViewModel
import com.innovcrest.utildroid.ui.theme.UtilDroidTheme

class MainActivity : ComponentActivity() {

    private val vm: StoreViewModel by viewModels()
    private var openUpdates by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        openUpdates = intent?.getStringExtra(EXTRA_OPEN) == OPEN_UPDATES
        setContent {
            UtilDroidTheme {
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
