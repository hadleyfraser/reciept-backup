package com.hadley.receiptbackup.data.repository

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hadley.receiptbackup.data.local.ReceiptItemDataStore
import com.hadley.receiptbackup.data.model.ReceiptItem
import com.hadley.receiptbackup.data.model.toMap
import com.hadley.receiptbackup.data.model.toReceiptItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReceiptItemViewModel : ViewModel() {
    private val _items = MutableStateFlow<List<ReceiptItem>>(emptyList())
    val items: StateFlow<List<ReceiptItem>> = _items

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    fun loadCachedReceipts(context: Context) {
        viewModelScope.launch {
            try {
                _items.value = ReceiptItemDataStore.getReceipts(context).first()
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
                _items.value = loadedItems

                viewModelScope.launch {
                    ReceiptItemDataStore.saveReceipts(context, loadedItems)
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

    fun addItem(item: ReceiptItem) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        Firebase.firestore.collection("users")
            .document(uid)
            .collection("receipts")
            .add(item.toMap())
            .addOnSuccessListener { docRef ->
                val withId = item.copy(id = docRef.id)
                _items.value += withId
            }
    }

    fun updateItem(context: Context, updated: ReceiptItem) {
        val previous = _items.value.find { it.id == updated.id }
        _items.value = _items.value.map {
            if (it.id == updated.id) updated else it
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = Firebase.firestore

        db.collection("users")
            .document(uid)
            .collection("receipts")
            .document(updated.id)
            .set(updated.toMap())
            .addOnSuccessListener {
                val storage = Firebase.storage
                if (previous?.imageUrl != null && previous.imageUrl != updated.imageUrl && !updated.imageUrl.isNullOrEmpty()) {
                    try {
                        val oldImageRef = storage.getReferenceFromUrl(previous.imageUrl!!)
                        oldImageRef.delete()
                    } catch (e: IllegalArgumentException) {
                        Log.e("ReceiptItemViewModel", "Invalid previous image URL: \${previous.imageUrl}", e)
                    }
                }
                viewModelScope.launch {
                    ReceiptItemDataStore.saveReceipts(context, _items.value)
                }
            }
            .addOnFailureListener { e ->
                Log.e("ReceiptItemViewModel", "Failed to update Firestore", e)
            }
    }

    fun deleteItem(itemId: String) {
        val item = _items.value.find { it.id == itemId }

        _items.value = _items.value.filterNot { it.id == itemId }

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

    fun getItemById(id: String): ReceiptItem? {
        return _items.value.find { it.id == id }
    }

    fun updateSearchQuery(newQuery: String) {
        _searchQuery.value = newQuery
    }
}
