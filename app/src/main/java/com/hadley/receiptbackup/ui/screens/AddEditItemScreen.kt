package com.hadley.receiptbackup.ui.screens

import android.app.DatePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.navigation.NavController
import com.hadley.receiptbackup.data.model.ReceiptItem
import com.hadley.receiptbackup.data.repository.ReceiptItemViewModel
import com.hadley.receiptbackup.ui.components.ReceiptImage
import com.hadley.receiptbackup.ui.components.StoreDropdownField
import com.hadley.receiptbackup.utils.submitReceipt
import java.text.DecimalFormat
import java.time.LocalDate
import java.util.*

@Composable
fun AddEditItemScreen(
    navController: NavController,
    viewModel: ReceiptItemViewModel,
    existingItem: ReceiptItem? = null
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val formatter = DecimalFormat("0.00")

    var name by remember { mutableStateOf(existingItem?.name ?: "") }
    var store by remember { mutableStateOf(existingItem?.store ?: "") }
    var price by remember {
        mutableStateOf(
            if (existingItem?.price != null) formatter.format(existingItem.price)
            else ""
        )
    }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    val date = remember { mutableStateOf(existingItem?.date ?: LocalDate.now()) }
    val items by viewModel.items.collectAsState()
    val allStores = remember(items) {
        items.map { it.store }.distinct().sorted()
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> imageUri = uri }

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


    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ReceiptImage(navController, imageUrl = imageUri?.toString() ?: existingItem?.imageUrl)

            Button(onClick = { imagePickerLauncher.launch("image/*") }, enabled = !isUploading) {
                Text("Choose Image")
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Item Name") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                enabled = !isUploading
            )

            StoreDropdownField(
                store = store,
                onStoreChange = { store = it },
                allStores = allStores,
                enabled = !isUploading
            )

            OutlinedTextField(
                value = price,
                onValueChange = {
                    val clean = it.filter { c -> c.isDigit() || c == '.' }
                    if (clean.count { ch -> ch == '.' } <= 1) price = clean
                },
                label = { Text("Price") },
                leadingIcon = { Text("$") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                singleLine = true,
                enabled = !isUploading
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Date: ${date.value}")
                Button(onClick = {
                    datePickerDialog.show()
                    focusManager.clearFocus()
                    }, enabled = !isUploading) {
                    Text("Pick Date")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { submitReceipt(
                    context = context,
                    navController = navController,
                    viewModel = viewModel,
                    existingItem = existingItem,
                    name = name,
                    store = store,
                    price = price,
                    date = date.value,
                    imageUri = imageUri,
                    setIsUploading = { isUploading = it }
                )},
                modifier = Modifier.align(Alignment.End),
                enabled = !isUploading
            ) {
                Text(if (isUploading) "Saving..." else if (existingItem != null) "Update" else "Save")
            }
        }
    }
}
