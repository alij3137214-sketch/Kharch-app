package com.example.data.repository

import com.example.data.db.KharchDao
import com.example.data.model.BillReminderEntity
import com.example.data.model.BudgetEntity
import com.example.data.model.CommitteeEntity
import com.example.data.model.DebtEntity
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.WishItemEntity
import com.example.data.model.WishStatus
import com.example.domain.MoneyMath
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class KharchRepository(private val dao: KharchDao) {

    val allTransactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()
    val allBudgets: Flow<List<BudgetEntity>> = dao.getAllBudgets()
    val allSavingsGoals: Flow<List<SavingsGoalEntity>> = dao.getAllSavingsGoals()
    val allBillReminders: Flow<List<BillReminderEntity>> = dao.getAllBillReminders()
    val allWishItems: Flow<List<WishItemEntity>> = dao.getAllWishItems()
    val allDebts: Flow<List<DebtEntity>> = dao.getAllDebts()
    val allCommittees: Flow<List<CommitteeEntity>> = dao.getAllCommittees()

    // ---- Transactions ----------------------------------------------------------------------

    suspend fun getTransactionById(id: Long): TransactionEntity? = dao.getTransactionById(id)

    suspend fun insertTransaction(transaction: TransactionEntity): Long = dao.insertTransaction(transaction)

    suspend fun updateTransaction(transaction: TransactionEntity) = dao.updateTransaction(transaction)

    suspend fun deleteTransaction(transaction: TransactionEntity) = dao.deleteTransaction(transaction)

    suspend fun deleteTransactionById(id: Long) = dao.deleteTransactionById(id)

    // ---- Budgets ---------------------------------------------------------------------------

    /** The limits as saved right now (not a cached copy). */
    suspend fun budgetsNow(): List<BudgetEntity> = dao.getAllBudgetsOnce()

    suspend fun billsNow(): List<BillReminderEntity> = dao.getAllBillRemindersOnce()

    suspend fun insertBudget(budget: BudgetEntity) = dao.insertBudget(budget)

    suspend fun insertBudgets(budgets: List<BudgetEntity>) = dao.insertBudgets(budgets)

    suspend fun updateBudget(budget: BudgetEntity) = dao.updateBudget(budget)

    suspend fun deleteBudget(budget: BudgetEntity) = dao.deleteBudget(budget)

    suspend fun clearAllBudgets() = dao.clearAllBudgets()

    // ---- Savings goals ---------------------------------------------------------------------

    suspend fun insertSavingsGoal(goal: SavingsGoalEntity) = dao.insertSavingsGoal(goal)

    suspend fun updateSavingsGoal(goal: SavingsGoalEntity) = dao.updateSavingsGoal(goal)

    suspend fun deleteSavingsGoal(goal: SavingsGoalEntity) = dao.deleteSavingsGoal(goal)

    /**
     * Puts money into a goal. The goal never goes above its target, and the money that
     * actually went in is logged as spending, so what is saved matches what is recorded.
     * Returns the amount that was really added.
     */
    suspend fun depositToSavingsGoal(staleGoal: SavingsGoalEntity, amount: Double, paymentMethod: String = "Bank"): Double {
        // Always work from the latest saved copy, so a quick double tap cannot add the money twice.
        val goal = dao.getSavingsGoalById(staleGoal.id) ?: return 0.0
        val room = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
        val added = amount.coerceIn(0.0, room)
        if (added <= 0.0) return 0.0
        dao.updateSavingsGoal(goal.copy(currentAmount = goal.currentAmount + added))
        dao.insertTransaction(
            TransactionEntity(
                title = "Saved for ${goal.title}",
                amount = added,
                type = TransactionType.EXPENSE.name,
                category = "Savings",
                paymentMethod = paymentMethod,
                note = "Put into goal"
            )
        )
        return added
    }

    // ---- Bills -----------------------------------------------------------------------------

    suspend fun insertBillReminder(bill: BillReminderEntity) = dao.insertBillReminder(bill)

    suspend fun updateBillReminder(bill: BillReminderEntity) = dao.updateBillReminder(bill)

    suspend fun deleteBillReminder(bill: BillReminderEntity) = dao.deleteBillReminder(bill)

    /** Marks a bill paid and logs it as spending. A monthly bill is created again for next month. */
    suspend fun markBillAsPaid(staleBill: BillReminderEntity) {
        // Use the latest saved copy: a quick double tap on "Paid" must not log the payment twice.
        val bill = dao.getBillReminderById(staleBill.id) ?: return
        if (bill.isPaid) return
        dao.updateBillReminder(bill.copy(isPaid = true))
        dao.insertTransaction(
            TransactionEntity(
                title = bill.title,
                amount = bill.amount,
                type = TransactionType.EXPENSE.name,
                category = bill.category,
                paymentMethod = bill.paymentMethod,
                timestamp = System.currentTimeMillis(),
                note = "Paid bill"
            )
        )
        if (bill.repeatMonthly) {
            val next = Calendar.getInstance().apply {
                timeInMillis = bill.dueDate
                add(Calendar.MONTH, 1)
            }.timeInMillis
            dao.insertBillReminder(bill.copy(id = 0, dueDate = next, isPaid = false))
        }
    }

    // ---- Wish list -------------------------------------------------------------------------

    suspend fun addWishItem(item: WishItemEntity) = dao.insertWishItem(item)

    suspend fun dropWishItem(staleItem: WishItemEntity) {
        val item = dao.getWishItemById(staleItem.id) ?: return
        if (item.status != WishStatus.WAITING.name) return
        dao.updateWishItem(item.copy(status = WishStatus.DROPPED.name))
    }

    /** The person decided to buy it after waiting. This logs the spending. */
    suspend fun buyWishItem(staleItem: WishItemEntity, category: String, paymentMethod: String) {
        val item = dao.getWishItemById(staleItem.id) ?: return
        if (item.status != WishStatus.WAITING.name) return
        dao.updateWishItem(item.copy(status = WishStatus.BOUGHT.name))
        dao.insertTransaction(
            TransactionEntity(
                title = item.title,
                amount = item.price,
                type = TransactionType.EXPENSE.name,
                category = category,
                paymentMethod = paymentMethod,
                note = "From my wish list"
            )
        )
    }

    suspend fun deleteWishItem(item: WishItemEntity) = dao.deleteWishItem(item)

    // ---- Udhaar ----------------------------------------------------------------------------

    suspend fun addDebt(debt: DebtEntity) = dao.insertDebt(debt)

    /** Records that some (or all) of the money was given back. */
    suspend fun payDebt(staleDebt: DebtEntity, amount: Double) {
        val debt = dao.getDebtById(staleDebt.id) ?: return
        val paid = (debt.paidAmount + amount.coerceAtLeast(0.0)).coerceAtMost(debt.amount)
        dao.updateDebt(debt.copy(paidAmount = paid))
    }

    suspend fun deleteDebt(debt: DebtEntity) = dao.deleteDebt(debt)

    // ---- Committees ------------------------------------------------------------------------

    suspend fun addCommittee(committee: CommitteeEntity) = dao.insertCommittee(committee)

    /** Logs this month's committee payment as spending. */
    suspend fun payCommitteeMonth(staleCommittee: CommitteeEntity, paymentMethod: String = "Cash") {
        val committee = dao.getCommitteeById(staleCommittee.id) ?: return
        // Only a month that has already started can be paid, so a double tap cannot pay next month too.
        if (committee.paidMonths >= committee.totalMembers || !MoneyMath.committeeStatus(committee).paymentDue) return
        dao.updateCommittee(committee.copy(paidMonths = committee.paidMonths + 1))
        dao.insertTransaction(
            TransactionEntity(
                title = "${committee.name} committee",
                amount = committee.monthlyAmount,
                type = TransactionType.EXPENSE.name,
                category = "Committee",
                paymentMethod = paymentMethod,
                note = "Month ${committee.paidMonths + 1} of ${committee.totalMembers}"
            )
        )
    }

    /** Logs the pot the person received on their turn as income. */
    suspend fun receiveCommitteePayout(staleCommittee: CommitteeEntity, paymentMethod: String = "Cash") {
        val committee = dao.getCommitteeById(staleCommittee.id) ?: return
        if (committee.payoutReceived || !MoneyMath.committeeStatus(committee).myTurnNow) return
        dao.updateCommittee(committee.copy(payoutReceived = true))
        dao.insertTransaction(
            TransactionEntity(
                title = "${committee.name} committee payout",
                amount = committee.pot,
                type = TransactionType.INCOME.name,
                category = "Committee",
                paymentMethod = paymentMethod,
                note = "My turn"
            )
        )
    }

    suspend fun deleteCommittee(committee: CommitteeEntity) = dao.deleteCommittee(committee)

    // ---- Removing data ---------------------------------------------------------------------

    /** Deletes every transaction in one calendar month. [month] is 0-based, like Calendar.MONTH. */
    suspend fun deleteTransactionsInMonth(year: Int, month: Int) {
        val start = Calendar.getInstance().apply {
            clear()
            set(year, month, 1, 0, 0, 0)
        }
        val end = (start.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
        dao.deleteTransactionsBetween(start.timeInMillis, end.timeInMillis)
    }

    suspend fun deleteAllTransactions() = dao.clearAllTransactions()

    suspend fun clearAllData() {
        dao.clearAllTransactions()
        dao.clearAllBudgets()
        dao.clearAllSavingsGoals()
        dao.clearAllBillReminders()
        dao.clearAllWishItems()
        dao.clearAllDebts()
        dao.clearAllCommittees()
    }

    // ---- Sample data (for trying the app out, and for tests) -------------------------------

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

        val generated = mutableListOf<TransactionEntity>()
        val primaryIncome = listOf(95000.0, 120000.0, 145000.0, 85000.0)[random.nextInt(4)]
        val freelanceAmt = listOf(22000.0, 35000.0, 18000.0, 40000.0)[random.nextInt(4)]

        generated.add(
            TransactionEntity(
                title = "Primary Tech Salary", amount = primaryIncome, type = TransactionType.INCOME.name,
                category = "Salary", paymentMethod = "Bank", timestamp = timeAgo(random.nextInt(1, 4), 10, 0),
                note = "Monthly direct deposit"
            )
        )
        generated.add(
            TransactionEntity(
                title = "Freelance Consulting Payout", amount = freelanceAmt, type = TransactionType.INCOME.name,
                category = "Freelance", paymentMethod = "Easypaisa", timestamp = timeAgo(random.nextInt(3, 7), 16, 30),
                note = "Milestone payout"
            )
        )

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
            generated.add(
                TransactionEntity(
                    title = title, amount = amt, type = TransactionType.EXPENSE.name, category = cat,
                    paymentMethod = paymentMethods[random.nextInt(paymentMethods.size)],
                    timestamp = timeAgo(random.nextInt(0, 11), random.nextInt(9, 22), random.nextInt(0, 59)),
                    note = "Sample record"
                )
            )
        }
        generated.add(
            TransactionEntity(
                title = "Transfer to Spending Wallet", amount = (random.nextInt(30, 80) * 100).toDouble(),
                type = TransactionType.TRANSFER.name, category = "Transfer", paymentMethod = "Bank",
                toPaymentMethod = "Easypaisa", timestamp = timeAgo(2, 11, 45), note = "Daily spending transfer"
            )
        )
        dao.insertTransactions(generated)

        val monthYear = "${cal.get(Calendar.YEAR)}-${String.format("%02d", cal.get(Calendar.MONTH) + 1)}"
        dao.insertBudgets(
            listOf(
                BudgetEntity(category = "Food", monthlyLimit = 25000.0, monthYear = monthYear),
                BudgetEntity(category = "Transport", monthlyLimit = 15000.0, monthYear = monthYear),
                BudgetEntity(category = "Shopping", monthlyLimit = 20000.0, monthYear = monthYear),
                BudgetEntity(category = "Bills", monthlyLimit = 30000.0, monthYear = monthYear),
                BudgetEntity(category = "Entertainment", monthlyLimit = 8000.0, monthYear = monthYear),
                BudgetEntity(category = "Health", monthlyLimit = 10000.0, monthYear = monthYear)
            )
        )
        dao.insertBillReminders(
            listOf(
                BillReminderEntity(title = "Electricity", amount = 7500.0, dueDate = timeAhead(random.nextInt(3, 7), 18), category = "Bills", paymentMethod = "Bank", repeatMonthly = true),
                BillReminderEntity(title = "Internet", amount = 3500.0, dueDate = timeAhead(random.nextInt(7, 14), 12), category = "Bills", paymentMethod = "Card", repeatMonthly = true),
                BillReminderEntity(title = "Rent", amount = 35000.0, dueDate = timeAhead(random.nextInt(15, 25), 10), category = "Housing", paymentMethod = "Bank", repeatMonthly = true)
            )
        )
        dao.insertSavingsGoals(
            listOf(
                SavingsGoalEntity(title = "Emergency Fund", targetAmount = 150000.0, currentAmount = 65000.0, targetDate = "Dec 2026", iconName = "SAVINGS"),
                SavingsGoalEntity(title = "New laptop", targetAmount = 220000.0, currentAmount = 120000.0, targetDate = "Nov 2026", iconName = "TECH"),
                SavingsGoalEntity(title = "Trip to Hunza", targetAmount = 60000.0, currentAmount = 35000.0, targetDate = "Oct 2026", iconName = "TRAVEL")
            )
        )
    }

    suspend fun seedSampleData() = seedRandomTestingData()
}
