package com.hadley.receiptbackup.data.sync

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.hadley.receiptbackup.data.local.LocalDateAdapter
import com.hadley.receiptbackup.data.local.ReceiptItemDataStore
import com.hadley.receiptbackup.data.model.ReceiptItem
import com.hadley.receiptbackup.data.model.toMap
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import coil.Coil
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.annotation.ExperimentalCoilApi
import com.google.firebase.storage.UploadTask
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.io.File

class ReceiptSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    @OptIn(ExperimentalCoilApi::class)
    override suspend fun doWork(): Result {
        val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val payloadJson = inputData.getString(KEY_PAYLOAD) ?: return Result.failure()
        val payload = payloadAdapter.fromJson(payloadJson) ?: return Result.failure()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.retry()

        return try {
            val storage = Firebase.storage
            var imageUrl = payload.remoteImageUrl

            if (!payload.localImageUri.isNullOrBlank()) {
                val imageRef = storage.reference
                    .child("users/$uid/images/${UUID.randomUUID()}.jpg")

                val bytes = compressImage(applicationContext, Uri.parse(payload.localImageUri))
                val uploadTask = imageRef.putBytes(bytes)
                var lastPercent = 0
                uploadTask.addOnProgressListener { snapshot ->
                    val rawPercent = ((snapshot.bytesTransferred * 100) / snapshot.totalByteCount).toInt()
                    if (rawPercent >= lastPercent + 5 || rawPercent == 100) {
                        val percent = (rawPercent / 5) * 5
                        if (percent > lastPercent) {
                            lastPercent = percent
                        } else if (rawPercent == 100 && lastPercent < 100) {
                            lastPercent = 100
                        } else {
                            return@addOnProgressListener
                        }
                        workerScope.launch {
                            updateUploadProgress(payload.id, lastPercent)
                        }
                    }
                }
                awaitUpload(uploadTask)
                imageUrl = Tasks.await(imageRef.downloadUrl).toString()

                payload.previousImageUrl?.let { previousUrl ->
                    try {
                        val oldImageRef = storage.getReferenceFromUrl(previousUrl)
                        Tasks.await(oldImageRef.delete())
                    } catch (e: IllegalArgumentException) {
                        Log.e(TAG, "Invalid previous image URL: $previousUrl", e)
                    }
                }
            }

            if (!imageUrl.isNullOrBlank()) {
                val imageLoader = Coil.imageLoader(applicationContext)
                val request = ImageRequest.Builder(applicationContext)
                    .data(imageUrl)
                    .diskCacheKey(imageUrl)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
                imageLoader.execute(request)
            }

            deleteLocalFile(payload.localImageUri)

            val receipt = ReceiptItem(
                id = payload.id,
                name = payload.name,
                store = payload.store,
                date = payload.date,
                price = payload.price,
                imageUrl = imageUrl,
                localImageUri = null,
                pendingUpload = false,
                uploadProgress = null
            )

            val db = Firebase.firestore
            Tasks.await(
                db.collection("users")
                    .document(uid)
                    .collection("receipts")
                    .document(payload.id)
                    .set(receipt.toMap())
            )

            updateCachedReceipt(receipt)

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Receipt sync failed", e)
            Result.retry()
        } finally {
            workerScope.cancel()
        }
    }

    private fun compressImage(context: Context, uri: Uri): ByteArray {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(source)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        return outputStream.toByteArray()
    }

    private suspend fun awaitUpload(task: UploadTask) {
        suspendCancellableCoroutine<Unit> { cont ->
            task.addOnSuccessListener { cont.resume(Unit) }
            task.addOnFailureListener { cont.resumeWithException(it) }
            cont.invokeOnCancellation { task.cancel() }
        }
    }

    private suspend fun updateUploadProgress(id: String, percent: Int) {
        val cachedItems = ReceiptItemDataStore.getReceipts(applicationContext).first()
        val updatedItems = cachedItems.map { cached ->
            if (cached.id == id) cached.copy(uploadProgress = percent, pendingUpload = true) else cached
        }
        ReceiptItemDataStore.saveReceipts(applicationContext, updatedItems)
    }

    private suspend fun updateCachedReceipt(receipt: ReceiptItem) {
        val cachedItems = ReceiptItemDataStore.getReceipts(applicationContext).first()
        val updatedItems = cachedItems.map { cached ->
            if (cached.id == receipt.id) receipt else cached
        }
        ReceiptItemDataStore.saveReceipts(applicationContext, updatedItems)
    }

    private fun deleteLocalFile(uriString: String?) {
        if (uriString.isNullOrBlank()) return
        val uri = Uri.parse(uriString)
        if (uri.scheme != "file") return
        runCatching { File(uri.path ?: return).delete() }
    }

    data class Payload(
        val id: String,
        val name: String,
        val store: String,
        val date: java.time.LocalDate,
        val price: Double,
        val localImageUri: String?,
        val remoteImageUrl: String?,
        val previousImageUrl: String?
    )

    companion object {
        private const val TAG = "ReceiptSyncWorker"
        private const val KEY_PAYLOAD = "payload"

        private val moshi = Moshi.Builder()
            .add(LocalDateAdapter())
            .add(KotlinJsonAdapterFactory())
            .build()
        private val payloadAdapter = moshi.adapter(Payload::class.java)

        fun createInputData(payload: Payload): Data {
            val json = payloadAdapter.toJson(payload)
            return Data.Builder()
                .putString(KEY_PAYLOAD, json)
                .build()
        }
    }
}
