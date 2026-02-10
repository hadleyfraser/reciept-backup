package com.hadley.receiptbackup.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hadley.receiptbackup.data.model.LoyaltyCard
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.cardDataStore by preferencesDataStore(name = "loyalty_cards")

object LoyaltyCardDataStore {

    private val CARDS_KEY = stringPreferencesKey("cards")

    private val moshi = Moshi.Builder()
        .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
        .build()
    private val type = Types.newParameterizedType(List::class.java, LoyaltyCard::class.java)
    private val adapter = moshi.adapter<List<LoyaltyCard>>(type)

    suspend fun saveCards(context: Context, cards: List<LoyaltyCard>) {
        val json = adapter.toJson(cards)
        context.cardDataStore.edit { prefs ->
            prefs[CARDS_KEY] = json
        }
    }

    fun getCards(context: Context): Flow<List<LoyaltyCard>> {
        return context.cardDataStore.data.map { prefs ->
            val json = prefs[CARDS_KEY] ?: return@map emptyList()
            try {
                adapter.fromJson(json) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun clearCards(context: Context) {
        context.cardDataStore.edit { prefs ->
            prefs.remove(CARDS_KEY)
        }
    }
}

