package com.example.data.repository

import com.example.data.db.CategoryDao
import com.example.data.db.ExpenseDao
import com.example.data.model.Category
import com.example.data.model.Expense
import com.example.data.model.ExpenseWithCategory
import kotlinx.coroutines.flow.Flow

/**
 * Unified Repository pattern implementation for the 'Track' feature,
 * orchestrating data persistence for [Expense] and [Category] entities.
 */
class TrackRepository(
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao
) {

    // --- Expenses ---
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()

    val expensesWithCategory: Flow<List<ExpenseWithCategory>> = expenseDao.getExpensesWithCategory()

    fun getExpensesByCategory(category: String): Flow<List<Expense>> =
        expenseDao.getExpensesByCategory(category)

    fun getExpensesByCategoryId(categoryId: Long): Flow<List<Expense>> =
        expenseDao.getExpensesByCategoryId(categoryId)

    fun getExpensesBetweenDates(startDate: Long, endDate: Long): Flow<List<Expense>> =
        expenseDao.getExpensesBetweenDates(startDate, endDate)

    suspend fun getExpenseById(id: Long): Expense? = expenseDao.getExpenseById(id)

    suspend fun insertExpense(expense: Expense): Long = expenseDao.insertExpense(expense)

    suspend fun insertExpenses(expenses: List<Expense>) = expenseDao.insertExpenses(expenses)

    suspend fun updateExpense(expense: Expense) = expenseDao.updateExpense(expense)

    suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)

    suspend fun deleteExpenseById(id: Long) = expenseDao.deleteExpenseById(id)

    fun getTotalExpenseAmount(): Flow<Double?> = expenseDao.getTotalExpenseAmount()

    // --- Categories ---
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    suspend fun getCategoriesList(): List<Category> = categoryDao.getCategoriesList()

    fun getCategoryById(id: Long): Flow<Category?> = categoryDao.getCategoryById(id)

    suspend fun findCategoryById(id: Long): Category? = categoryDao.findCategoryById(id)

    suspend fun findCategoryByName(name: String): Category? = categoryDao.findCategoryByName(name)

    suspend fun insertCategory(category: Category): Long = categoryDao.insertCategory(category)

    suspend fun insertCategories(categories: List<Category>) = categoryDao.insertCategories(categories)

    suspend fun updateCategory(category: Category) = categoryDao.updateCategory(category)

    suspend fun deleteCategory(category: Category) = categoryDao.deleteCategory(category)

    suspend fun deleteCategoryById(id: Long) = categoryDao.deleteCategoryById(id)
}
