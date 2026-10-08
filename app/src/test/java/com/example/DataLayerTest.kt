package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.KharchDatabase
import com.example.data.model.BillReminderEntity
import com.example.data.model.BudgetEntity
import com.example.data.model.CommitteeEntity
import com.example.data.model.DebtEntity
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.WishItemEntity
import com.example.data.model.WishStatus
import com.example.data.profile.ProfileStore
import com.example.data.profile.UserProfile
import com.example.data.repository.KharchRepository
import com.example.ui.viewmodel.KharchViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.Calendar

/** Saving and loading real records, including the "what happens when..." rules. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DataLayerTest {

    private lateinit var db: KharchDatabase
    private lateinit var repo: KharchRepository

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, KharchDatabase::class.java).allowMainThreadQueries().build()
        repo = KharchRepository(db.kharchDao())
    }

    @After
    fun tearDown() = db.close()

    /** The ViewModel works on the main thread, which Robolectric keeps paused. This lets it finish its work. */
    private fun settle() {
        repeat(25) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(40)
        }
    }

    private fun monthsFromNow(m: Int) = Calendar.getInstance().apply { add(Calendar.MONTH, m) }.timeInMillis

    // ---- Bills ---------------------------------------------------------------------------

    @Test
    fun payingAMonthlyBill_logsSpending_andCreatesNextMonthsBill() = runBlocking {
        val due = System.currentTimeMillis()
        repo.insertBillReminder(BillReminderEntity(title = "Rent", amount = 30000.0, dueDate = due, repeatMonthly = true))
        val bill = repo.allBillReminders.first().single()

        repo.markBillAsPaid(bill)

        val bills = repo.allBillReminders.first()
        assertEquals(2, bills.size)
        assertEquals(1, bills.count { it.isPaid })
        val next = bills.first { !it.isPaid }
        assertEquals(30000.0, next.amount, 0.001)
        assertTrue(next.repeatMonthly)
        val expectedNext = Calendar.getInstance().apply { timeInMillis = due; add(Calendar.MONTH, 1) }.timeInMillis
        assertEquals(expectedNext, next.dueDate)
        val spent = repo.allTransactions.first().filter { it.type == TransactionType.EXPENSE.name }
        assertEquals(1, spent.size)
        assertEquals(30000.0, spent[0].amount, 0.001)
        assertEquals("Rent", spent[0].title)
    }

    @Test
    fun doubleTappingPaid_logsOnlyOnce() = runBlocking {
        repo.insertBillReminder(BillReminderEntity(title = "Internet", amount = 3000.0, dueDate = System.currentTimeMillis()))
        val stale = repo.allBillReminders.first().single()
        repo.markBillAsPaid(stale)
        repo.markBillAsPaid(stale) // the screen still holds the old copy that says "not paid"
        assertEquals(1, repo.allTransactions.first().size)
    }

    @Test
    fun doubleTappingCommitteePayment_logsOnlyOneMonth() = runBlocking {
        repo.addCommittee(CommitteeEntity(name = "C", monthlyAmount = 1000.0, totalMembers = 5, myTurn = 2, startMonth = com.example.domain.MoneyMath.monthKey(System.currentTimeMillis())))
        val stale = repo.allCommittees.first().single()
        repo.payCommitteeMonth(stale)
        repo.payCommitteeMonth(stale)
        assertEquals(1, repo.allCommittees.first().single().paidMonths) // the second tap is for next month, which has not started
    }
    @Test
    fun aOneTimeBill_doesNotRepeat() = runBlocking {
        repo.insertBillReminder(BillReminderEntity(title = "Fine", amount = 500.0, dueDate = System.currentTimeMillis(), repeatMonthly = false))
        repo.markBillAsPaid(repo.allBillReminders.first().single())
        assertEquals(1, repo.allBillReminders.first().size)
    }

    // ---- Goals ---------------------------------------------------------------------------

    @Test
    fun goalNeverGoesAboveItsTarget_andOnlyRealMoneyIsLogged() = runBlocking {
        repo.insertSavingsGoal(SavingsGoalEntity(title = "Phone", targetAmount = 1000.0, currentAmount = 900.0))
        val goal = repo.allSavingsGoals.first().single()

        val added = repo.depositToSavingsGoal(goal, 500.0)

        assertEquals(100.0, added, 0.001)
        assertEquals(1000.0, repo.allSavingsGoals.first().single().currentAmount, 0.001)
        val logged = repo.allTransactions.first().single()
        assertEquals(100.0, logged.amount, 0.001)
        assertEquals("Savings", logged.category)
    }

    @Test
    fun aFullGoalAcceptsNothing() = runBlocking {
        repo.insertSavingsGoal(SavingsGoalEntity(title = "Done", targetAmount = 1000.0, currentAmount = 1000.0))
        val goal = repo.allSavingsGoals.first().single()
        assertEquals(0.0, repo.depositToSavingsGoal(goal, 50.0), 0.001)
        assertTrue(repo.allTransactions.first().isEmpty())
    }

    // ---- Udhaar --------------------------------------------------------------------------

    @Test
    fun udhaar_partPayments_addUp_andNeverGoBelowZero() = runBlocking {
        repo.addDebt(DebtEntity(person = "Ali", amount = 1000.0, direction = "LENT"))
        var d = db.kharchDao().getAllDebts().first().single()
        repo.payDebt(d, 400.0)
        d = db.kharchDao().getAllDebts().first().single()
        assertEquals(600.0, d.remaining, 0.001)
        assertFalse(d.isSettled)
        repo.payDebt(d, 5000.0)
        d = db.kharchDao().getAllDebts().first().single()
        assertEquals(0.0, d.remaining, 0.001)
        assertTrue(d.isSettled)
    }

    // ---- Committee -----------------------------------------------------------------------

    @Test
    fun committee_paymentsAndPayout_areLoggedOnce() = runBlocking {
        repo.addCommittee(CommitteeEntity(name = "Office", monthlyAmount = 5000.0, totalMembers = 2, myTurn = 1, startMonth = com.example.domain.MoneyMath.monthKey(monthsFromNow(-1))))
        var c = repo.allCommittees.first().single()

        repo.payCommitteeMonth(c)
        c = repo.allCommittees.first().single()
        assertEquals(1, c.paidMonths)
        repo.payCommitteeMonth(c)
        c = repo.allCommittees.first().single()
        assertEquals(2, c.paidMonths)
        repo.payCommitteeMonth(c) // all months are paid: nothing more to log
        c = repo.allCommittees.first().single()
        assertEquals(2, c.paidMonths)

        repo.receiveCommitteePayout(c)
        c = repo.allCommittees.first().single()
        assertTrue(c.payoutReceived)
        repo.receiveCommitteePayout(c) // second try does nothing

        val all = repo.allTransactions.first()
        assertEquals(2, all.count { it.type == TransactionType.EXPENSE.name && it.category == "Committee" })
        val income = all.filter { it.type == TransactionType.INCOME.name }
        assertEquals(1, income.size)
        assertEquals(10000.0, income[0].amount, 0.001)
        assertTrue(c.isFinished)
    }

    // ---- Wish list -----------------------------------------------------------------------

    @Test
    fun wishList_buyingLogsSpending_droppingDoesNot() = runBlocking {
        repo.addWishItem(WishItemEntity(title = "Headphones", price = 8000.0, waitDays = 3))
        repo.addWishItem(WishItemEntity(title = "Jacket", price = 6000.0, waitDays = 3))
        val items = repo.allWishItems.first()
        repo.buyWishItem(items.first { it.title == "Headphones" }, "Technology", "Card")
        repo.dropWishItem(items.first { it.title == "Jacket" })

        val after = repo.allWishItems.first().associateBy { it.title }
        assertEquals(WishStatus.BOUGHT.name, after["Headphones"]!!.status)
        assertEquals(WishStatus.DROPPED.name, after["Jacket"]!!.status)
        val spent = repo.allTransactions.first()
        assertEquals(1, spent.size)
        assertEquals(8000.0, spent[0].amount, 0.001)
    }

    // ---- Deleting ------------------------------------------------------------------------

    private fun expense(ts: Long) = TransactionEntity(title = "x", amount = 10.0, type = TransactionType.EXPENSE.name, category = "Food", paymentMethod = "Cash", timestamp = ts)

    @Test
    fun deletingAMonth_removesOnlyThatMonth() = runBlocking {
        val thisMonth = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 15) }.timeInMillis
        val lastMonth = monthsFromNow(-1)
        val twoMonthsAgo = monthsFromNow(-2)
        repo.insertTransaction(expense(thisMonth))
        repo.insertTransaction(expense(lastMonth))
        repo.insertTransaction(expense(twoMonthsAgo))

        val last = Calendar.getInstance().apply { timeInMillis = lastMonth }
        repo.deleteTransactionsInMonth(last.get(Calendar.YEAR), last.get(Calendar.MONTH))

        val left = repo.allTransactions.first()
        assertEquals(2, left.size)
        assertTrue(left.none { it.timestamp == lastMonth })
    }

    @Test
    fun deletingAllRecords_keepsLimitsBillsAndGoals() = runBlocking {
        repo.insertTransaction(expense(System.currentTimeMillis()))
        repo.insertBudget(BudgetEntity(category = "Food", monthlyLimit = 100.0, monthYear = "2026-10"))
        repo.insertBillReminder(BillReminderEntity(title = "Rent", amount = 1.0, dueDate = 0))
        repo.deleteAllTransactions()
        assertTrue(repo.allTransactions.first().isEmpty())
        assertEquals(1, repo.allBudgets.first().size)
        assertEquals(1, repo.allBillReminders.first().size)
    }

    @Test
    fun deletingEverything_emptiesEveryTable_andForgetsTheAnswers() = runBlocking {
        repo.insertTransaction(expense(System.currentTimeMillis()))
        repo.insertBudget(BudgetEntity(category = "Food", monthlyLimit = 100.0, monthYear = "2026-10"))
        repo.insertBillReminder(BillReminderEntity(title = "Rent", amount = 1.0, dueDate = 0))
        repo.insertSavingsGoal(SavingsGoalEntity(title = "g", targetAmount = 1.0, currentAmount = 0.0))
        repo.addWishItem(WishItemEntity(title = "w", price = 1.0, waitDays = 1))
        repo.addDebt(DebtEntity(person = "p", amount = 1.0, direction = "LENT"))
        repo.addCommittee(CommitteeEntity(name = "c", monthlyAmount = 1.0, totalMembers = 2, myTurn = 1, startMonth = "2026-10"))

        val store = ProfileStore.memory().also { it.save(UserProfile(onboardingDone = true, name = "Ali")) }
        val vm = KharchViewModel(repo, store)
        vm.deleteEverything()
        settle()

        assertTrue(repo.allTransactions.first().isEmpty())
        assertTrue(repo.allBudgets.first().isEmpty())
        assertTrue(repo.allBillReminders.first().isEmpty())
        assertTrue(repo.allSavingsGoals.first().isEmpty())
        assertTrue(repo.allWishItems.first().isEmpty())
        assertTrue(repo.allDebts.first().isEmpty())
        assertTrue(repo.allCommittees.first().isEmpty())
        assertFalse(store.profile.value.onboardingDone)
        assertEquals("", store.profile.value.name)
    }

    // ---- First-run answers ---------------------------------------------------------------

    @Test
    fun completingTheQuestions_createsTheLimitsAndTheMonthlyBill() = runBlocking {
        val store = ProfileStore.memory()
        val vm = KharchViewModel(repo, store)
        vm.completeOnboarding(
            UserProfile(monthlyIncome = 100000.0, savePercent = 10, monthlyBills = 30000.0, billsDay = 5, focusCategories = setOf("Food", "Transport"))
        )
        settle()

        assertTrue(store.profile.value.onboardingDone)
        val budgets = repo.allBudgets.first().associate { it.category to it.monthlyLimit }
        assertEquals(90000.0, budgets["OVERALL"]!!, 0.001)
        assertEquals(37800.0, budgets["Food"]!!, 0.001)
        assertEquals(16200.0, budgets["Transport"]!!, 0.001)
        val bill = repo.allBillReminders.first().single()
        assertEquals(30000.0, bill.amount, 0.001)
        assertTrue(bill.repeatMonthly)
        assertTrue(bill.dueDate > System.currentTimeMillis())
        val day = Calendar.getInstance().apply { timeInMillis = bill.dueDate }.get(Calendar.DAY_OF_MONTH)
        assertEquals(5, day)
    }

    @Test
    fun answeringAgain_changesTheLimitsInsteadOfDuplicatingThem() = runBlocking {
        val store = ProfileStore.memory()
        val vm = KharchViewModel(repo, store)
        vm.completeOnboarding(UserProfile(monthlyIncome = 100000.0, savePercent = 0, focusCategories = setOf("Food")))
        settle()
        vm.completeOnboarding(UserProfile(monthlyIncome = 50000.0, savePercent = 0, focusCategories = setOf("Food")))
        settle()
        val budgets = repo.allBudgets.first()
        assertEquals(2, budgets.size)
        assertEquals(50000.0, budgets.first { it.category == "OVERALL" }.monthlyLimit, 0.001)
    }

    // ---- Settings are remembered ---------------------------------------------------------

    @Test
    fun profileIsSavedAndLoadedAgain() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val first = ProfileStore.from(ctx)
        first.reset()
        first.save(UserProfile(onboardingDone = true, name = "Sara", monthlyIncome = 85000.0, savePercent = 20, focusCategories = setOf("Food", "Health"), billsDay = 10, dailyReminder = true))
        val second = ProfileStore.from(ctx).profile.value
        assertTrue(second.onboardingDone)
        assertEquals("Sara", second.name)
        assertEquals(85000.0, second.monthlyIncome, 0.01)
        assertEquals(20, second.savePercent)
        assertEquals(setOf("Food", "Health"), second.focusCategories)
        assertEquals(10, second.billsDay)
        assertTrue(second.dailyReminder)
        first.reset()
    }

    // ---- Upgrading an old database -------------------------------------------------------

    @Test
    fun upgradingFromVersion4_keepsRecords_andAddsNewTables() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val name = "upgrade_test.db"
        ctx.deleteDatabase(name)
        val file = ctx.getDatabasePath(name).also { it.parentFile?.mkdirs() }

        // Build a database exactly like version 4 of the app made it.
        SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            old.execSQL("CREATE TABLE IF NOT EXISTS `transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `amount` REAL NOT NULL, `type` TEXT NOT NULL, `category` TEXT NOT NULL, `paymentMethod` TEXT NOT NULL, `toPaymentMethod` TEXT, `timestamp` INTEGER NOT NULL, `note` TEXT NOT NULL, `receiptUri` TEXT)")
            old.execSQL("CREATE TABLE IF NOT EXISTS `budgets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `category` TEXT NOT NULL, `monthlyLimit` REAL NOT NULL, `monthYear` TEXT NOT NULL, `alertThresholdPercent` INTEGER NOT NULL)")
            old.execSQL("CREATE TABLE IF NOT EXISTS `savings_goals` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `targetAmount` REAL NOT NULL, `currentAmount` REAL NOT NULL, `targetDate` TEXT NOT NULL, `iconName` TEXT NOT NULL)")
            old.execSQL("CREATE TABLE IF NOT EXISTS `bill_reminders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `amount` REAL NOT NULL, `dueDate` INTEGER NOT NULL, `category` TEXT NOT NULL, `isPaid` INTEGER NOT NULL, `paymentMethod` TEXT NOT NULL)")
            old.execSQL("CREATE TABLE IF NOT EXISTS `expenses` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `amount` REAL NOT NULL, `category` TEXT NOT NULL, `date` INTEGER NOT NULL, `description` TEXT NOT NULL, `categoryId` INTEGER)")
            old.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_categoryId` ON `expenses` (`categoryId`)")
            old.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_date` ON `expenses` (`date`)")
            old.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_category` ON `expenses` (`category`)")
            old.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `icon` TEXT NOT NULL, `colorHex` TEXT NOT NULL, `budgetLimit` REAL, `isDefault` INTEGER NOT NULL)")
            old.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            old.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'd65e94032dbe3b318d340493e4276379')")
            old.execSQL("INSERT INTO transactions (title, amount, type, category, paymentMethod, timestamp, note) VALUES ('Lunch', 450.0, 'EXPENSE', 'Food', 'Cash', 1000, '')")
            old.execSQL("INSERT INTO budgets (category, monthlyLimit, monthYear, alertThresholdPercent) VALUES ('Food', 9000.0, '2026-10', 80)")
            old.execSQL("INSERT INTO bill_reminders (title, amount, dueDate, category, isPaid, paymentMethod) VALUES ('Rent', 20000.0, 2000, 'Bills', 0, 'Bank')")
            old.execSQL("INSERT INTO expenses (amount, category, date, description) VALUES (450.0, 'Food', 1000, 'Lunch')")
            old.version = 4
        }

        val upgraded = Room.databaseBuilder(ctx, KharchDatabase::class.java, name)
            .addMigrations(KharchDatabase.MIGRATION_4_5)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = upgraded.kharchDao()
            assertEquals("Lunch", dao.getAllTransactionsOnce().single().title)
            assertEquals(9000.0, dao.getAllBudgetsOnce().single().monthlyLimit, 0.001)
            val bill = dao.getAllBillRemindersOnce().single()
            assertEquals("Rent", bill.title)
            assertFalse(bill.repeatMonthly) // old bills get the default
            // The new tables exist and work
            dao.insertDebt(DebtEntity(person = "Ali", amount = 10.0, direction = "LENT"))
            dao.insertWishItem(WishItemEntity(title = "x", price = 1.0, waitDays = 1))
            dao.insertCommittee(CommitteeEntity(name = "c", monthlyAmount = 1.0, totalMembers = 2, myTurn = 1, startMonth = "2026-10"))
            assertEquals(1, dao.getAllDebts().first().size)
            assertEquals(1, dao.getAllWishItems().first().size)
            assertEquals(1, dao.getAllCommittees().first().size)
        } finally {
            upgraded.close()
            ctx.deleteDatabase(name)
        }
    }
}
