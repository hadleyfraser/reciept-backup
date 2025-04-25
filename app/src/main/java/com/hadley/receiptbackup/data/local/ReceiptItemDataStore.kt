package com.hadley.receiptbackup.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hadley.receiptbackup.data.model.ReceiptItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "receipt_items")

object ReceiptItemDataStore {

    private val RECEIPTS_KEY = stringPreferencesKey("receipts")

    private val moshi = Moshi.Builder()
        .add(LocalDateAdapter())
        .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
        .build()
    private val type = Types.newParameterizedType(List::class.java, ReceiptItem::class.java)
    private val adapter = moshi.adapter<List<ReceiptItem>>(type)

    suspend fun saveReceipts(context: Context, items: List<ReceiptItem>) {
        val json = adapter.toJson(items)
        context.dataStore.edit { prefs ->
            prefs[RECEIPTS_KEY] = json
        }
    }

    fun getReceipts(context: Context): Flow<List<ReceiptItem>> {
        return context.dataStore.data.map { prefs ->
            val json = prefs[RECEIPTS_KEY] ?: return@map emptyList()
            try {
                adapter.fromJson(json) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun clearReceipts(context: Context) {
        context.dataStore.edit { prefs ->
            prefs.remove(RECEIPTS_KEY)
        }
    }
}
