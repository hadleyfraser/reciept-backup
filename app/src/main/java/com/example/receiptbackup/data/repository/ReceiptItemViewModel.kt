package com.example.receiptbackup.data.repository

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.receiptbackup.data.model.ReceiptItem
import com.example.receiptbackup.data.model.toMap
import com.example.receiptbackup.data.model.toReceiptItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger

class ReceiptItemViewModel : ViewModel() {
    private val _items = MutableStateFlow<List<ReceiptItem>>(emptyList())
    val items: StateFlow<List<ReceiptItem>> = _items

    private val idCounter = AtomicInteger(1)

    init {
        loadReceiptsFromFirestore()
    }

    fun addItem(item: ReceiptItem) {
        Log.d("ReceiptItemViewModel", "addItem")
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        Log.d("ReceiptItemViewModel", "Current user: $uid")
        val firestore = Firebase.firestore

        firestore.collection("users")
            .document(uid)
            .collection("receipts")
            .add(item.toMap())

        // Update local list immediately (with generated local ID)
        val withId = item.copy(id = idCounter.getAndIncrement())
        _items.value = _items.value + withId
    }

    fun updateItem(updated: ReceiptItem) {
        _items.value = _items.value.map {
            if (it.id == updated.id) updated else it
        }
        // You could also update Firestore here if you store Firestore document IDs
    }

    fun deleteItem(itemId: Int) {
        _items.value = _items.value.filterNot { it.id == itemId }
        // You could also delete from Firestore here if storing Firestore doc IDs
    }

    fun getItemById(id: Int): ReceiptItem? {
        return _items.value.find { it.id == id }
    }

    private fun loadReceiptsFromFirestore() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val firestore = Firebase.firestore

        firestore.collection("users")
            .document(uid)
            .collection("receipts")
            .get()
            .addOnSuccessListener { snapshot ->
                val loadedItems = snapshot.documents.mapIndexed { index, doc ->
                    val data = doc.data ?: emptyMap()
                    data.toReceiptItem(index + 1)
                }
                _items.value = loadedItems
                idCounter.set(loadedItems.maxOfOrNull { it.id }?.plus(1) ?: 1)
            }
            .addOnFailureListener { e ->
                // Optional: Handle error, show Snackbar, etc.
                e.printStackTrace()
            }
    }
}
