package com.hadley.receiptbackup.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.util.*

/**
 * Creates an intent that allows the user to choose between taking a photo or picking from the gallery.
 * Returns the chooser intent and the URI to use for the captured image.
 */
fun createImagePickerIntent(context: Context): Pair<Intent, Uri> {
    val imageFile = File.createTempFile("temp_image_${UUID.randomUUID()}", ".jpg", context.cacheDir)
    val imageUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", imageFile)

    val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
        putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }

    val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
        type = "image/*"
    }

    val chooser = Intent.createChooser(pickIntent, "Select or Take Photo").apply {
        putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(captureIntent))
    }

    return Pair(chooser, imageUri)
}
