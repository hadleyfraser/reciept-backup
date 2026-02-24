package com.hadley.receiptbackup.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

object LoyaltyCardImageManager {

    private const val MAX_HEIGHT_PX = 256
    private const val STORAGE_DIR = "loyalty_card_images"

    fun localCacheFile(context: Context, cardId: String): File {
        val dir = File(context.filesDir, STORAGE_DIR).apply { mkdirs() }
        return File(dir, "$cardId.jpg")
    }

    suspend fun uploadFromUri(context: Context, cardId: String, uri: Uri): String {
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)
        } ?: throw IllegalArgumentException("Could not read image from URI")
        return uploadBitmap(context, cardId, bitmap)
    }

    suspend fun uploadFromUrl(context: Context, cardId: String, url: String): String {
        val connection = java.net.URL(url).openConnection().apply {
            connectTimeout = 10_000
            readTimeout = 10_000
        }
        val bitmap = connection.getInputStream().use { BitmapFactory.decodeStream(it) }
            ?: throw IllegalArgumentException("Could not download image from URL")
        return uploadBitmap(context, cardId, bitmap)
    }

    private suspend fun uploadBitmap(context: Context, cardId: String, original: Bitmap): String {
        val resized = resize(original)
        val cacheFile = localCacheFile(context, cardId)
        FileOutputStream(cacheFile).use { out ->
            resized.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("Not signed in")
        val storageRef = FirebaseStorage.getInstance()
            .reference.child("$STORAGE_DIR/$uid/$cardId.jpg")
        return suspendCancellableCoroutine { cont ->
            storageRef.putBytes(cacheFile.readBytes())
                .continueWithTask { task ->
                    if (!task.isSuccessful) throw task.exception!!
                    storageRef.downloadUrl
                }
                .addOnSuccessListener { uri -> cont.resume(uri.toString()) }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }
    }

    suspend fun downloadToCache(context: Context, cardId: String, url: String) {
        val cacheFile = localCacheFile(context, cardId)
        if (cacheFile.exists()) return
        suspendCancellableCoroutine<Unit> { cont ->
            FirebaseStorage.getInstance()
                .getReferenceFromUrl(url)
                .getFile(cacheFile)
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }
    }

    fun deleteLocalCache(context: Context, cardId: String) {
        localCacheFile(context, cardId).delete()
    }

    suspend fun deleteFromStorage(cardId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        suspendCancellableCoroutine<Unit> { cont ->
            FirebaseStorage.getInstance()
                .reference.child("$STORAGE_DIR/$uid/$cardId.jpg")
                .delete()
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resume(Unit) }
        }
    }

    private fun resize(bitmap: Bitmap): Bitmap {
        if (bitmap.height <= MAX_HEIGHT_PX) return bitmap
        val scale = MAX_HEIGHT_PX.toFloat() / bitmap.height
        val newWidth = (bitmap.width * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, MAX_HEIGHT_PX, true)
    }
}
