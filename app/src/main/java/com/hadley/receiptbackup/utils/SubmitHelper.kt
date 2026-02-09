package com.hadley.receiptbackup.utils

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.hadley.receiptbackup.data.model.ReceiptItem
import com.hadley.receiptbackup.data.repository.ReceiptItemViewModel
import com.hadley.receiptbackup.data.sync.ReceiptSyncWorker
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.time.LocalDate
import java.util.*

fun submitReceipt(
    context: Context,
    viewModel: ReceiptItemViewModel,
    existingItem: ReceiptItem?,
    name: String,
    store: String,
    price: String,
    date: LocalDate,
    imageUri: Uri?,
    setIsUploading: (Boolean) -> Unit,
    onFinish: () -> Unit
) {
    val formatter = DecimalFormat("0.00")
    val parsedPrice = price.toDoubleOrNull()

    if (name.isBlank() || store.isBlank() || parsedPrice == null) {
        Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
        return
    }

    setIsUploading(true)

    val itemId = existingItem?.id ?: UUID.randomUUID().toString()
    val newLocalImageUri = imageUri?.let { persistLocalImage(context, it) }
    val effectiveLocalImageUri = newLocalImageUri ?: existingItem?.localImageUri

    val onComplete: (String?) -> Unit = { _ ->
        val formattedPrice = formatter.format(parsedPrice).toDouble()
        val item = ReceiptItem(
            id = itemId,
            name = name,
            store = store,
            date = date,
            price = formattedPrice,
            imageUrl = existingItem?.imageUrl,
            localImageUri = effectiveLocalImageUri,
            pendingUpload = true,
            uploadProgress = null
        )

        if (existingItem == null) viewModel.addItem(context, item)
        else viewModel.updateItem(context, item)

        enqueueReceiptSync(
            context = context,
            item = item,
            newLocalImageUri = newLocalImageUri,
            previousImageUrl = if (newLocalImageUri != null) existingItem?.imageUrl else null
        )

        setIsUploading(false)
        onFinish()
    }

    onComplete(null)
}

private fun enqueueReceiptSync(
    context: Context,
    item: ReceiptItem,
    newLocalImageUri: String?,
    previousImageUrl: String?
) {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    val payload = ReceiptSyncWorker.Payload(
        id = item.id,
        name = item.name,
        store = item.store,
        date = item.date,
        price = item.price,
        localImageUri = newLocalImageUri,
        remoteImageUrl = item.imageUrl,
        previousImageUrl = previousImageUrl
    )

    val request = OneTimeWorkRequestBuilder<ReceiptSyncWorker>()
        .setConstraints(constraints)
        .setInputData(ReceiptSyncWorker.createInputData(payload))
        .build()

    WorkManager.getInstance(context)
        .enqueueUniqueWork("receipt-sync-${item.id}", ExistingWorkPolicy.REPLACE, request)
}

private fun persistLocalImage(context: Context, uri: Uri): String? {
    return try {
        val fileName = "receipt_${UUID.randomUUID()}.jpg"
        val imagesDir = File(context.filesDir, "receipt_images").apply { mkdirs() }
        val destFile = File(imagesDir, fileName)

        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        } ?: return null

        Uri.fromFile(destFile).toString()
    } catch (e: Exception) {
        null
    }
}
