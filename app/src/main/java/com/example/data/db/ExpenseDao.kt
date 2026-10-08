package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.Expense
import com.example.data.model.ExpenseWithCategory
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for local [Expense] records in Room.
 * Handles database operations for inserting, retrieving, updating, and deleting expense records.
 */
@Dao
interface ExpenseDao {

    // --- Retrieving Operations ---

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun queryAllExpenses(): Flow<List<Expense>> = getAllExpenses()

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    suspend fun getExpensesList(): List<Expense>

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    fun getExpenseByIdFlow(id: Long): Flow<Expense?>

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun getExpenseById(id: Long): Expense?

    @Query("SELECT * FROM expenses WHERE category = :category ORDER BY date DESC")
    fun getExpensesByCategory(category: String): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE category = :category ORDER BY date DESC")
    fun queryExpensesByCategory(category: String): Flow<List<Expense>> = getExpensesByCategory(category)

    @Query("SELECT * FROM expenses WHERE category = :category ORDER BY date DESC")
    suspend fun getExpensesByCategoryList(category: String): List<Expense>

    @Query("SELECT * FROM expenses WHERE categoryId = :categoryId ORDER BY date DESC")
    fun getExpensesByCategoryId(categoryId: Long): Flow<List<Expense>>

    @Transaction
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getExpensesWithCategory(): Flow<List<ExpenseWithCategory>>

    @Query("SELECT * FROM expenses WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getExpensesBetweenDates(startDate: Long, endDate: Long): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getExpensesByDateRange(startDate: Long, endDate: Long): Flow<List<Expense>> = getExpensesBetweenDates(startDate, endDate)

    @Query("SELECT * FROM expenses WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun queryExpensesByDateRange(startDate: Long, endDate: Long): Flow<List<Expense>> = getExpensesBetweenDates(startDate, endDate)

    @Query("SELECT * FROM expenses WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getExpensesBetweenDatesList(startDate: Long, endDate: Long): List<Expense>

    @Query("SELECT SUM(amount) FROM expenses")
    fun getTotalExpenseAmount(): Flow<Double?>

    @Query("SELECT COUNT(*) FROM expenses")
    fun getExpenseCount(): Flow<Int>

    // --- Inserting Operations ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: Expense): Long = insertExpense(expense)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<Expense>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg expenses: Expense)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expenses: List<Expense>) = insertExpenses(expenses)

    // --- Updating Operations ---

    @Update
    suspend fun updateExpense(expense: Expense)

    @Update
    suspend fun update(expense: Expense) = updateExpense(expense)

    // --- Deleting Operations ---

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense) = deleteExpense(expense)

    @Delete
    suspend fun deleteExpenses(expenses: List<Expense>)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Long)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long) = deleteExpenseById(id)

    @Query("DELETE FROM expenses")
    suspend fun clearAllExpenses()

    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses() = clearAllExpenses()

    @Query("DELETE FROM expenses")
    suspend fun deleteAll() = clearAllExpenses()
}
