package com.innovcrest.utildroid.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
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
import com.innovcrest.utildroid.ui.theme.Brand

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("browse", "Browse", Icons.Outlined.GridView),
    Tab("updates", "Updates", Icons.Outlined.Download),
    Tab("settings", "Settings", Icons.Outlined.Tune),
)

@Composable
fun StoreApp(vm: StoreViewModel, openUpdates: Boolean, onUpdatesOpened: () -> Unit) {
    val nav = rememberNavController()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route

    LaunchedEffect(openUpdates) {
        if (openUpdates) {
            nav.goToTab("updates")
            onUpdatesOpened()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (tabs.any { it.route == route }) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = { nav.goToTab(tab.route) },
                            label = { Text(tab.label) },
                            icon = {
                                if (tab.route == "updates" && ui.pendingUpdateCount > 0) {
                                    BadgedBox(badge = {
                                        Badge(containerColor = Brand.Accent, contentColor = Brand.OnAccent) {
                                            Text(ui.pendingUpdateCount.toString())
                                        }
                                    }) { Icon(tab.icon, contentDescription = null) }
                                } else {
                                    Icon(tab.icon, contentDescription = null)
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = "browse", modifier = Modifier.padding(padding)) {
            composable("browse") {
                BrowseScreen(
                    vm,
                    onOpen = { nav.navigate("detail/${it.app.id}") },
                    onShowUpdates = { nav.goToTab("updates") },
                )
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
