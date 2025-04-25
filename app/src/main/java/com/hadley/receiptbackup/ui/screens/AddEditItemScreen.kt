package com.hadley.receiptbackup.ui.screens

import android.app.DatePickerDialog
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import coil.compose.rememberAsyncImagePainter
import com.hadley.receiptbackup.data.model.ReceiptItem
import com.hadley.receiptbackup.data.repository.ReceiptItemViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
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
    val formatter = remember { DecimalFormat("0.00") }

    var name by remember { mutableStateOf(existingItem?.name ?: "") }
    var store by remember { mutableStateOf(existingItem?.store ?: "") }
    var price by remember { mutableStateOf(existingItem?.price?.toString() ?: "") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    val date = remember { mutableStateOf(existingItem?.date ?: LocalDate.now()) }

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

    fun submit() {
        val parsedPrice = price.toDoubleOrNull()
        if (name.isBlank() || store.isBlank() || parsedPrice == null) {
            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        isUploading = true

        val onComplete: (String?) -> Unit = { imageUrl ->
            val formattedPrice = formatter.format(parsedPrice).toDouble()
            val item = ReceiptItem(
                id = existingItem?.id ?: "",
                name = name,
                store = store,
                date = date.value,
                price = formattedPrice,
                imageUrl = imageUrl ?: existingItem?.imageUrl
            )

            if (existingItem == null) viewModel.addItem(item)
            else viewModel.updateItem(context, item)

            isUploading = false
            navController.popBackStack()
        }

        if (imageUri != null) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                Toast.makeText(context, "User not signed in", Toast.LENGTH_SHORT).show()
                isUploading = false
                return
            }

            try {
                val inputStream = context.contentResolver.openInputStream(imageUri!!)
                if (inputStream == null) {
                    Toast.makeText(context, "Cannot open selected image", Toast.LENGTH_SHORT).show()
                    isUploading = false
                    return
                }
                inputStream.close()
            } catch (e: Exception) {
                Log.e("AddEditItemScreen", "Image URI error", e)
                Toast.makeText(context, "Invalid image file", Toast.LENGTH_SHORT).show()
                isUploading = false
                return
            }

            val storage = Firebase.storage
            val imageRef = storage.reference
                .child("users/$uid/images/${UUID.randomUUID()}.jpg")

            imageRef.putFile(imageUri!!)
                .continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let { throw it }
                    }
                    imageRef.downloadUrl
                }
                .addOnSuccessListener { uri ->
                    onComplete(uri.toString())
                }
                .addOnFailureListener { e ->
                    Log.e("AddEditItemScreen", "Image upload failed", e)
                    Toast.makeText(context, "Image upload failed", Toast.LENGTH_SHORT).show()
                    onComplete(null)
                }
        } else {
            onComplete(null)
        }
    }

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
            if (imageUri != null || existingItem?.imageUrl != null) {
                val painter = rememberAsyncImagePainter(imageUri ?: existingItem?.imageUrl)
                Image(
                    painter = painter,
                    contentDescription = "Selected image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }

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

            OutlinedTextField(
                value = store,
                onValueChange = { store = it },
                label = { Text("Store") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
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
                Button(onClick = { datePickerDialog.show() }, enabled = !isUploading) {
                    Text("Pick Date")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { submit() },
                modifier = Modifier.align(Alignment.End),
                enabled = !isUploading
            ) {
                Text(if (isUploading) "Saving..." else if (existingItem != null) "Update" else "Save")
            }
        }
    }
}
