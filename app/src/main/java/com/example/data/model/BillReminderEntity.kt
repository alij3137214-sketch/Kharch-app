package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bill_reminders")
data class BillReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val dueDate: Long,
    val category: String = "Bills",
    val isPaid: Boolean = false,
    val paymentMethod: String = "Bank"
)
