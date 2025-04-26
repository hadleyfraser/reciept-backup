package com.hadley.receiptbackup.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hadley.receiptbackup.data.repository.ReceiptItemViewModel
import com.hadley.receiptbackup.ui.screens.AddEditItemScreen
import com.hadley.receiptbackup.ui.screens.FullScreenImageScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditScreenWrapper (
    sheetState: SheetState,
    closeSheet: () -> Unit,
    viewModel: ReceiptItemViewModel
) {
    val localNavController = rememberNavController()
    val isViewingImage = remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = closeSheet,
        sheetState = sheetState
    ) {
        BackHandler(enabled = sheetState.isVisible, onBack = {
            if (isViewingImage.value) {
                localNavController.popBackStack()
            }
        })

        NavHost(
            navController = localNavController,
            startDestination = "receipt"
        ) {
            composable("receipt") {
                isViewingImage.value = false
                AddEditItemScreen(
                    navController = localNavController,
                    viewModel = viewModel,
                    onFinish = closeSheet
                )
            }
            composable(
                route = "image?uri={uri}",
                arguments = listOf(navArgument("uri") { type = NavType.StringType })
            ) { backStackEntry ->
                isViewingImage.value = true
                val uri = backStackEntry.arguments?.getString("uri") ?: ""
                FullScreenImageScreen(localNavController, uri)
            }
        }
    }
}