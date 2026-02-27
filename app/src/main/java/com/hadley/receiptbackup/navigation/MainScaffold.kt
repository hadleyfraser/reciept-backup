package com.hadley.receiptbackup.navigation

import android.app.Activity

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.Coil
import coil.annotation.ExperimentalCoilApi
import com.hadley.receiptbackup.R
import com.hadley.receiptbackup.auth.GoogleAuthManager
import com.hadley.receiptbackup.data.local.NavigationPreferences
import com.hadley.receiptbackup.data.repository.ReceiptItemViewModel
import com.hadley.receiptbackup.data.repository.LoyaltyCardViewModel
import com.hadley.receiptbackup.ui.components.AppDrawerScaffold
import com.hadley.receiptbackup.ui.components.AppScaffoldState
import com.hadley.receiptbackup.ui.components.LocalAppScaffoldState
import com.hadley.receiptbackup.ui.screens.DetailScreen
import com.hadley.receiptbackup.ui.screens.FullScreenImageScreen
import com.hadley.receiptbackup.ui.screens.ListScreen
import com.hadley.receiptbackup.ui.screens.SettingsScreen
import com.hadley.receiptbackup.ui.screens.AddEditLoyaltyCardScreen
import com.hadley.receiptbackup.ui.screens.LoyaltyCardDetailScreen
import com.hadley.receiptbackup.ui.screens.LoyaltyCardListScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalCoilApi::class)
@Composable
fun MainScaffold(
    rootNavController: NavController,
    viewModel: ReceiptItemViewModel,
    loyaltyCardViewModel: LoyaltyCardViewModel
) {
    val activity = LocalContext.current as Activity
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val navController = rememberNavController()
    val scaffoldState = remember { AppScaffoldState() }
    val didRestore = remember { mutableStateOf(false) }
    val persistedRoutes = remember { setOf("list", "cards", "settings") }

    val routeState = remember { mutableStateOf<RouteState>(RouteState.Loading) }
    LaunchedEffect(context) {
        val lastRoute = NavigationPreferences.lastRoute(context).first()
        routeState.value = RouteState.Ready(lastRoute)
        didRestore.value = true
    }

    val startDestination = when (val state = routeState.value) {
        is RouteState.Loading -> null
        is RouteState.Ready -> {
            val lastRoute = state.route
            if (lastRoute != null && lastRoute in persistedRoutes) lastRoute else "list"
        }
    }

    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { entry ->
            val route = entry.destination.route
            if (didRestore.value && route != null && route in persistedRoutes) {
                NavigationPreferences.saveLastRoute(context, route)
            }
        }
    }

    CompositionLocalProvider(LocalAppScaffoldState provides scaffoldState) {
        AppDrawerScaffold(
            navController = navController,
            title = scaffoldState.title,
            actions = {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(24.dp)
                )
            },
            floatingActionButton = scaffoldState.floatingActionButton,
            showTopBar = scaffoldState.showTopBar,
            drawerEnabled = scaffoldState.drawerEnabled,
            snackbarHostState = scaffoldState.snackbarHostState,
            drawerContent = { closeDrawer ->
                NavigationDrawerItem(
                    label = { Text("Logout") },
                    selected = false,
                    onClick = {
                        closeDrawer()
                        coroutineScope.launch {
                            viewModel.clearItems()
                            loyaltyCardViewModel.clearCards()
                            GoogleAuthManager.signOut()
                            rootNavController.navigate("landing") {
                                popUpTo("main") { inclusive = true }
                            }
                            launch(Dispatchers.IO) {
                                GoogleAuthManager.clearCredentialState(activity)
                                viewModel.clearLocalCache(activity)
                                viewModel.clearCachedImages(activity)
                                loyaltyCardViewModel.clearLocalCache(activity)
                                val imageLoader = Coil.imageLoader(activity)
                                imageLoader.diskCache?.clear()
                                imageLoader.memoryCache?.clear()
                            }
                        }
                    },
                    icon = {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                    },
                    colors = NavigationDrawerItemDefaults.colors()
                )
            }
        ) { paddingValues: PaddingValues ->
            val destination = startDestination ?: return@AppDrawerScaffold
            NavHost(
                navController = navController,
                startDestination = destination
            ) {
                composable("list") {
                    ListScreen(navController, viewModel, paddingValues)
                }

                composable("settings") {
                    SettingsScreen(navController, viewModel, paddingValues)
                }

                composable(
                    route = "detail/{itemId}",
                    arguments = listOf(navArgument("itemId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
                    DetailScreen(navController, itemId, viewModel, paddingValues)
                }

                composable(
                    route = "image?uri={uri}",
                    arguments = listOf(navArgument("uri") { type = NavType.StringType })
                ) { backStackEntry ->
                    val uri = backStackEntry.arguments?.getString("uri") ?: ""
                    FullScreenImageScreen(navController, uri, paddingValues)
                }

                composable("cards") {
                    LoyaltyCardListScreen(navController, loyaltyCardViewModel, paddingValues)
                }

                composable(
                    route = "cardDetail/{cardId}",
                    arguments = listOf(navArgument("cardId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val cardId = backStackEntry.arguments?.getString("cardId") ?: ""
                    LoyaltyCardDetailScreen(navController, cardId, loyaltyCardViewModel, paddingValues)
                }

                composable(
                    route = "cardEdit?cardId={cardId}",
                    arguments = listOf(navArgument("cardId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    })
                ) { backStackEntry ->
                    val cardId = backStackEntry.arguments?.getString("cardId")
                    AddEditLoyaltyCardScreen(navController, loyaltyCardViewModel, cardId, paddingValues)
                }
            }
        }
    }
}

private sealed interface RouteState {
    data object Loading : RouteState
    data class Ready(val route: String?) : RouteState
}
