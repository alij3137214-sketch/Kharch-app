package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: String, // "EXPENSE", "INCOME", "TRANSFER"
    val category: String, // Category name or Income source
    val paymentMethod: String,
    val toPaymentMethod: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = "",
    val receiptUri: String? = null
)
