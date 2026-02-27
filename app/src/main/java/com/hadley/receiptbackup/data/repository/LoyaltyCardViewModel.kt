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
import com.hadley.receiptbackup.utils.LoyaltyCardImageManager
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
                applyLoadedCards(context, cached, persistIfChanged = false)
            }
        }
        loadCardsFromFirestore(context)
    }

    fun addCard(context: Context, card: LoyaltyCard) {
        val orderedCard = card.copy(sortOrder = _cards.value.size)
        _cards.value = _cards.value + orderedCard
        persist(context)
        saveCardToFirestore(orderedCard)
    }

    fun moveCard(context: Context, fromId: String, toId: String) {
        val current = _cards.value.toMutableList()
        val fromIndex = current.indexOfFirst { it.id == fromId }
        val toIndex = current.indexOfFirst { it.id == toId }
        if (fromIndex == -1 || toIndex == -1 || fromIndex == toIndex) return
        val moved = current.removeAt(fromIndex)
        current.add(toIndex, moved)
        val normalized = current.mapIndexed { index, card ->
            if (card.sortOrder == index) card else card.copy(sortOrder = index)
        }
        _cards.value = normalized
        persist(context)
        normalized.forEach { saveCardToFirestore(it) }
    }

    fun updateCard(context: Context, updated: LoyaltyCard) {
        _cards.value = _cards.value.map { if (it.id == updated.id) updated else it }
        persist(context)
        saveCardToFirestore(updated)
    }

    fun deleteCard(context: Context, cardId: String) {
        val index = _cards.value.indexOfFirst { it.id == cardId }
        if (index == -1) return
        val card = _cards.value[index]
        _cards.value = _cards.value.filterNot { it.id == cardId }
        persist(context)
        deleteCardFromFirestore(cardId)
        if (card.cardImageUrl != null) {
            viewModelScope.launch {
                LoyaltyCardImageManager.deleteLocalCache(context, cardId)
                LoyaltyCardImageManager.deleteFromStorage(cardId)
            }
        }
    }

    fun syncCardImages(context: Context) {
        viewModelScope.launch {
            _cards.value.forEach { card ->
                val url = card.cardImageUrl ?: return@forEach
                try {
                    LoyaltyCardImageManager.downloadToCache(context, card.id, url)
                } catch (e: Exception) {
                    Log.w("LoyaltyCardViewModel", "Failed to cache image for card ${card.id}", e)
                }
            }
        }
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
                applyLoadedCards(context, loadedCards, persistIfChanged = true)
                syncCardImages(context)
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
            "imageOnly" to card.imageOnly,
            "cardImageUrl" to card.cardImageUrl,
            "sortOrder" to card.sortOrder,
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
        val sortOrder = when (val value = data["sortOrder"]) {
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
        val imageOnly = data["imageOnly"] as? Boolean ?: false
        val cardImageUrl = data["cardImageUrl"] as? String

        return LoyaltyCard(
            id = id,
            name = name,
            notes = notes,
            barcodeType = barcodeType,
            barcodeValue = barcodeValue,
            coverColor = coverColor,
            imageOnly = imageOnly,
            cardImageUrl = cardImageUrl,
            sortOrder = sortOrder,
            createdAt = createdAt
        )
    }

    private fun applyLoadedCards(
        context: Context,
        loadedCards: List<LoyaltyCard>,
        persistIfChanged: Boolean
    ) {
        val normalized = normalizeSortOrder(loadedCards)
        _cards.value = normalized
        if (persistIfChanged && normalized != loadedCards) {
            persist(context)
            normalized.forEach { saveCardToFirestore(it) }
        }
    }

    private fun normalizeSortOrder(cards: List<LoyaltyCard>): List<LoyaltyCard> {
        if (cards.isEmpty()) return cards
        val hasExplicitOrder = cards.any { it.sortOrder != 0 }
        val ordered = if (hasExplicitOrder) {
            cards.sortedBy { it.sortOrder }
        } else {
            cards
        }
        return ordered.mapIndexed { index, card ->
            if (card.sortOrder == index) card else card.copy(sortOrder = index)
        }
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
