package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.KharchDatabase
import com.example.data.model.BillReminderEntity
import com.example.data.model.BudgetEntity
import com.example.data.model.ExpenseCategory
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.repository.KharchRepository
import com.example.ui.viewmodel.BudgetAlertStatus
import com.example.ui.viewmodel.KharchUiState
import com.example.domain.MoneyMath
import com.example.domain.PeriodKind
import com.example.ui.viewmodel.SimVerdict
import com.example.util.CsvExporter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.util.Calendar
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class KharchAppCujTest {

    private lateinit var db: KharchDatabase
    private lateinit var repository: KharchRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, KharchDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = KharchRepository(db.kharchDao())
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    private fun getCurrentMonthYear(): String {
        val cal = Calendar.getInstance()
        return String.format(Locale.US, "%d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    @Test
    fun cuj_insertTransaction_andVerifyBalances() = runBlocking {
        // 1. Add Income
        val incomeTx = TransactionEntity(
            title = "Monthly Salary",
            amount = 120000.0,
            type = TransactionType.INCOME.name,
            category = "Salary",
            paymentMethod = "Bank Account",
            timestamp = System.currentTimeMillis()
        )
        val incomeId = repository.insertTransaction(incomeTx)
        assertTrue(incomeId > 0)

        // 2. Add Expense
        val expenseTx = TransactionEntity(
            title = "Supermarket",
            amount = 25000.0,
            type = TransactionType.EXPENSE.name,
            category = ExpenseCategory.FOOD.displayName,
            paymentMethod = "Credit Card",
            timestamp = System.currentTimeMillis()
        )
        val expenseId = repository.insertTransaction(expenseTx)
        assertTrue(expenseId > 0)

        // 3. Verify in repository Flow
        val allTx = repository.allTransactions.first()
        assertEquals(2, allTx.size)

        // 4. Verify in UiState
        val uiState = KharchUiState(transactions = allTx)
        assertEquals(120000.0, uiState.totalIncome, 0.01)
        assertEquals(25000.0, uiState.totalExpense, 0.01)
        assertEquals(95000.0, uiState.totalBalance, 0.01)
    }

    @Test
    fun cuj_budgetTracking_andThresholdAlerts() = runBlocking {
        val monthYear = getCurrentMonthYear()

        // 1. Set budget with 80% warning threshold
        val budget = BudgetEntity(
            category = ExpenseCategory.FOOD.displayName,
            monthlyLimit = 20000.0,
            monthYear = monthYear,
            alertThresholdPercent = 80
        )
        repository.insertBudget(budget)

        // 2. Add expense below threshold (10,000 / 20,000 = 50%) -> SAFE
        repository.insertTransaction(
            TransactionEntity(
                title = "Dining",
                amount = 10000.0,
                type = TransactionType.EXPENSE.name,
                category = ExpenseCategory.FOOD.displayName,
                paymentMethod = "Cash",
                timestamp = System.currentTimeMillis()
            )
        )

        var uiState = KharchUiState(
            budgets = repository.allBudgets.first(),
            transactions = repository.allTransactions.first()
        )
        var foodStatus = uiState.categoryBudgetStatuses.find { it.category == ExpenseCategory.FOOD.displayName }
        assertNotNull(foodStatus)
        assertEquals(BudgetAlertStatus.SAFE, foodStatus?.alertStatus)
        assertEquals(10000.0, foodStatus?.remaining ?: 0.0, 0.01)

        // 3. Add expense crossing 80% threshold (total 17,000 / 20,000 = 85%) -> WARNING
        repository.insertTransaction(
            TransactionEntity(
                title = "Dinner Out",
                amount = 7000.0,
                type = TransactionType.EXPENSE.name,
                category = ExpenseCategory.FOOD.displayName,
                paymentMethod = "Card",
                timestamp = System.currentTimeMillis()
            )
        )

        uiState = KharchUiState(
            budgets = repository.allBudgets.first(),
            transactions = repository.allTransactions.first()
        )
        foodStatus = uiState.categoryBudgetStatuses.find { it.category == ExpenseCategory.FOOD.displayName }
        assertEquals(BudgetAlertStatus.WARNING, foodStatus?.alertStatus)
        assertEquals(3000.0, foodStatus?.remaining ?: 0.0, 0.01)
        assertTrue(uiState.hasBudgetAlerts)

        // 4. Add expense exceeding 100% (total 22,000 / 20,000 = 110%) -> EXCEEDED
        repository.insertTransaction(
            TransactionEntity(
                title = "Gourmet Meal",
                amount = 5000.0,
                type = TransactionType.EXPENSE.name,
                category = ExpenseCategory.FOOD.displayName,
                paymentMethod = "Card",
                timestamp = System.currentTimeMillis()
            )
        )

        uiState = KharchUiState(
            budgets = repository.allBudgets.first(),
            transactions = repository.allTransactions.first()
        )
        foodStatus = uiState.categoryBudgetStatuses.find { it.category == ExpenseCategory.FOOD.displayName }
        assertEquals(BudgetAlertStatus.EXCEEDED, foodStatus?.alertStatus)
        assertEquals(2000.0, foodStatus?.overAmount ?: 0.0, 0.01)
        assertEquals(1, uiState.exceededBudgets.size)
    }

    @Test
    fun cuj_affordabilitySimulator_withRecurringFixedCosts() = runBlocking {
        // 1. Add recurring bills
        val rent = BillReminderEntity(
            title = "Apartment Rent",
            amount = 30000.0,
            category = "Housing",
            dueDate = System.currentTimeMillis() + 86400000L,
            isPaid = false
        )
        val internet = BillReminderEntity(
            title = "Broadband",
            amount = 3500.0,
            category = "Utilities",
            dueDate = System.currentTimeMillis() + 86400000L,
            isPaid = false
        )
        repository.insertBillReminder(rent)
        repository.insertBillReminder(internet)

        // 2. Add income
        repository.insertTransaction(
            TransactionEntity(
                title = "Income",
                amount = 90000.0,
                type = TransactionType.INCOME.name,
                category = "Salary",
                paymentMethod = "Bank",
                timestamp = System.currentTimeMillis()
            )
        )

        // 3. Add variable expense
        repository.insertTransaction(
            TransactionEntity(
                title = "Groceries",
                amount = 16500.0,
                type = TransactionType.EXPENSE.name,
                category = ExpenseCategory.FOOD.displayName,
                paymentMethod = "Card",
                timestamp = System.currentTimeMillis()
            )
        )

        val uiState = KharchUiState(
            transactions = repository.allTransactions.first(),
            billReminders = repository.allBillReminders.first(),
            affordabilityBasisType = "INCOME"
        )

        val analysis = uiState.affordabilityAnalysis

        // Total Recurring Fixed Costs = 30,000 + 3,500 = 33,500
        // Variable spend = 16,500
        // Remaining Discretionary = 90,000 - 33,500 - 16,500 = 40,000
        assertEquals(90000.0, analysis.budgetBasis, 0.01)
        assertEquals(33500.0, analysis.totalRecurringFixedCosts, 0.01)
        assertEquals(16500.0, analysis.alreadySpentVariable, 0.01)
        assertEquals(40000.0, analysis.remainingDiscretionary, 0.01)
        assertFalse(analysis.isDeficit)
        assertTrue(analysis.safeDailySpend > 0.0)

        // 4. Test Affordable Purchase (e.g. Rs. 3,000)
        val simComfortable = uiState.simulatePurchaseAffordability(analysis, "Shoes", 3000.0)
        assertEquals(SimVerdict.COMFORTABLE, simComfortable.verdict)
        assertEquals(37000.0, simComfortable.remainingAfter, 0.01)

        // 5. Test Deficit Purchase (e.g. Rs. 50,000 > 40,000 remaining)
        val simDeficit = uiState.simulatePurchaseAffordability(analysis, "Luxury TV", 50000.0)
        assertEquals(SimVerdict.DEFICIT, simDeficit.verdict)
        assertEquals(-10000.0, simDeficit.remainingAfter, 0.01)
    }

    @Test
    fun cuj_savingsGoal_contributionAndProgress() = runBlocking {
        // 1. Create a Savings Goal
        val goal = SavingsGoalEntity(
            title = "Emergency Fund",
            targetAmount = 100000.0,
            currentAmount = 20000.0,
            targetDate = "2026-12-31",
            iconName = "SAVINGS"
        )
        val goalId = repository.insertSavingsGoal(goal)
        assertTrue(goalId > 0)

        // 2. Add contribution
        val updatedGoal = goal.copy(id = goalId, currentAmount = 50000.0)
        repository.updateSavingsGoal(updatedGoal)

        val goals = repository.allSavingsGoals.first()
        val saved = goals.find { it.id == goalId }
        assertNotNull(saved)
        assertEquals(50000.0, saved?.currentAmount ?: 0.0, 0.01)
        assertEquals(100000.0, saved?.targetAmount ?: 0.0, 0.01)
        assertEquals(0.5f, ((saved?.currentAmount ?: 0.0) / (saved?.targetAmount ?: 1.0)).toFloat(), 0.01f)
    }

    @Test
    fun cuj_billReminder_markAsPaid_createsExpenseTransaction() = runBlocking {
        val bill = BillReminderEntity(
            title = "Gym Membership",
            amount = 4500.0,
            category = "Fitness",
            dueDate = System.currentTimeMillis() + 86400000L,
            isPaid = false
        )
        val billId = repository.insertBillReminder(bill)
        val storedBill = bill.copy(id = billId)

        // Mark as paid via repository
        repository.markBillAsPaid(storedBill)

        // 1. Bill should now be marked as paid
        val bills = repository.allBillReminders.first()
        val paidBill = bills.find { it.id == billId }
        assertTrue(paidBill?.isPaid == true)

        // 2. Automatic expense transaction created
        val txs = repository.allTransactions.first()
        val autoTx = txs.find { it.title == "Gym Membership" }
        assertNotNull(autoTx)
        assertEquals(4500.0, autoTx?.amount ?: 0.0, 0.01)
        assertEquals(TransactionType.EXPENSE.name, autoTx?.type)
    }

    @Test
    fun cuj_monthlySpendingLimit_andRemainingBudgetTracking() = runBlocking {
        val monthYear = getCurrentMonthYear()

        // 1. Set Monthly Spending Limit to Rs. 80,000 with 80% threshold
        val overallBudget = BudgetEntity(
            category = "OVERALL",
            monthlyLimit = 80000.0,
            monthYear = monthYear,
            alertThresholdPercent = 80
        )
        val budgetId = repository.insertBudget(overallBudget)

        // 2. Initial state: 0 spent -> 80,000 remaining (100% remaining)
        var uiState = KharchUiState(
            budgets = repository.allBudgets.first(),
            transactions = repository.allTransactions.first()
        )

        assertEquals(80000.0, uiState.effectiveMonthlySpendingLimit, 0.01)
        assertTrue(uiState.isMonthlyLimitCustomSet)
        assertEquals(0.0, uiState.currentMonthTotalExpense, 0.01)
        assertEquals(80000.0, uiState.currentMonthBudgetRemaining, 0.01)
        assertEquals(100, uiState.currentMonthBudgetRemainingPercentage)
        assertEquals(BudgetAlertStatus.SAFE, uiState.currentMonthBudgetAlertStatus)
        assertFalse(uiState.isCurrentMonthBudgetExceeded)
        assertTrue(uiState.dailyBudgetRemainingAllowance > 0)

        // 3. Add Expense of Rs. 30,000
        repository.insertTransaction(
            TransactionEntity(
                title = "Grocery & Household",
                amount = 30000.0,
                type = TransactionType.EXPENSE.name,
                category = "Food",
                paymentMethod = "Card",
                timestamp = System.currentTimeMillis()
            )
        )

        uiState = KharchUiState(
            budgets = repository.allBudgets.first(),
            transactions = repository.allTransactions.first()
        )

        assertEquals(30000.0, uiState.currentMonthTotalExpense, 0.01)
        assertEquals(50000.0, uiState.currentMonthBudgetRemaining, 0.01)
        assertEquals(37, uiState.currentMonthBudgetSpentPercentage) // 30,000 / 80,000 = 37.5% -> 37%
        assertEquals(62, uiState.currentMonthBudgetRemainingPercentage)
        assertEquals(BudgetAlertStatus.SAFE, uiState.currentMonthBudgetAlertStatus)
        assertFalse(uiState.isCurrentMonthBudgetExceeded)

        // 4. Add another Expense of Rs. 38,000 (Total spent = 68,000 / 80,000 = 85% -> reaches Warning threshold >= 80%)
        repository.insertTransaction(
            TransactionEntity(
                title = "Electronics Purchase",
                amount = 38000.0,
                type = TransactionType.EXPENSE.name,
                category = "Shopping",
                paymentMethod = "Bank",
                timestamp = System.currentTimeMillis()
            )
        )

        uiState = KharchUiState(
            budgets = repository.allBudgets.first(),
            transactions = repository.allTransactions.first()
        )

        assertEquals(68000.0, uiState.currentMonthTotalExpense, 0.01)
        assertEquals(12000.0, uiState.currentMonthBudgetRemaining, 0.01)
        assertEquals(85, uiState.currentMonthBudgetSpentPercentage)
        assertEquals(BudgetAlertStatus.WARNING, uiState.currentMonthBudgetAlertStatus)
        assertFalse(uiState.isCurrentMonthBudgetExceeded)

        // 5. Add Expense that crosses monthly limit (Total spent = 68,000 + 15,000 = 83,000 > 80,000)
        repository.insertTransaction(
            TransactionEntity(
                title = "Weekend Trip",
                amount = 15000.0,
                type = TransactionType.EXPENSE.name,
                category = "Travel",
                paymentMethod = "Cash",
                timestamp = System.currentTimeMillis()
            )
        )

        uiState = KharchUiState(
            budgets = repository.allBudgets.first(),
            transactions = repository.allTransactions.first()
        )

        assertEquals(83000.0, uiState.currentMonthTotalExpense, 0.01)
        assertEquals(-3000.0, uiState.currentMonthBudgetRemaining, 0.01)
        assertTrue(uiState.isCurrentMonthBudgetExceeded)
        assertEquals(BudgetAlertStatus.EXCEEDED, uiState.currentMonthBudgetAlertStatus)
        assertEquals(0.0, uiState.dailyBudgetRemainingAllowance, 0.01)

        // 6. User raises their monthly spending limit to Rs. 100,000
        val updatedBudget = overallBudget.copy(id = budgetId, monthlyLimit = 100000.0)
        repository.updateBudget(updatedBudget)

        uiState = KharchUiState(
            budgets = repository.allBudgets.first(),
            transactions = repository.allTransactions.first()
        )

        assertEquals(100000.0, uiState.effectiveMonthlySpendingLimit, 0.01)
        assertEquals(17000.0, uiState.currentMonthBudgetRemaining, 0.01)
        assertFalse(uiState.isCurrentMonthBudgetExceeded)
        // 83,000 / 100,000 = 83% >= 80% threshold -> WARNING
        assertEquals(BudgetAlertStatus.WARNING, uiState.currentMonthBudgetAlertStatus)
        assertTrue(uiState.dailyBudgetRemainingAllowance > 0)

        // 7. User further increases limit to Rs. 120,000 (83k / 120k = 69% < 80% -> SAFE)
        val generousBudget = updatedBudget.copy(monthlyLimit = 120000.0)
        repository.updateBudget(generousBudget)

        uiState = KharchUiState(
            budgets = repository.allBudgets.first(),
            transactions = repository.allTransactions.first()
        )

        assertEquals(120000.0, uiState.effectiveMonthlySpendingLimit, 0.01)
        assertEquals(37000.0, uiState.currentMonthBudgetRemaining, 0.01)
        assertFalse(uiState.isCurrentMonthBudgetExceeded)
        assertEquals(BudgetAlertStatus.SAFE, uiState.currentMonthBudgetAlertStatus)
        assertTrue(uiState.dailyBudgetRemainingAllowance > 0)
    }

    @Test
    fun cuj_understandScreen_rechartsMonthlyExpenseCategoryBreakdown() = runBlocking {
        // 1. Add monthly expenses in different categories
        repository.insertTransaction(
            TransactionEntity(
                title = "Dinner & Groceries",
                amount = 20000.0,
                type = TransactionType.EXPENSE.name,
                category = "Food",
                paymentMethod = "Card",
                timestamp = System.currentTimeMillis()
            )
        )
        repository.insertTransaction(
            TransactionEntity(
                title = "Fuel & Cab",
                amount = 10000.0,
                type = TransactionType.EXPENSE.name,
                category = "Transport",
                paymentMethod = "Cash",
                timestamp = System.currentTimeMillis()
            )
        )
        repository.insertTransaction(
            TransactionEntity(
                title = "Electric Utility",
                amount = 10000.0,
                type = TransactionType.EXPENSE.name,
                category = "Bills",
                paymentMethod = "Bank",
                timestamp = System.currentTimeMillis()
            )
        )

        val txs = repository.allTransactions.first()
        val uiState = KharchUiState(transactions = txs)

        // Total monthly spending = 40,000
        val shares = uiState.currentMonthCategoryShares
        assertEquals(3, shares.size)

        // Food: 20,000 / 40,000 = 50%
        val foodShare = shares.find { it.category == "Food" }
        assertNotNull(foodShare)
        assertEquals(20000.0, foodShare?.amount ?: 0.0, 0.01)
        assertEquals(0.50f, foodShare?.percentage ?: 0f, 0.01f)

        // Transport: 10,000 / 40,000 = 25%
        val transportShare = shares.find { it.category == "Transport" }
        assertNotNull(transportShare)
        assertEquals(10000.0, transportShare?.amount ?: 0.0, 0.01)
        assertEquals(0.25f, transportShare?.percentage ?: 0f, 0.01f)

        // Bills: 10,000 / 40,000 = 25%
        val billsShare = shares.find { it.category == "Bills" }
        assertNotNull(billsShare)
        assertEquals(10000.0, billsShare?.amount ?: 0.0, 0.01)
        assertEquals(0.25f, billsShare?.percentage ?: 0f, 0.01f)
    }

    @Test
    fun cuj_exportMonthlyExpensesCsv_andVerifyFormat() = runBlocking {
        // 1. Add sample monthly expense transactions
        val now = System.currentTimeMillis()
        val expense1 = TransactionEntity(
            title = "Whole Foods, Market",
            amount = 14500.50,
            type = TransactionType.EXPENSE.name,
            category = "Groceries",
            paymentMethod = "Credit Card",
            timestamp = now,
            note = "Weekly groceries, fruits & veggies"
        )
        val expense2 = TransactionEntity(
            title = "Shell Gas \"Fuel\"",
            amount = 5200.00,
            type = TransactionType.EXPENSE.name,
            category = "Transport",
            paymentMethod = "Cash",
            timestamp = now + 1000L,
            note = "Full tank"
        )
        repository.insertTransaction(expense1)
        repository.insertTransaction(expense2)

        val expenses = listOf(expense1, expense2)
        val monthLabel = "September 2026"

        // 2. Generate CSV
        val csv = CsvExporter.generateMonthlyExpensesCsv(expenses, monthLabel)
        assertNotNull(csv)
        assertTrue(csv.startsWith("Date,Time,Title,Category,Amount,Payment Method,Note\n"))

        // 3. Verify escaping of commas and quotes
        assertTrue(csv.contains("\"Whole Foods, Market\""))
        assertTrue(csv.contains("\"Shell Gas \"\"Fuel\"\"\""))
        assertTrue(csv.contains("14500.50"))
        assertTrue(csv.contains("5200.00"))

        // 4. Verify filename generator
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val filename = CsvExporter.getMonthlyCsvFilename(cal)
        assertTrue(filename.startsWith("kharch_expenses_"))
        assertTrue(filename.endsWith(".csv"))
    }

    @Test
    fun cuj_monthlyCharts_dailyBarsAndBiggestSpend() = runBlocking {
        // A fixed "today" (20 October 2026, noon) so the test gives the same answer on every day.
        val today = Calendar.getInstance().apply { clear(); set(2026, Calendar.OCTOBER, 20, 12, 0) }.timeInMillis
        val day = 86_400_000L
        repository.insertTransaction(
            TransactionEntity(title = "Coffee & Lunch", amount = 2500.0, type = TransactionType.EXPENSE.name, category = "Food", paymentMethod = "Card", timestamp = today)
        )
        repository.insertTransaction(
            TransactionEntity(title = "Electronics Store", amount = 45000.0, type = TransactionType.EXPENSE.name, category = "Shopping", paymentMethod = "Card", timestamp = today - 5 * day)
        )
        repository.insertTransaction(
            TransactionEntity(title = "Utility Bills", amount = 8000.0, type = TransactionType.EXPENSE.name, category = "Bills", paymentMethod = "Bank", timestamp = today - 10 * day)
        )

        val all = repository.allTransactions.first()
        val range = MoneyMath.periodRange(PeriodKind.MONTH, 0, today)
        val summary = MoneyMath.summarize(all, range, today)

        // One bar for each of the 31 days of October
        assertEquals(31, summary.bars.size)
        assertEquals(55500.0, summary.spent, 0.01)
        // The biggest bar and the biggest spend are the 45,000 electronics
        assertEquals(45000.0, summary.busiestBar?.amount ?: 0.0, 0.01)
        assertEquals(45000.0, summary.biggestExpense?.amount ?: 0.0, 0.01)
        // Categories are sorted from biggest to smallest
        assertEquals("Shopping", summary.byCategory.first().category)
        // 20 days had passed (1st to 19th) before today; 3 of them... only days after the first record count.
        assertTrue(summary.noSpendDays in 0..19)
    }
}