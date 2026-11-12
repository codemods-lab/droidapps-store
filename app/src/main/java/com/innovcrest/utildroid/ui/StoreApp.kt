package com.innovcrest.utildroid.ui

import android.net.Uri
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.innovcrest.utildroid.ui.screens.AllAppsScreen
import com.innovcrest.utildroid.ui.screens.BrowseScreen
import com.innovcrest.utildroid.ui.screens.DetailScreen
import com.innovcrest.utildroid.ui.screens.SettingsScreen
import com.innovcrest.utildroid.ui.screens.UpdatesScreen

/** Navigation routes. Tabs show the bottom bar; the rest are pushed on top. */
private object Routes {
    const val BROWSE = "browse"
    const val UPDATES = "updates"
    const val SETTINGS = "settings"
    const val ALL_APPS = "apps?category={category}"
    const val DETAIL = "detail/{id}"
    const val ARG_CATEGORY = "category"
    const val ARG_ID = "id"

    fun allApps(category: String?) = if (category == null) "apps" else "apps?category=${Uri.encode(category)}"
    fun detail(appId: String) = "detail/$appId"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

private val tabs = listOf(
    Tab(Routes.BROWSE, "Browse", Icons.Outlined.GridView, Icons.Filled.GridView),
    Tab(Routes.UPDATES, "Updates", Icons.Outlined.FileDownload, Icons.Filled.FileDownload),
    Tab(Routes.SETTINGS, "Settings", Icons.Outlined.Tune, Icons.Filled.Tune),
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
            nav.goToTab(Routes.UPDATES)
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
                                if (tab.route == Routes.UPDATES && ui.pendingUpdateCount > 0) {
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
        NavHost(nav, startDestination = Routes.BROWSE, modifier = Modifier.padding(padding)) {
            composable(Routes.BROWSE) {
                BrowseScreen(
                    vm,
                    onDetails = { nav.navigate(Routes.detail(it.app.id)) },
                    onSeeAll = { category -> nav.navigate(Routes.allApps(category)) },
                    onOpenUpdates = { nav.goToTab(Routes.UPDATES) },
                )
            }
            composable(
                Routes.ALL_APPS,
                arguments = listOf(navArgument(Routes.ARG_CATEGORY) { type = NavType.StringType; nullable = true }),
            ) { entry ->
                AllAppsScreen(
                    vm,
                    initialCategory = entry.arguments?.getString(Routes.ARG_CATEGORY),
                    onBack = { nav.popBackStack() },
                    onDetails = { nav.navigate(Routes.detail(it.app.id)) },
                )
            }
            composable(Routes.UPDATES) {
                UpdatesScreen(vm, onOpen = { nav.navigate(Routes.detail(it.app.id)) })
            }
            composable(Routes.SETTINGS) { SettingsScreen(vm) }
            composable(Routes.DETAIL) { entry ->
                DetailScreen(vm, entry.arguments?.getString(Routes.ARG_ID).orEmpty(), onBack = { nav.popBackStack() })
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
