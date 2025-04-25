package com.example.receiptbackup.data.model

import java.time.LocalDate

data class ReceiptItem(
    val id: String,
    val name: String,
    val store: String,
    val date: LocalDate,
    val price: Double,
    val imageUrl: String? = null
)

// 🔁 Convert to Firestore-compatible map
fun ReceiptItem.toMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "store" to store,
    "date" to date.toString(), // ISO 8601 format: yyyy-MM-dd
    "price" to price,
    "imageUrl" to imageUrl
)

// 🔁 Convert Firestore document back to ReceiptItem
fun Map<String, Any?>.toReceiptItem(id: String): ReceiptItem {
    return ReceiptItem(
        id = id,
        name = this["name"] as? String ?: "",
        store = this["store"] as? String ?: "",
        date = LocalDate.parse(this["date"] as? String ?: ""),
        price = (this["price"] as? Number)?.toDouble() ?: 0.0,
        imageUrl = this["imageUrl"] as? String
    )
}