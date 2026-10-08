package com.example.data.model

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Relation data class combining an [Expense] with its associated [Category].
 */
data class ExpenseWithCategory(
    @Embedded
    val expense: Expense,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: Category? = null
)
