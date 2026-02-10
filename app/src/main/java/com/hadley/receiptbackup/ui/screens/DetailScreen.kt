package com.hadley.receiptbackup.ui.screens

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hadley.receiptbackup.R
import com.hadley.receiptbackup.auth.GoogleAuthManager
import com.hadley.receiptbackup.data.repository.ReceiptItemViewModel
import com.hadley.receiptbackup.ui.components.AppDrawerScaffold
import com.hadley.receiptbackup.ui.components.LabelValueText
import com.hadley.receiptbackup.ui.components.ReceiptImage
import coil.Coil
import coil.annotation.ExperimentalCoilApi
import java.text.DecimalFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoilApi::class)
@Composable
fun DetailScreen(navController: NavController, itemId: String, viewModel: ReceiptItemViewModel) {
    val context = LocalContext.current
    val activity = LocalContext.current as Activity
    val coroutineScope = rememberCoroutineScope()
    val item = viewModel.getItemById(itemId)
    var showConfirmDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isSheetOpen by remember { mutableStateOf(false) }

    val formatter = DecimalFormat("0.00")

    if (item == null) {
        Text("Item not found", modifier = Modifier.padding(16.dp))
        return
    }

    if (isSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { isSheetOpen = false },
            sheetState = sheetState
        ) {
            AddEditItemScreen(
                navController = navController,
                viewModel = viewModel,
                existingItem = item,
                onFinish = { isSheetOpen = false }
            )
        }
    }

    AppDrawerScaffold(
        navController = navController,
        title = "Receipt",
        actions = {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "App Logo",
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(24.dp)
            )
        },
        drawerContent = { closeDrawer ->
            NavigationDrawerItem(
                label = { Text("Logout") },
                selected = false,
                onClick = {
                    closeDrawer()
                    coroutineScope.launch {
                        viewModel.clearItems()
                        GoogleAuthManager.signOut(activity, viewModel)
                        navController.navigate("landing") {
                            popUpTo("list") { inclusive = true }
                        }
                        launch(Dispatchers.IO) {
                            viewModel.clearLocalCache(activity)
                            viewModel.clearCachedImages(activity)

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
        },
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                FloatingActionButton(onClick = { isSheetOpen = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }

                FloatingActionButton(onClick = {
                    showConfirmDialog = true
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ReceiptImage(navController, item.imageUrl, item.localImageUri)
            Text(text = item.name, style = MaterialTheme.typography.headlineSmall)
            LabelValueText("Store", item.store)
            LabelValueText("Date", item.date.toString())
            LabelValueText("Price", formatter.format(item.price))
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Delete Receipt?") },
            text = { Text("Are you sure you want to delete this receipt? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteItem(context, item.id)
                    showConfirmDialog = false
                    navController.popBackStack()
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
