package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.db.ExpenseDao
import com.example.data.db.ExpenseDatabase
import com.example.data.db.KharchDatabase
import com.example.data.model.Expense
import com.example.data.repository.ExpenseRepository
import com.example.data.repository.KharchRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExpenseRoomDatabaseTest {

    private lateinit var expenseDb: ExpenseDatabase
    private lateinit var appDb: AppDatabase
    private lateinit var kharchDb: KharchDatabase
    private lateinit var expenseDao: ExpenseDao
    private lateinit var repository: ExpenseRepository
    private lateinit var kharchRepository: KharchRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // 1. In-memory ExpenseDatabase
        expenseDb = Room.inMemoryDatabaseBuilder(context, ExpenseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        expenseDao = expenseDb.expenseDao()
        repository = ExpenseRepository(expenseDao)

        // 2. In-memory AppDatabase
        appDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        // 3. In-memory KharchDatabase
        kharchDb = Room.inMemoryDatabaseBuilder(context, KharchDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        kharchRepository = KharchRepository(kharchDb.kharchDao(), kharchDb.expenseDao())
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        expenseDb.close()
        appDb.close()
        kharchDb.close()
    }

    @Test
    fun testExpenseEntity_fieldsAndCreation() {
        val testDate = 1727481600000L
        val expense = Expense(
            amount = 45.50,
            category = "Groceries",
            date = testDate,
            description = "Weekly vegetable and fruit run"
        )

        assertEquals(45.50, expense.amount, 0.001)
        assertEquals("Groceries", expense.category)
        assertEquals(testDate, expense.date)
        assertEquals("Weekly vegetable and fruit run", expense.description)
        assertEquals(0L, expense.id)
    }

    @Test
    fun testInsertMethods_insertAndInsertExpense() = runBlocking {
        val testDate = System.currentTimeMillis()
        val expense1 = Expense(
            amount = 120.00,
            category = "Utilities",
            date = testDate,
            description = "Electricity bill payment"
        )

        // Test insertExpense method
        val id1 = expenseDao.insertExpense(expense1)
        assertTrue(id1 > 0)

        // Test insert method
        val expense2 = Expense(
            amount = 35.00,
            category = "Transport",
            date = testDate + 1000,
            description = "Metro pass"
        )
        val id2 = expenseDao.insert(expense2)
        assertTrue(id2 > 0)

        val allExpenses = expenseDao.queryAllExpenses().first()
        assertEquals(2, allExpenses.size)
    }

    @Test
    fun testDeleteMethods_deleteAndClearAll() = runBlocking {
        val expense = Expense(
            amount = 15.00,
            category = "Dining",
            date = System.currentTimeMillis(),
            description = "Lunch sushi"
        )
        val id = expenseDao.insertExpense(expense)

        val inserted = expenseDao.getExpenseById(id)
        assertNotNull(inserted)

        // Test delete method
        expenseDao.delete(inserted!!)
        assertNull(expenseDao.getExpenseById(id))

        // Test insert and deleteExpense
        val id2 = expenseDao.insert(expense.copy(id = 0, description = "Ramen dinner"))
        val inserted2 = expenseDao.getExpenseById(id2)
        assertNotNull(inserted2)
        expenseDao.deleteExpense(inserted2!!)
        assertNull(expenseDao.getExpenseById(id2))
    }

    @Test
    fun testQueryAllExpenses_flowAndList() = runBlocking {
        expenseDao.clearAllExpenses()
        val e1 = Expense(amount = 10.0, category = "Snacks", date = 1000L, description = "Chips")
        val e2 = Expense(amount = 25.0, category = "Fuel", date = 2000L, description = "Petrol")
        expenseDao.insertExpenses(listOf(e1, e2))

        // Test queryAllExpenses Flow
        val flowList = expenseDao.queryAllExpenses().first()
        assertEquals(2, flowList.size)

        // Test getAllExpenses Flow
        val allFlow = expenseDao.getAllExpenses().first()
        assertEquals(2, allFlow.size)

        // Test getExpensesList suspend list
        val suspendList = expenseDao.getExpensesList()
        assertEquals(2, suspendList.size)
    }

    @Test
    fun testQueryExpensesByCategory_flowAndAliases() = runBlocking {
        expenseDao.clearAllExpenses()
        val e1 = Expense(amount = 20.0, category = "Coffee", date = 100L, description = "Latte")
        val e2 = Expense(amount = 50.0, category = "Dining", date = 200L, description = "Dinner")
        val e3 = Expense(amount = 15.0, category = "Coffee", date = 300L, description = "Espresso")

        expenseDao.insertExpenses(listOf(e1, e2, e3))

        // getExpensesByCategory
        val coffeeList1 = expenseDao.getExpensesByCategory("Coffee").first()
        assertEquals(2, coffeeList1.size)
        assertTrue(coffeeList1.all { it.category == "Coffee" })

        // queryExpensesByCategory alias
        val coffeeList2 = expenseDao.queryExpensesByCategory("Coffee").first()
        assertEquals(2, coffeeList2.size)

        // suspend list
        val diningList = expenseDao.getExpensesByCategoryList("Dining")
        assertEquals(1, diningList.size)
        assertEquals("Dinner", diningList.first().description)
    }

    @Test
    fun testQueryExpensesByDateRange_flowAndAliases() = runBlocking {
        expenseDao.clearAllExpenses()
        val e1 = Expense(amount = 10.0, category = "Books", date = 1000L, description = "Novel")
        val e2 = Expense(amount = 20.0, category = "Books", date = 2000L, description = "Textbook")
        val e3 = Expense(amount = 30.0, category = "Books", date = 3000L, description = "Audiobook")

        expenseDao.insertExpenses(listOf(e1, e2, e3))

        // queryExpensesByDateRange
        val ranged1 = expenseDao.queryExpensesByDateRange(1500L, 3500L).first()
        assertEquals(2, ranged1.size)

        // getExpensesByDateRange
        val ranged2 = expenseDao.getExpensesByDateRange(1500L, 2500L).first()
        assertEquals(1, ranged2.size)
        assertEquals("Textbook", ranged2.first().description)

        // getExpensesBetweenDates
        val ranged3 = expenseDao.getExpensesBetweenDates(500L, 3500L).first()
        assertEquals(3, ranged3.size)
    }

    @Test
    fun testRemoveDummyData_clearsAllRecordsForRealData() = runBlocking {
        // Seed testing data
        kharchRepository.seedRandomTestingData()
        val txBefore = kharchRepository.allTransactions.first()
        assertTrue(txBefore.isNotEmpty())

        // Clear all dummy data
        kharchRepository.clearAllData()
        val txAfter = kharchRepository.allTransactions.first()
        val budgetsAfter = kharchRepository.allBudgets.first()
        val goalsAfter = kharchRepository.allSavingsGoals.first()
        val billsAfter = kharchRepository.allBillReminders.first()
        val expensesAfter = kharchRepository.allExpenses.first()

        // All should be completely 0, pristine and ready for real data
        assertEquals(0, txAfter.size)
        assertEquals(0, budgetsAfter.size)
        assertEquals(0, goalsAfter.size)
        assertEquals(0, billsAfter.size)
        assertEquals(0, expensesAfter.size)
    }
}
