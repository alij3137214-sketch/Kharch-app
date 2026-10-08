package com.example.data.repository

import com.example.data.db.ExpenseDao
import com.example.data.db.KharchDao
import com.example.data.model.BillReminderEntity
import com.example.data.model.BudgetEntity
import com.example.data.model.Expense
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import java.util.Calendar

class KharchRepository(
    private val dao: KharchDao,
    private val expenseDao: ExpenseDao? = null
) {

    val allExpenses: Flow<List<Expense>> = expenseDao?.getAllExpenses() ?: emptyFlow()
    val allTransactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()
    val allBudgets: Flow<List<BudgetEntity>> = dao.getAllBudgets()
    val allSavingsGoals: Flow<List<SavingsGoalEntity>> = dao.getAllSavingsGoals()
    val allBillReminders: Flow<List<BillReminderEntity>> = dao.getAllBillReminders()

    suspend fun insertExpense(expense: Expense): Long {
        val id = expenseDao?.insertExpense(expense) ?: 0L
        dao.insertTransaction(
            TransactionEntity(
                title = expense.description.ifBlank { expense.category },
                amount = expense.amount,
                type = TransactionType.EXPENSE.name,
                category = expense.category,
                paymentMethod = "Cash",
                timestamp = expense.date,
                note = expense.description
            )
        )
        return id
    }

    suspend fun getTransactionById(id: Long): TransactionEntity? = dao.getTransactionById(id)

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        val id = dao.insertTransaction(transaction)
        if (transaction.type == TransactionType.EXPENSE.name && expenseDao != null) {
            expenseDao.insertExpense(
                Expense(
                    amount = transaction.amount,
                    category = transaction.category,
                    date = transaction.timestamp,
                    description = transaction.title + if (transaction.note.isNotBlank()) " - ${transaction.note}" else ""
                )
            )
        }
        return id
    }

    suspend fun updateTransaction(transaction: TransactionEntity) = dao.updateTransaction(transaction)

    suspend fun deleteTransaction(transaction: TransactionEntity) = dao.deleteTransaction(transaction)

    suspend fun deleteTransactionById(id: Long) = dao.deleteTransactionById(id)

    suspend fun insertBudget(budget: BudgetEntity) = dao.insertBudget(budget)

    suspend fun updateBudget(budget: BudgetEntity) = dao.updateBudget(budget)

    suspend fun deleteBudget(budget: BudgetEntity) = dao.deleteBudget(budget)

    suspend fun insertSavingsGoal(goal: SavingsGoalEntity) = dao.insertSavingsGoal(goal)

    suspend fun updateSavingsGoal(goal: SavingsGoalEntity) = dao.updateSavingsGoal(goal)

    suspend fun deleteSavingsGoal(goal: SavingsGoalEntity) = dao.deleteSavingsGoal(goal)

    suspend fun insertBillReminder(bill: BillReminderEntity) = dao.insertBillReminder(bill)

    suspend fun updateBillReminder(bill: BillReminderEntity) = dao.updateBillReminder(bill)

    suspend fun deleteBillReminder(bill: BillReminderEntity) = dao.deleteBillReminder(bill)

    suspend fun markBillAsPaid(bill: BillReminderEntity) {
        dao.updateBillReminder(bill.copy(isPaid = true))
        // Automatically log this as an expense
        dao.insertTransaction(
            TransactionEntity(
                title = bill.title,
                amount = bill.amount,
                type = TransactionType.EXPENSE.name,
                category = bill.category,
                paymentMethod = bill.paymentMethod,
                timestamp = System.currentTimeMillis(),
                note = "Paid bill reminder"
            )
        )
    }

    suspend fun resetAllData() {
        clearAllData()
        seedRandomTestingData()
    }

    suspend fun clearAllData() {
        dao.clearAllTransactions()
        dao.clearAllBudgets()
        dao.clearAllSavingsGoals()
        dao.clearAllBillReminders()
        expenseDao?.clearAllExpenses()
    }

    suspend fun seedSampleDataIfEmpty() {
        // Ready for real personal finance: do not auto-inject dummy data on startup
    }

    suspend fun seedRandomTestingData() {
        clearAllData()

        val cal = Calendar.getInstance()
        val now = System.currentTimeMillis()
        val random = kotlin.random.Random(System.currentTimeMillis())

        fun timeAgo(daysAgo: Int, hour: Int, minute: Int): Long {
            val c = Calendar.getInstance()
            c.timeInMillis = now
            c.add(Calendar.DAY_OF_YEAR, -daysAgo)
            c.set(Calendar.HOUR_OF_DAY, hour)
            c.set(Calendar.MINUTE, minute)
            c.set(Calendar.SECOND, 0)
            return c.timeInMillis
        }

        fun timeAhead(daysAhead: Int, hour: Int): Long {
            val c = Calendar.getInstance()
            c.timeInMillis = now
            c.add(Calendar.DAY_OF_YEAR, daysAhead)
            c.set(Calendar.HOUR_OF_DAY, hour)
            c.set(Calendar.MINUTE, 0)
            return c.timeInMillis
        }

        val generatedTransactions = mutableListOf<TransactionEntity>()

        // 1. Realistic Income Transactions
        val incomeAmounts = listOf(95000.0, 120000.0, 145000.0, 85000.0)
        val primaryIncome = incomeAmounts[random.nextInt(incomeAmounts.size)]
        val freelanceAmt = listOf(22000.0, 35000.0, 18000.0, 40000.0)[random.nextInt(4)]

        generatedTransactions.add(
            TransactionEntity(
                title = "Primary Tech Salary",
                amount = primaryIncome,
                type = TransactionType.INCOME.name,
                category = "Salary",
                paymentMethod = "Bank",
                timestamp = timeAgo(random.nextInt(1, 4), 10, 0),
                note = "Monthly direct deposit"
            )
        )

        generatedTransactions.add(
            TransactionEntity(
                title = "Freelance Consulting Payout",
                amount = freelanceAmt,
                type = TransactionType.INCOME.name,
                category = "Freelance",
                paymentMethod = "Easypaisa",
                timestamp = timeAgo(random.nextInt(3, 7), 16, 30),
                note = "Milestone payout"
            )
        )

        // 2. Random Realistic Expenses
        val expensePool = listOf(
            Triple("Al-Fatah Supermarket", 3500.0..9500.0, "Food"),
            Triple("Total Parco Fuel Refill", 1800.0..4500.0, "Transport"),
            Triple("Monal Terrace Dinner", 3200.0..7500.0, "Food"),
            Triple("Electricity Bill Payment", 6000.0..12000.0, "Bills"),
            Triple("High-speed Fiber Broadband", 3200.0..4800.0, "Bills"),
            Triple("Outfitters Casual Wear", 2500.0..8000.0, "Shopping"),
            Triple("Pharmacy & Vitamins", 1100.0..3200.0, "Health"),
            Triple("Netflix & Spotify Subscription", 1800.0..2600.0, "Entertainment"),
            Triple("Vehicle Maintenance & Oil", 3000.0..6800.0, "Transport"),
            Triple("Espresso Lab Coffee", 650.0..1500.0, "Food")
        )

        val paymentMethods = listOf("Card", "Cash", "Easypaisa", "JazzCash", "Bank")

        expensePool.shuffled(random).take(random.nextInt(6, 9)).forEach { (title, range, cat) ->
            val amt = (random.nextDouble(range.start, range.endInclusive) / 50.0).toInt() * 50.0
            val pMethod = paymentMethods[random.nextInt(paymentMethods.size)]
            val daysAgo = random.nextInt(0, 11)
            val hour = random.nextInt(9, 22)
            val min = random.nextInt(0, 59)

            generatedTransactions.add(
                TransactionEntity(
                    title = title,
                    amount = amt,
                    type = TransactionType.EXPENSE.name,
                    category = cat,
                    paymentMethod = pMethod,
                    timestamp = timeAgo(daysAgo, hour, min),
                    note = "Random test record"
                )
            )
        }

        // 3. Random transfer
        generatedTransactions.add(
            TransactionEntity(
                title = "Transfer to Spending Wallet",
                amount = (random.nextInt(30, 80) * 100).toDouble(),
                type = TransactionType.TRANSFER.name,
                category = "Transfer",
                paymentMethod = "Bank",
                toPaymentMethod = "Easypaisa",
                timestamp = timeAgo(2, 11, 45),
                note = "Daily spending transfer"
            )
        )

        dao.insertTransactions(generatedTransactions)

        // 4. Budgets for current month
        val currentMonthYear = "${cal.get(Calendar.YEAR)}-${String.format("%02d", cal.get(Calendar.MONTH) + 1)}"
        val testBudgets = listOf(
            BudgetEntity(category = "Food", monthlyLimit = 25000.0, monthYear = currentMonthYear, alertThresholdPercent = 80),
            BudgetEntity(category = "Transport", monthlyLimit = 15000.0, monthYear = currentMonthYear, alertThresholdPercent = 80),
            BudgetEntity(category = "Shopping", monthlyLimit = 20000.0, monthYear = currentMonthYear, alertThresholdPercent = 80),
            BudgetEntity(category = "Bills", monthlyLimit = 30000.0, monthYear = currentMonthYear, alertThresholdPercent = 80),
            BudgetEntity(category = "Entertainment", monthlyLimit = 8000.0, monthYear = currentMonthYear, alertThresholdPercent = 80),
            BudgetEntity(category = "Health", monthlyLimit = 10000.0, monthYear = currentMonthYear, alertThresholdPercent = 80)
        )
        dao.insertBudgets(testBudgets)

        // 5. Bill Reminders
        val testBills = listOf(
            BillReminderEntity(
                title = "Electricity Utility",
                amount = 7500.0,
                dueDate = timeAhead(random.nextInt(3, 7), 18),
                category = "Bills",
                isPaid = false,
                paymentMethod = "Bank"
            ),
            BillReminderEntity(
                title = "High-speed Fiber",
                amount = 3500.0,
                dueDate = timeAhead(random.nextInt(7, 14), 12),
                category = "Bills",
                isPaid = false,
                paymentMethod = "Card"
            ),
            BillReminderEntity(
                title = "Apartment Rent",
                amount = 35000.0,
                dueDate = timeAhead(random.nextInt(15, 25), 10),
                category = "Housing",
                isPaid = false,
                paymentMethod = "Bank"
            )
        )
        dao.insertBillReminders(testBills)

        // 6. Savings Goals
        val testGoals = listOf(
            SavingsGoalEntity(
                title = "Emergency Fund",
                targetAmount = 150000.0,
                currentAmount = 65000.0,
                targetDate = "Dec 2026",
                iconName = "SAVINGS"
            ),
            SavingsGoalEntity(
                title = "Cyberpunk Gaming Rig",
                targetAmount = 220000.0,
                currentAmount = 120000.0,
                targetDate = "Nov 2026",
                iconName = "TECH"
            ),
            SavingsGoalEntity(
                title = "Trip to Hunza",
                targetAmount = 60000.0,
                currentAmount = 35000.0,
                targetDate = "Oct 2026",
                iconName = "TRAVEL"
            )
        )
        dao.insertSavingsGoals(testGoals)
    }

    suspend fun seedSampleData() {
        seedRandomTestingData()
    }
}
