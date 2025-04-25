package com.example.receiptbackup.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.receiptbackup.data.repository.ReceiptItemViewModel
import com.example.receiptbackup.ui.screens.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.asPaddingValues

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    receiptItemViewModel: ReceiptItemViewModel
) {
    val navController = rememberNavController()

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(WindowInsets.safeDrawing.asPaddingValues()) // ✅ apply insets globally here
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

            composable(
                route = "detail/{itemId}",
                arguments = listOf(navArgument("itemId") { type = NavType.StringType })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
                DetailScreen(navController, itemId, receiptItemViewModel)
            }

            composable(
                route = "edit/{itemId}",
                arguments = listOf(navArgument("itemId") { type = NavType.StringType })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
                AddEditItemScreen(navController, receiptItemViewModel, receiptItemViewModel.getItemById((itemId)))
            }

            composable("add") {
                AddEditItemScreen(navController, receiptItemViewModel)
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
