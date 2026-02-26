package com.hadley.receiptbackup.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.Coil
import coil.annotation.ExperimentalCoilApi
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.hadley.receiptbackup.data.local.ReceiptItemDataStore
import com.hadley.receiptbackup.data.model.ReceiptItem
import com.hadley.receiptbackup.data.model.toReceiptItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import androidx.work.WorkManager
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import com.hadley.receiptbackup.data.sync.ReceiptSyncWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Request

@OptIn(ExperimentalCoilApi::class)
class ReceiptItemViewModel : ViewModel() {
    private val _items = MutableStateFlow<List<ReceiptItem>>(emptyList())
    val items: StateFlow<List<ReceiptItem>> = _items

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedStore = MutableStateFlow("All")
    val selectedStore: StateFlow<String> = _selectedStore

    private val _imageCacheStatus = MutableStateFlow<Map<String, ImageCacheStatus>>(emptyMap())
    val imageCacheStatus: StateFlow<Map<String, ImageCacheStatus>> = _imageCacheStatus

    private val _imageDownloadProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val imageDownloadProgress: StateFlow<Map<String, Int>> = _imageDownloadProgress

    private var didResetPendingProgress = false

    private val okHttpClient = OkHttpClient()
    private val localUriMutex = Mutex()

    fun loadCachedReceipts(context: Context) {
        viewModelScope.launch {
            try {
                ReceiptItemDataStore.getReceipts(context).collectLatest { cached ->
                    _items.value = cached
                    hydrateCacheStatus(context, cached)
                    if (!didResetPendingProgress) {
                        val resetItems = cached.map { item ->
                            if (item.pendingUpload && !item.localImageUri.isNullOrBlank()) {
                                item.copy(uploadProgress = null)
                            } else {
                                item
                            }
                        }
                        if (resetItems != cached) {
                            ReceiptItemDataStore.saveReceipts(context, resetItems)
                        }
                        didResetPendingProgress = true
                        enqueuePendingUploads(context, resetItems)
                        return@collectLatest
                    }
                    enqueuePendingUploads(context, cached)
                }
            } catch (e: Exception) {
                Log.e("ReceiptItemViewModel", "Error loading cached receipts", e)
            }
        }
    }

    fun loadReceiptsFromFirestore(context: Context) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val firestore = Firebase.firestore

        _isLoading.value = true

        firestore.collection("users")
            .document(uid)
            .collection("receipts")
            .get()
            .addOnSuccessListener { snapshot ->
                val loadedItems = snapshot.documents.map { doc ->
                    val data = doc.data ?: emptyMap()
                    data.toReceiptItem(doc.id)
                }

                // localImageUri is local-only (not stored in Firestore), so preserve it
                // from the currently loaded state before overwriting with Firestore data
                val preserved = _items.value.associateBy { it.id }
                val mergedItems = loadedItems.map { item ->
                    val existing = preserved[item.id]
                    if (existing?.localImageUri != null) item.copy(localImageUri = existing.localImageUri)
                    else item
                }

                _items.value = mergedItems
                hydrateCacheStatus(context, mergedItems)
                prefetchReceiptImages(context, mergedItems)

                viewModelScope.launch {
                    ReceiptItemDataStore.saveReceipts(context, mergedItems)
                }
                _isLoading.value = false
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                _isLoading.value = false
            }
    }

    fun clearItems() {
        _items.value = emptyList()
    }

    suspend fun clearLocalCache(context: Context) {
        ReceiptItemDataStore.clearReceipts(context)
    }

    fun clearCachedImages(context: Context) {
        _imageCacheStatus.value = emptyMap()
        _imageDownloadProgress.value = emptyMap()
        val cacheDir = getImageCacheDir(context)
        if (cacheDir.exists()) {
            cacheDir.listFiles()?.forEach { it.delete() }
        }
    }

    fun addItem(context: Context, item: ReceiptItem) {
        _items.value += item
        viewModelScope.launch {
            ReceiptItemDataStore.saveReceipts(context, _items.value)
        }
    }

    fun updateItem(context: Context, updated: ReceiptItem) {
        _items.value = _items.value.map {
            if (it.id == updated.id) updated else it
        }
        viewModelScope.launch {
            ReceiptItemDataStore.saveReceipts(context, _items.value)
        }
    }

    fun deleteItem(context: Context, itemId: String) {
        val item = _items.value.find { it.id == itemId }

        _items.value = _items.value.filterNot { it.id == itemId }

        viewModelScope.launch {
            ReceiptItemDataStore.saveReceipts(context, _items.value)
        }

        WorkManager.getInstance(context).cancelUniqueWork("receipt-sync-$itemId")

        removeCachedImage(context, item?.imageUrl)
        removeCachedImage(context, item?.localImageUri)
        deleteLocalFile(item?.localImageUri)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = Firebase.firestore
        val storage = Firebase.storage

        db.collection("users")
            .document(uid)
            .collection("receipts")
            .document(itemId)
            .delete()

        item?.imageUrl?.let { url ->
            try {
                val imageRef = storage.getReferenceFromUrl(url)
                imageRef.delete()
            } catch (e: IllegalArgumentException) {
                Log.e("ReceiptItemViewModel", "Invalid image URL: $url", e)
            }
        }
    }

    fun retryMissingImageDownloads(context: Context) {
        prefetchReceiptImages(context, _items.value)
    }

    private fun prefetchReceiptImages(context: Context, items: List<ReceiptItem>) {
        val imageLoader = Coil.imageLoader(context)
        val semaphore = Semaphore(10)
        items.filter { !it.imageUrl.isNullOrBlank() }
            .distinctBy { it.id }
            .forEach { item ->
                val url = item.imageUrl ?: return@forEach
                val cachedOnDisk = isImageCached(imageLoader.diskCache, url)
                val localFileExists = !item.localImageUri.isNullOrBlank() && localFileExists(item.localImageUri)
                if (cachedOnDisk || localFileExists) {
                    setCacheStatus(item.id, ImageCacheStatus.CACHED)
                    return@forEach
                }
                if (_imageCacheStatus.value[item.id] == ImageCacheStatus.DOWNLOADING) {
                    return@forEach
                }

                setCacheStatus(item.id, ImageCacheStatus.DOWNLOADING)
                viewModelScope.launch(Dispatchers.IO) {
                    semaphore.withPermit {
                        var lastError: Exception? = null
                        for (attempt in 1..3) {
                            try {
                                val localUri = downloadImageToLocalCache(context, item.id, url)
                                updateLocalImageUri(context, item.id, localUri)
                                setCacheStatus(item.id, ImageCacheStatus.CACHED)
                                return@withPermit
                            } catch (e: Exception) {
                                lastError = e
                                if (attempt < 3) delay(1000L * attempt)
                            }
                        }
                        Log.e("ReceiptItemViewModel", "Failed to download image after 3 attempts for ${item.id}", lastError)
                        setCacheStatus(item.id, ImageCacheStatus.FAILED)
                        clearDownloadProgress(item.id)
                    }
                }
            }
    }

    private suspend fun downloadImageToLocalCache(context: Context, itemId: String, url: String): String {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder().url(url).build()
            val cacheFile = File(getImageCacheDir(context), "$itemId.jpg")

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Download failed with code ${response.code}")
                }
                val body = response.body ?: throw IOException("Empty response body")
                val totalBytes = body.contentLength()
                var downloadedBytes = 0L
                var lastPercent = 0

                body.byteStream().use { input ->
                    FileOutputStream(cacheFile).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloadedBytes += read

                            if (totalBytes > 0) {
                                val rawPercent = ((downloadedBytes * 100) / totalBytes).toInt()
                                val rounded = (rawPercent / 5) * 5
                                if (rounded >= lastPercent + 5 || rawPercent == 100) {
                                    lastPercent = if (rawPercent == 100) 100 else rounded
                                    setDownloadProgress(itemId, lastPercent)
                                }
                            }
                        }
                    }
                }
            }

            clearDownloadProgress(itemId)
            Uri.fromFile(cacheFile).toString()
        }
    }

    private suspend fun updateLocalImageUri(context: Context, itemId: String, localUri: String) {
        localUriMutex.withLock {
            val updated = _items.value.map { item ->
                if (item.id == itemId) item.copy(localImageUri = localUri) else item
            }
            _items.value = updated
            ReceiptItemDataStore.saveReceipts(context, updated)
        }
    }

    private fun getImageCacheDir(context: Context): File {
        val dir = File(context.cacheDir, "receipt-images")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun setDownloadProgress(id: String, percent: Int) {
        _imageDownloadProgress.value = _imageDownloadProgress.value.toMutableMap().apply {
            this[id] = percent
        }
    }

    private fun clearDownloadProgress(id: String) {
        if (_imageDownloadProgress.value.containsKey(id)) {
            _imageDownloadProgress.value = _imageDownloadProgress.value.toMutableMap().apply {
                remove(id)
            }
        }
    }

    private fun hydrateCacheStatus(context: Context, items: List<ReceiptItem>) {
        val imageLoader = Coil.imageLoader(context)
        items.forEach { item ->
            when {
                !item.localImageUri.isNullOrBlank() && localFileExists(item.localImageUri) -> {
                    setCacheStatus(item.id, ImageCacheStatus.CACHED)
                }
                !item.imageUrl.isNullOrBlank() -> {
                    val url = item.imageUrl
                    val cached = isImageCached(imageLoader.diskCache, url)
                    setCacheStatus(item.id, if (cached) ImageCacheStatus.CACHED else ImageCacheStatus.NOT_CACHED)
                }
                else -> {
                    setCacheStatus(item.id, ImageCacheStatus.NOT_CACHED)
                }
            }
        }
    }

    private fun isImageCached(diskCache: DiskCache?, key: String): Boolean {
        if (diskCache == null) return false
        return try {
            diskCache.openSnapshot(key)?.use { true } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun setCacheStatus(id: String, status: ImageCacheStatus) {
        _imageCacheStatus.value = _imageCacheStatus.value.toMutableMap().apply {
            this[id] = status
        }
    }

    fun getItemById(id: String): ReceiptItem? {
        return _items.value.find { it.id == id }
    }

    fun updateSearchQuery(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun updateselectedStore(newQuery: String) {
        _selectedStore.value = newQuery
    }

    private suspend fun enqueuePendingUploads(context: Context, items: List<ReceiptItem>) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val workManager = WorkManager.getInstance(context)

        val pendingItems = items.filter { it.pendingUpload && localFileExists(it.localImageUri) }
        if (pendingItems.isEmpty()) return

        pendingItems.forEach { item ->
            val payload = ReceiptSyncWorker.Payload(
                id = item.id,
                name = item.name,
                store = item.store,
                date = item.date,
                price = item.price,
                localImageUri = item.localImageUri,
                remoteImageUrl = item.imageUrl,
                previousImageUrl = null
            )

            val request = OneTimeWorkRequestBuilder<ReceiptSyncWorker>()
                .setConstraints(constraints)
                .setInputData(ReceiptSyncWorker.createInputData(payload))
                .build()

            workManager.enqueueUniqueWork(
                "receipt-sync-${item.id}",
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }

    private fun localFileExists(uriString: String?): Boolean {
        if (uriString.isNullOrBlank()) return false
        val uri = Uri.parse(uriString)
        if (uri.scheme != "file") return false
        return File(uri.path ?: return false).exists()
    }

    private fun deleteLocalFile(uriString: String?) {
        if (uriString.isNullOrBlank()) return
        val uri = Uri.parse(uriString)
        if (uri.scheme != "file") return
        runCatching { File(uri.path ?: return).delete() }
    }

    private fun removeCachedImage(context: Context, data: String?) {
        if (data.isNullOrBlank()) return
        val uri = Uri.parse(data)
        if (uri.scheme == "file") {
            deleteLocalFile(data)
            return
        }
        val imageLoader = Coil.imageLoader(context)
        imageLoader.diskCache?.remove(data)
        imageLoader.memoryCache?.remove(MemoryCache.Key(data))
    }
}

enum class ImageCacheStatus {
    DOWNLOADING,
    CACHED,
    FAILED,
    NOT_CACHED
}
