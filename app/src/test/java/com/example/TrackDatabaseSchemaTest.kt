package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.CategoryDao
import com.example.data.db.ExpenseDao
import com.example.data.db.TrackDatabase
import com.example.data.model.Category
import com.example.data.model.Expense
import com.example.data.repository.TrackRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/**
 * Unit & Robolectric tests verifying the Room database schema for the 'Track' feature,
 * testing [Expense] and [Category] entity persistence, queries, relations, and repositories.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TrackDatabaseSchemaTest {

    private lateinit var db: TrackDatabase
    private lateinit var expenseDao: ExpenseDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var repository: TrackRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, TrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        expenseDao = db.expenseDao()
        categoryDao = db.categoryDao()
        repository = TrackRepository(expenseDao, categoryDao)
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun cuj_insertCategories_andVerifyPersistence() = runBlocking {
        // 1. Insert multiple categories
        val foodCat = Category(name = "Food & Dining", icon = "restaurant", colorHex = "#10B981", budgetLimit = 15000.0)
        val transportCat = Category(name = "Transportation", icon = "directions_car", colorHex = "#6366F1", budgetLimit = 8000.0)
        val shoppingCat = Category(name = "Shopping", icon = "shopping_bag", colorHex = "#0284C7", budgetLimit = 12000.0)

        val foodId = categoryDao.insertCategory(foodCat)
        val transportId = categoryDao.insertCategory(transportCat)
        val shoppingId = categoryDao.insertCategory(shoppingCat)

        assertTrue(foodId > 0)
        assertTrue(transportId > 0)
        assertTrue(shoppingId > 0)

        // 2. Query all categories via Flow
        val categories = categoryDao.getAllCategories().first()
        assertEquals(3, categories.size)

        // 3. Query category by name
        val fetchedFood = categoryDao.findCategoryByName("Food & Dining")
        assertNotNull(fetchedFood)
        assertEquals("Food & Dining", fetchedFood?.name)
        assertEquals(15000.0, fetchedFood?.budgetLimit ?: 0.0, 0.01)

        // 4. Test repository queries
        val repoCategories = repository.allCategories.first()
        assertEquals(3, repoCategories.size)
    }

    @Test
    fun cuj_insertExpenses_andVerifyTrackingPersistence() = runBlocking {
        // 1. Insert Category
        val catId = categoryDao.insertCategory(
            Category(name = "Groceries", icon = "local_grocery_store", colorHex = "#84CC16")
        )

        val now = System.currentTimeMillis()

        // 2. Insert Expenses linked with category
        val expense1 = Expense(
            amount = 3200.0,
            category = "Groceries",
            date = now - 1000L,
            description = "Weekly Vegetables & Fruits",
            categoryId = catId
        )
        val expense2 = Expense(
            amount = 1800.0,
            category = "Groceries",
            date = now,
            description = "Dairy & Bread",
            categoryId = catId
        )

        val id1 = expenseDao.insertExpense(expense1)
        val id2 = expenseDao.insertExpense(expense2)

        assertTrue(id1 > 0)
        assertTrue(id2 > 0)

        // 3. Query all expenses
        val allExpenses = expenseDao.getAllExpenses().first()
        assertEquals(2, allExpenses.size)

        // 4. Verify total expense calculation
        val total = expenseDao.getTotalExpenseAmount().first()
        assertEquals(5000.0, total ?: 0.0, 0.01)

        // 5. Query expenses by categoryId
        val catExpenses = expenseDao.getExpensesByCategoryId(catId).first()
        assertEquals(2, catExpenses.size)

        // 6. Query with category relation
        val expensesWithCat = expenseDao.getExpensesWithCategory().first()
        assertEquals(2, expensesWithCat.size)
        assertEquals("Groceries", expensesWithCat[0].category?.name)
    }

    @Test
    fun cuj_updateAndDeleteExpensesAndCategories() = runBlocking {
        val catId = repository.insertCategory(Category(name = "Utilities", colorHex = "#F59E0B"))
        val expId = repository.insertExpense(
            Expense(
                amount = 4500.0,
                category = "Utilities",
                description = "Electricity Bill",
                categoryId = catId
            )
        )

        // Verify initial insert
        val exp = repository.getExpenseById(expId)
        assertNotNull(exp)
        assertEquals(4500.0, exp?.amount ?: 0.0, 0.01)

        // Update expense amount
        val updatedExp = exp!!.copy(amount = 4800.0, description = "Electricity Bill (Revised)")
        repository.updateExpense(updatedExp)

        val fetchedUpdated = repository.getExpenseById(expId)
        assertEquals(4800.0, fetchedUpdated?.amount ?: 0.0, 0.01)
        assertEquals("Electricity Bill (Revised)", fetchedUpdated?.description)

        // Delete expense
        repository.deleteExpenseById(expId)
        val afterDelete = repository.allExpenses.first()
        assertEquals(0, afterDelete.size)

        // Delete category
        repository.deleteCategoryById(catId)
        val afterCatDelete = repository.allCategories.first()
        assertEquals(0, afterCatDelete.size)
    }
}
