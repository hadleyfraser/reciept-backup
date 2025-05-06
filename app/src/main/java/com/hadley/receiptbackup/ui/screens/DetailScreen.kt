package com.hadley.receiptbackup.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hadley.receiptbackup.data.repository.ReceiptItemViewModel
import com.hadley.receiptbackup.ui.components.LabelValueText
import com.hadley.receiptbackup.ui.components.ReceiptImage
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(navController: NavController, itemId: String, viewModel: ReceiptItemViewModel) {
    val context = LocalContext.current
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

    Scaffold(
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
            ReceiptImage(navController, item.imageUrl)
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
