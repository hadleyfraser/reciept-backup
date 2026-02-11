package com.hadley.receiptbackup.data.repository

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hadley.receiptbackup.data.local.LoyaltyCardDataStore
import com.hadley.receiptbackup.data.model.LoyaltyCard
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LoyaltyCardViewModel : ViewModel() {
    private val _cards = MutableStateFlow<List<LoyaltyCard>>(emptyList())
    val cards: StateFlow<List<LoyaltyCard>> = _cards

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    fun loadCards(context: Context) {
        viewModelScope.launch {
            LoyaltyCardDataStore.getCards(context).collectLatest { cached ->
                _cards.value = cached
            }
        }
        loadCardsFromFirestore(context)
    }

    fun addCard(context: Context, card: LoyaltyCard) {
        _cards.value = _cards.value + card
        persist(context)
        saveCardToFirestore(card)
    }

    fun updateCard(context: Context, updated: LoyaltyCard) {
        _cards.value = _cards.value.map { if (it.id == updated.id) updated else it }
        persist(context)
        saveCardToFirestore(updated)
    }

    fun deleteCard(context: Context, cardId: String) {
        val index = _cards.value.indexOfFirst { it.id == cardId }
        if (index == -1) return
        _cards.value = _cards.value.filterNot { it.id == cardId }
        persist(context)
        deleteCardFromFirestore(cardId)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun getCardById(id: String): LoyaltyCard? {
        return _cards.value.find { it.id == id }
    }

    fun clearCards() {
        _cards.value = emptyList()
    }

    suspend fun clearLocalCache(context: Context) {
        LoyaltyCardDataStore.clearCards(context)
    }

    private fun persist(context: Context) {
        viewModelScope.launch {
            LoyaltyCardDataStore.saveCards(context, _cards.value)
        }
    }

    private fun loadCardsFromFirestore(context: Context) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val firestore = Firebase.firestore

        firestore.collection("users")
            .document(uid)
            .collection("cards")
            .get()
            .addOnSuccessListener { snapshot ->
                val loadedCards = snapshot.documents.map { doc ->
                    val data = doc.data ?: emptyMap()
                    mapToLoyaltyCard(data, doc.id)
                }
                _cards.value = loadedCards
                viewModelScope.launch {
                    LoyaltyCardDataStore.saveCards(context, loadedCards)
                }
            }
            .addOnFailureListener { e ->
                Log.e("LoyaltyCardViewModel", "Failed to load cards from Firestore", e)
            }
    }

    private fun saveCardToFirestore(card: LoyaltyCard) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val firestore = Firebase.firestore

        firestore.collection("users")
            .document(uid)
            .collection("cards")
            .document(card.id)
            .set(cardToFirestoreMap(card))
            .addOnFailureListener { e ->
                Log.e("LoyaltyCardViewModel", "Failed to save card ${card.id}", e)
            }
    }

    private fun cardToFirestoreMap(card: LoyaltyCard): Map<String, Any?> {
        return mapOf(
            "name" to card.name,
            "notes" to card.notes,
            "barcodeType" to card.barcodeType,
            "barcodeValue" to card.barcodeValue,
            "coverColor" to card.coverColor,
            "createdAt" to card.createdAt
        )
    }

    private fun mapToLoyaltyCard(data: Map<String, Any?>, id: String): LoyaltyCard {
        val name = data["name"] as? String ?: ""
        val notes = data["notes"] as? String ?: ""
        val barcodeType = data["barcodeType"] as? String ?: ""
        val barcodeValue = data["barcodeValue"] as? String ?: ""
        val coverColor = when (val value = data["coverColor"]) {
            is Long -> value.toInt()
            is Int -> value
            is Double -> value.toInt()
            else -> 0
        }
        val createdAt = when (val value = data["createdAt"]) {
            is Long -> value
            is Int -> value.toLong()
            is Double -> value.toLong()
            else -> System.currentTimeMillis()
        }

        return LoyaltyCard(
            id = id,
            name = name,
            notes = notes,
            barcodeType = barcodeType,
            barcodeValue = barcodeValue,
            coverColor = coverColor,
            createdAt = createdAt
        )
    }

    private fun deleteCardFromFirestore(cardId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val firestore = Firebase.firestore

        firestore.collection("users")
            .document(uid)
            .collection("cards")
            .document(cardId)
            .delete()
            .addOnFailureListener { e ->
                Log.e("LoyaltyCardViewModel", "Failed to delete card $cardId", e)
            }
    }
}
