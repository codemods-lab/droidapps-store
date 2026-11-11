package com.innovcrest.utildroid.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.innovcrest.utildroid.ui.screens.BrowseScreen
import com.innovcrest.utildroid.ui.screens.DetailScreen
import com.innovcrest.utildroid.ui.screens.SettingsScreen
import com.innovcrest.utildroid.ui.screens.UpdatesScreen

private data class Tab(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

private val tabs = listOf(
    Tab("browse", "Browse", Icons.Outlined.GridView, Icons.Filled.GridView),
    Tab("updates", "Updates", Icons.Outlined.FileDownload, Icons.Filled.FileDownload),
    Tab("settings", "Settings", Icons.Outlined.Tune, Icons.Filled.Tune),
)

@Composable
fun StoreApp(vm: StoreViewModel, openUpdates: Boolean, onUpdatesOpened: () -> Unit) {
    val nav = rememberNavController()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val colors = MaterialTheme.colorScheme

    LaunchedEffect(openUpdates) {
        if (openUpdates) {
            nav.goToTab("updates")
            onUpdatesOpened()
        }
    }

    Scaffold(
        containerColor = colors.background,
        bottomBar = {
            if (tabs.any { it.route == route }) {
                NavigationBar(containerColor = colors.surfaceContainerHigh) {
                    tabs.forEach { tab ->
                        val selected = route == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { nav.goToTab(tab.route) },
                            label = { Text(tab.label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
                            icon = {
                                val image = if (selected) tab.selectedIcon else tab.icon
                                if (tab.route == "updates" && ui.pendingUpdateCount > 0) {
                                    BadgedBox(badge = {
                                        Badge(containerColor = colors.error, contentColor = colors.onError) {
                                            Text(ui.pendingUpdateCount.toString(), fontWeight = FontWeight.Bold)
                                        }
                                    }) { Icon(image, contentDescription = null) }
                                } else {
                                    Icon(image, contentDescription = null)
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = colors.onSecondaryContainer,
                                selectedTextColor = colors.onSecondaryContainer,
                                indicatorColor = colors.primaryContainer,
                                unselectedIconColor = colors.onSurfaceVariant,
                                unselectedTextColor = colors.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = "browse", modifier = Modifier.padding(padding)) {
            composable("browse") {
                BrowseScreen(vm, onOpen = { nav.navigate("detail/${it.app.id}") })
            }
            composable("updates") {
                UpdatesScreen(vm, onOpen = { nav.navigate("detail/${it.app.id}") })
            }
            composable("settings") { SettingsScreen(vm) }
            composable("detail/{id}") { entry ->
                DetailScreen(vm, entry.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() })
            }
        }
    }
}

private fun NavHostController.goToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
