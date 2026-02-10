package com.hadley.receiptbackup.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.launch

data class DrawerDestination(
    val label: String,
    val route: String,
    val icon: @Composable () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawerScaffold(
    navController: NavController,
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val destinations = listOf(
        DrawerDestination(
            label = "Receipts",
            route = "list",
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) }
        ),
        DrawerDestination(
            label = "Reports",
            route = "reports",
            icon = { Icon(Icons.Default.Info, contentDescription = null) }
        ),
        DrawerDestination(
            label = "Settings",
            route = "settings",
            icon = { Icon(Icons.Default.Settings, contentDescription = null) }
        )
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column {
                    Text(
                        text = "Functions",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    destinations.forEach { destination ->
                        val selected = when (destination.route) {
                            "list" -> currentRoute == "list" || currentRoute?.startsWith("detail/") == true || currentRoute?.startsWith("image") == true
                            else -> currentRoute == destination.route
                        }
                        NavigationDrawerItem(
                            label = { Text(destination.label) },
                            selected = selected,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                if (!selected) {
                                    navController.navigate(destination.route) {
                                        launchSingleTop = true
                                        restoreState = true
                                        popUpTo("list") { saveState = true }
                                    }
                                }
                            },
                            icon = destination.icon,
                            colors = NavigationDrawerItemDefaults.colors()
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            modifier = modifier,
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open menu")
                        }
                    },
                    actions = { actions() }
                )
            },
            floatingActionButton = floatingActionButton,
            content = content
        )
    }
}
