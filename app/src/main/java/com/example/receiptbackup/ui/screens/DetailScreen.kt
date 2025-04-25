package com.example.receiptbackup.ui.screens

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.receiptbackup.data.repository.ReceiptItemViewModel
import java.text.DecimalFormat

@Composable
fun DetailScreen(navController: NavController, itemId: String, viewModel: ReceiptItemViewModel) {
    val item = viewModel.getItemById(itemId)
    var showConfirmDialog by remember { mutableStateOf(false) }

    val formatter = DecimalFormat("0.00")

    if (item == null) {
        Text("Item not found", modifier = Modifier.padding(16.dp))
        return
    }

    Scaffold(
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                FloatingActionButton(onClick = {
                    navController.navigate("edit/${item.id}")
                }) {
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
                .padding(16.dp)
        ) {
            if (!item.imageUrl.isNullOrEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(item.imageUrl),
                    contentDescription = "Receipt Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clickable {
                            navController.navigate("image?uri=${Uri.encode(item.imageUrl)}")
                        },
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(text = item.name, style = MaterialTheme.typography.headlineSmall)
            Text(text = "Store: ${item.store}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Date: ${item.date}", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "Price: $${formatter.format(item.price)}",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Delete Receipt?") },
            text = { Text("Are you sure you want to delete this receipt? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteItem(item.id)
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
