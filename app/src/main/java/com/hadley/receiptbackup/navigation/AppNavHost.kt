package com.hadley.receiptbackup.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hadley.receiptbackup.data.repository.ReceiptItemViewModel
import com.hadley.receiptbackup.ui.screens.LandingScreen

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

            composable("main") {
                MainScaffold(navController, receiptItemViewModel)
            }
        }
    }
}
