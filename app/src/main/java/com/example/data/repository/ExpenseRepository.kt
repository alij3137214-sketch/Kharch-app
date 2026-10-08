package com.example.data.repository

import com.example.data.db.ExpenseDao
import com.example.data.model.Expense
import kotlinx.coroutines.flow.Flow

/**
 * Repository pattern implementation abstracting access to [Expense] data.
 */
class ExpenseRepository(private val expenseDao: ExpenseDao) {

    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()

    fun getExpensesByCategory(category: String): Flow<List<Expense>> =
        expenseDao.getExpensesByCategory(category)

    fun getExpensesBetweenDates(startDate: Long, endDate: Long): Flow<List<Expense>> =
        expenseDao.getExpensesBetweenDates(startDate, endDate)

    suspend fun getExpenseById(id: Long): Expense? = expenseDao.getExpenseById(id)

    suspend fun insert(expense: Expense): Long = expenseDao.insert(expense)

    suspend fun insertExpense(expense: Expense): Long = expenseDao.insertExpense(expense)

    suspend fun insertExpenses(expenses: List<Expense>) = expenseDao.insertExpenses(expenses)

    suspend fun insertAll(vararg expenses: Expense) = expenseDao.insertAll(*expenses)

    suspend fun insertAll(expenses: List<Expense>) = expenseDao.insertAll(expenses)

    suspend fun update(expense: Expense) = expenseDao.update(expense)

    suspend fun updateExpense(expense: Expense) = expenseDao.updateExpense(expense)

    suspend fun delete(expense: Expense) = expenseDao.delete(expense)

    suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)

    suspend fun deleteById(id: Long) = expenseDao.deleteExpenseById(id)

    suspend fun clearAll() = expenseDao.clearAllExpenses()

    suspend fun deleteAll() = expenseDao.deleteAllExpenses()

    fun getTotalExpenseAmount(): Flow<Double?> = expenseDao.getTotalExpenseAmount()
}
