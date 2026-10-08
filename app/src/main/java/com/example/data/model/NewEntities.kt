package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Something the user wants to buy. It waits a few days before the app asks "still want it?". */
@Entity(tableName = "wish_items")
data class WishItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val price: Double,
    val addedAt: Long = System.currentTimeMillis(),
    val waitDays: Int,
    val status: String = WishStatus.WAITING.name,
    val note: String = ""
)

enum class WishStatus { WAITING, BOUGHT, DROPPED }

/** Udhaar: money lent to someone, or borrowed from someone. */
@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val person: String,
    val amount: Double,
    /** LENT = they owe me. BORROWED = I owe them. */
    val direction: String,
    val createdAt: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val note: String = "",
    val paidAmount: Double = 0.0
) {
    val remaining: Double get() = (amount - paidAmount).coerceAtLeast(0.0)
    val isSettled: Boolean get() = remaining <= 0.0
}

enum class DebtDirection { LENT, BORROWED }

/** A committee (kameti / BC): everyone pays every month and one member takes the whole pot each month. */
@Entity(tableName = "committees")
data class CommitteeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val monthlyAmount: Double,
    /** Number of members. It is also the number of months the committee runs. */
    val totalMembers: Int,
    /** The month number (1..totalMembers) when this user receives the pot. */
    val myTurn: Int,
    /** First month of the committee as "yyyy-MM". */
    val startMonth: String,
    val paidMonths: Int = 0,
    val payoutReceived: Boolean = false
) {
    val pot: Double get() = monthlyAmount * totalMembers
    val monthsLeft: Int get() = (totalMembers - paidMonths).coerceAtLeast(0)
    val isFinished: Boolean get() = paidMonths >= totalMembers && payoutReceived
}
