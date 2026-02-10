package com.hadley.receiptbackup.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.hadley.receiptbackup.data.repository.ReceiptItemViewModel
import com.hadley.receiptbackup.ui.screens.*

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    receiptItemViewModel: ReceiptItemViewModel
) {
    val navController = rememberNavController()

    Box(
        modifier = modifier
            .fillMaxSize()
            // removed global safeDrawing padding to avoid double top inset
    ) {
        NavHost(
            navController = navController,
            startDestination = "landing"
        ) {
            composable("landing") {
                LandingScreen(navController, receiptItemViewModel)
            }

            composable("list") {
                ListScreen(navController, receiptItemViewModel)
            }

            composable("reports") {
                ReportsScreen(navController)
            }

            composable("settings") {
                SettingsScreen(navController)
            }

            composable(
                route = "detail/{itemId}",
                arguments = listOf(navArgument("itemId") { type = NavType.StringType })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
                DetailScreen(navController, itemId, receiptItemViewModel)
            }

            composable(
                route = "image?uri={uri}",
                arguments = listOf(navArgument("uri") { type = NavType.StringType })
            ) { backStackEntry ->
                val uri = backStackEntry.arguments?.getString("uri") ?: ""
                FullScreenImageScreen(navController, uri)
            }
        }
    }
}
