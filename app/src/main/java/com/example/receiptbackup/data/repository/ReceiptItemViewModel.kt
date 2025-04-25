package com.example.receiptbackup.data.repository

import androidx.lifecycle.ViewModel
import com.example.receiptbackup.data.model.ReceiptItem
import com.example.receiptbackup.data.model.toMap
import com.example.receiptbackup.data.model.toReceiptItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ReceiptItemViewModel : ViewModel() {
    private val _items = MutableStateFlow<List<ReceiptItem>>(emptyList())
    val items: StateFlow<List<ReceiptItem>> = _items

    init {
        loadReceiptsFromFirestore()
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

    fun updateItem(updated: ReceiptItem) {
        _items.value = _items.value.map {
            if (it.id == updated.id) updated else it
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        Firebase.firestore.collection("users")
            .document(uid)
            .collection("receipts")
            .document(updated.id)
            .set(updated.toMap())
    }

    fun deleteItem(itemId: String) {
        _items.value = _items.value.filterNot { it.id == itemId }

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        Firebase.firestore.collection("users")
            .document(uid)
            .collection("receipts")
            .document(itemId)
            .delete()
    }

    fun getItemById(id: String): ReceiptItem? {
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
                val loadedItems = snapshot.documents.map { doc ->
                    val data = doc.data ?: emptyMap()
                    data.toReceiptItem(doc.id)
                }
                _items.value = loadedItems
            }
            .addOnFailureListener { e ->
                // Optional: Handle error, show Snackbar, etc.
                e.printStackTrace()
            }
    }
}
