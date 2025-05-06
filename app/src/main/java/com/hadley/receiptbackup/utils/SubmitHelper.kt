package com.hadley.receiptbackup.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.hadley.receiptbackup.data.model.ReceiptItem
import com.hadley.receiptbackup.data.repository.ReceiptItemViewModel
import java.io.ByteArrayOutputStream
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

    val onComplete: (String?) -> Unit = { imageUrl ->
        val formattedPrice = formatter.format(parsedPrice).toDouble()
        val item = ReceiptItem(
            id = existingItem?.id ?: "",
            name = name,
            store = store,
            date = date,
            price = formattedPrice,
            imageUrl = imageUrl ?: existingItem?.imageUrl
        )

        if (existingItem == null) viewModel.addItem(context, item)
        else viewModel.updateItem(context, item)

        setIsUploading(false)
        onFinish()
    }

    if (imageUri != null) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(context, "User not signed in", Toast.LENGTH_SHORT).show()
            setIsUploading(false)
            return
        }

        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            if (inputStream == null) {
                Toast.makeText(context, "Cannot open selected image", Toast.LENGTH_SHORT).show()
                setIsUploading(false)
                return
            }
            inputStream.close()
        } catch (e: Exception) {
            Log.e("submitReceipt", "Image URI error", e)
            Toast.makeText(context, "Invalid image file", Toast.LENGTH_SHORT).show()
            setIsUploading(false)
            return
        }

        val storage = Firebase.storage
        val imageRef = storage.reference
            .child("users/$uid/images/${UUID.randomUUID()}.jpg")

        val source = ImageDecoder.createSource(context.contentResolver, imageUri)
        val bitmap = ImageDecoder.decodeBitmap(source)

        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        val compressedBytes = outputStream.toByteArray()

        imageRef.putBytes(compressedBytes)
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
                Log.e("submitReceipt", "Image upload failed", e)
                Toast.makeText(context, "Image upload failed", Toast.LENGTH_SHORT).show()
                onComplete(null)
            }
    } else {
        onComplete(null)
    }
}
