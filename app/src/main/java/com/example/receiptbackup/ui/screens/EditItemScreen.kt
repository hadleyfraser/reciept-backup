package com.example.receiptbackup.ui.screens

import android.app.DatePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.imePadding
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.receiptbackup.data.model.ReceiptItem
import com.example.receiptbackup.data.repository.ReceiptItemViewModel
import java.text.DecimalFormat
import java.time.LocalDate
import java.util.*

@Composable
fun EditItemScreen(navController: NavController, itemId: Int, viewModel: ReceiptItemViewModel) {
    val originalItem = viewModel.getItemById(itemId)
    if (originalItem == null) {
        Text("Item not found", modifier = Modifier.padding(16.dp))
        return
    }

    val formatter = remember { DecimalFormat("0.00") }

    var name by remember { mutableStateOf(originalItem.name) }
    var store by remember { mutableStateOf(originalItem.store) }
    var price by remember { mutableStateOf(formatter.format(originalItem.price)) }
    var imageUri by remember { mutableStateOf(originalItem.imageUrl?.let { Uri.parse(it) }) }
    val date = remember { mutableStateOf(originalItem.date) }

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    val calendar = Calendar.getInstance().apply {
        set(date.value.year, date.value.monthValue - 1, date.value.dayOfMonth)
    }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            date.value = LocalDate.of(year, month + 1, dayOfMonth)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (imageUri != null) {
            Image(
                painter = rememberAsyncImagePainter(imageUri),
                contentDescription = "Selected image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
        }

        Button(onClick = { imagePickerLauncher.launch("image/*") }) {
            Text("Change Image")
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Item Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = store,
            onValueChange = { store = it },
            label = { Text("Store") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = price,
            onValueChange = {
                val clean = it.filter { c -> c.isDigit() || c == '.' }
                if (clean.count { ch -> ch == '.' } <= 1) {
                    price = clean
                }
            },
            label = { Text("Price") },
            leadingIcon = { Text("$") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Date: ${date.value}")
            Button(onClick = { datePickerDialog.show() }) {
                Text("Pick Date")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val parsedPrice = price.toDoubleOrNull() ?: 0.0
                val formattedPrice = formatter.format(parsedPrice).toDouble()

                viewModel.updateItem(
                    originalItem.copy(
                        name = name,
                        store = store,
                        price = formattedPrice,
                        date = date.value,
                        imageUrl = imageUri?.toString()
                    )
                )
                navController.popBackStack()
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Save Changes")
        }
    }
}
