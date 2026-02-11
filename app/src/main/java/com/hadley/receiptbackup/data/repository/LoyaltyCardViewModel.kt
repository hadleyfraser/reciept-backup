package com.hadley.receiptbackup.data.repository

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hadley.receiptbackup.data.local.LoyaltyCardDataStore
import com.hadley.receiptbackup.data.model.LoyaltyCard
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
    }

    fun addCard(context: Context, card: LoyaltyCard) {
        _cards.value = _cards.value + card
        persist(context)
    }

    fun updateCard(context: Context, updated: LoyaltyCard) {
        _cards.value = _cards.value.map { if (it.id == updated.id) updated else it }
        persist(context)
    }

    fun deleteCard(context: Context, cardId: String): DeletedCard? {
        val index = _cards.value.indexOfFirst { it.id == cardId }
        if (index == -1) return null
        val card = _cards.value[index]
        _cards.value = _cards.value.filterNot { it.id == cardId }
        persist(context)
        return DeletedCard(card, index)
    }

    fun restoreCards(context: Context, deletedCards: List<DeletedCard>) {
        if (deletedCards.isEmpty()) return
        val current = _cards.value.toMutableList()
        deletedCards.sortedBy { it.index }.forEach { deleted ->
            val safeIndex = deleted.index.coerceIn(0, current.size)
            current.add(safeIndex, deleted.card)
        }
        _cards.value = current
        persist(context)
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
}

data class DeletedCard(
    val card: LoyaltyCard,
    val index: Int
)
