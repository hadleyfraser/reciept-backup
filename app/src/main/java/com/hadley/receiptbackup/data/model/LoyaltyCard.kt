package com.hadley.receiptbackup.data.model

import java.util.UUID

data class LoyaltyCard(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val notes: String = "",
    val barcodeType: String,
    val barcodeValue: String,
    val coverColor: Int,
    val barcodeFullWidth: Boolean = true,
    val cardImageUrl: String? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
