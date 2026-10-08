package com.example

import com.example.data.model.CommitteeEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.profile.BudgetPlanner
import com.example.data.model.BillReminderEntity
import com.example.data.model.BudgetEntity
import com.example.data.profile.UserProfile
import com.example.domain.BudgetAlertLogic
import com.example.domain.DAY_MS
import com.example.domain.MoneyMath
import com.example.domain.PeriodKind
import com.example.ui.viewmodel.KharchUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/** Pure logic: the same inputs must always give the same answer. */
class MoneyMathTest {

    private fun at(y: Int, month: Int, d: Int, h: Int = 12): Long =
        Calendar.getInstance().apply { clear(); set(y, month, d, h, 0) }.timeInMillis

    private fun tx(
        type: TransactionType,
        amount: Double,
        ts: Long,
        method: String = "Cash",
        to: String? = null,
        category: String = "Food"
    ) = TransactionEntity(
        title = "t", amount = amount, type = type.name, category = category,
        paymentMethod = method, toPaymentMethod = to, timestamp = ts
    )

    // ---- Accounts ------------------------------------------------------------------------

    @Test
    fun accountBalances_transferMovesMoneyBetweenPlaces() {
        val now = at(2026, Calendar.OCTOBER, 10)
        val list = listOf(
            tx(TransactionType.INCOME, 10000.0, now, "Bank"),
            tx(TransactionType.EXPENSE, 2000.0, now, "Cash"),
            tx(TransactionType.TRANSFER, 3000.0, now, "Bank", "Cash")
        )
        val balances = MoneyMath.accountBalances(list).associate { it.name to it.balance }
        assertEquals(7000.0, balances["Bank"]!!, 0.001)
        assertEquals(1000.0, balances["Cash"]!!, 0.001)
    }

    @Test
    fun totalBalance_isNotHiddenWhenNegative() {
        val now = at(2026, Calendar.OCTOBER, 10)
        val state = KharchUiState(
            transactions = listOf(tx(TransactionType.INCOME, 1000.0, now), tx(TransactionType.EXPENSE, 1800.0, now))
        )
        assertEquals(-800.0, state.totalBalance, 0.001)
    }

    @Test
    fun transfersDoNotChangeTheTotal() {
        val now = at(2026, Calendar.OCTOBER, 10)
        val state = KharchUiState(
            transactions = listOf(tx(TransactionType.INCOME, 5000.0, now, "Bank"), tx(TransactionType.TRANSFER, 2000.0, now, "Bank", "Cash"))
        )
        assertEquals(5000.0, state.totalBalance, 0.001)
        assertEquals(5000.0, state.accounts.sumOf { it.balance }, 0.001)
    }

    // ---- Daily allowance -----------------------------------------------------------------

    @Test
    fun dailyAllowance_sharesTheMonthAcrossRemainingDays() {
        val now = at(2026, Calendar.OCTOBER, 10) // 22 days left including the 10th
        val a = MoneyMath.dailyAllowance(emptyList(), emptyList(), 31000.0, now)
        assertEquals(22, a.daysLeft)
        assertEquals(31000.0 / 22, a.perDay, 0.001)
        assertEquals(a.perDay, a.leftToday, 0.001)
    }

    @Test
    fun dailyAllowance_cheapEarlierDaysMakeTodayBigger() {
        val now = at(2026, Calendar.OCTOBER, 10)
        val spentLittle = listOf(tx(TransactionType.EXPENSE, 100.0, at(2026, Calendar.OCTOBER, 9)))
        val spentALot = listOf(tx(TransactionType.EXPENSE, 9000.0, at(2026, Calendar.OCTOBER, 9)))
        val little = MoneyMath.dailyAllowance(spentLittle, emptyList(), 31000.0, now)
        val lot = MoneyMath.dailyAllowance(spentALot, emptyList(), 31000.0, now)
        assertTrue(little.perDay > lot.perDay)
        assertEquals((31000.0 - 100.0) / 22, little.perDay, 0.001)
    }

    @Test
    fun dailyAllowance_setsAsideUnpaidBillsButNotPaidOnes() {
        val now = at(2026, Calendar.OCTOBER, 10)
        val bills = listOf(
            BillReminderEntity(title = "Rent", amount = 8800.0, dueDate = at(2026, Calendar.OCTOBER, 25), isPaid = false),
            BillReminderEntity(title = "Old", amount = 5000.0, dueDate = at(2026, Calendar.OCTOBER, 2), isPaid = true),
            BillReminderEntity(title = "NextMonth", amount = 7000.0, dueDate = at(2026, Calendar.NOVEMBER, 5), isPaid = false)
        )
        val a = MoneyMath.dailyAllowance(emptyList(), bills, 30800.0, now)
        assertEquals(8800.0, a.reservedForBills, 0.001)
        assertEquals((30800.0 - 8800.0) / 22, a.perDay, 0.001)
    }

    @Test
    fun dailyAllowance_goingOverTodayShowsNegativeLeft() {
        val now = at(2026, Calendar.OCTOBER, 10)
        val spentToday = listOf(tx(TransactionType.EXPENSE, 5000.0, now))
        val a = MoneyMath.dailyAllowance(spentToday, emptyList(), 31000.0, now)
        assertTrue(a.isOverToday)
        assertEquals(a.perDay - 5000.0, a.leftToday, 0.001)
    }

    @Test
    fun daysMoneyWillLast_usesTheLastTwoWeeks() {
        val now = at(2026, Calendar.OCTOBER, 20)
        val spent = listOf(tx(TransactionType.EXPENSE, 14000.0, now - 3 * DAY_MS))
        assertEquals(5, MoneyMath.daysMoneyWillLast(5000.0, spent, now))
        assertNull(MoneyMath.daysMoneyWillLast(5000.0, emptyList(), now))
        assertEquals(0, MoneyMath.daysMoneyWillLast(-10.0, spent, now))
    }

    // ---- Work hours ----------------------------------------------------------------------

    @Test
    fun priceInWork_speaksInMinutesHoursAndDays() {
        val p = UserProfile(monthlyIncome = 20800.0, hoursPerDay = 8, daysPerMonth = 26) // Rs. 100 per hour
        assertEquals("about 30 minutes of work", MoneyMath.priceInWork(50.0, p))
        assertEquals("about 2.5 hours of work", MoneyMath.priceInWork(250.0, p))
        assertEquals("about 1 hour of work", MoneyMath.priceInWork(100.0, p))
        assertEquals("about 2 days of work", MoneyMath.priceInWork(1600.0, p))
        assertNull(MoneyMath.priceInWork(100.0, UserProfile()))
    }

    // ---- Wait list -----------------------------------------------------------------------

    @Test
    fun waitDays_biggerPricesWaitLonger() {
        assertEquals(1, MoneyMath.waitDaysFor(1000.0, 100000.0))
        assertEquals(3, MoneyMath.waitDaysFor(5000.0, 100000.0))
        assertEquals(7, MoneyMath.waitDaysFor(20000.0, 100000.0))
        assertEquals(14, MoneyMath.waitDaysFor(60000.0, 100000.0))
        assertEquals(3, MoneyMath.waitDaysFor(60000.0, 0.0))
    }

    @Test
    fun daysLeftToWait_countsDownAndStopsAtZero() {
        val added = at(2026, Calendar.OCTOBER, 1)
        assertEquals(3, MoneyMath.daysLeftToWait(added, 3, added))
        assertEquals(1, MoneyMath.daysLeftToWait(added, 3, added + 2 * DAY_MS))
        assertEquals(0, MoneyMath.daysLeftToWait(added, 3, added + 3 * DAY_MS))
        assertEquals(0, MoneyMath.daysLeftToWait(added, 3, added + 30 * DAY_MS))
    }

    // ---- Committee -----------------------------------------------------------------------

    @Test
    fun committeeStatus_knowsWhenToPayAndWhenItIsMyTurn() {
        val now = at(2026, Calendar.OCTOBER, 15)
        val c = CommitteeEntity(name = "Office", monthlyAmount = 5000.0, totalMembers = 10, myTurn = 1, startMonth = "2026-10")
        val s = MoneyMath.committeeStatus(c, now)
        assertTrue(s.paymentDue)
        assertTrue(s.myTurnNow)
        assertEquals(50000.0, c.pot, 0.001)

        val paid = MoneyMath.committeeStatus(c.copy(paidMonths = 1, payoutReceived = true), now)
        assertFalse(paid.paymentDue)
        assertFalse(paid.myTurnNow)

        val later = MoneyMath.committeeStatus(c.copy(myTurn = 5), now)
        assertFalse(later.myTurnNow)
    }

    @Test
    fun committeeStatus_notDueBeforeItStarts() {
        val now = at(2026, Calendar.OCTOBER, 15)
        val c = CommitteeEntity(name = "Next", monthlyAmount = 1000.0, totalMembers = 6, myTurn = 3, startMonth = "2026-12")
        assertFalse(MoneyMath.committeeStatus(c, now).paymentDue)
    }

    // ---- Charts --------------------------------------------------------------------------

    @Test
    fun periodRange_weekStartsOnMonday_andMonthAndYearAreCalendarBased() {
        val wednesday = at(2026, Calendar.OCTOBER, 14)
        val week = MoneyMath.periodRange(PeriodKind.WEEK, 0, wednesday)
        assertEquals(at(2026, Calendar.OCTOBER, 12, 0), week.start)
        assertEquals((7 * DAY_MS).toDouble(), (week.endExclusive - week.start).toDouble(), 3600_000.0 * 2)
        val month = MoneyMath.periodRange(PeriodKind.MONTH, -1, wednesday)
        assertEquals(at(2026, Calendar.SEPTEMBER, 1, 0), month.start)
        assertEquals(at(2026, Calendar.OCTOBER, 1, 0), month.endExclusive)
        val year = MoneyMath.periodRange(PeriodKind.YEAR, 0, wednesday)
        assertEquals(at(2026, Calendar.JANUARY, 1, 0), year.start)
        assertEquals("This week", week.label)
        assertEquals("Last month", month.label)
    }

    @Test
    fun summarize_week_hasSevenBars_andNoSpendDaysOnlyCountFinishedDays() {
        val now = at(2026, Calendar.OCTOBER, 14) // Wednesday
        val list = listOf(
            tx(TransactionType.EXPENSE, 300.0, at(2026, Calendar.OCTOBER, 12)), // Monday
            tx(TransactionType.EXPENSE, 700.0, at(2026, Calendar.OCTOBER, 14))  // today
        )
        val s = MoneyMath.summarize(list, MoneyMath.periodRange(PeriodKind.WEEK, 0, now), now)
        assertEquals(7, s.bars.size)
        assertEquals(1000.0, s.spent, 0.001)
        // Only Tuesday is finished and empty. Thursday to Sunday have not happened yet.
        assertEquals(1, s.noSpendDays)
        assertEquals(3, s.daysCounted)
    }

    @Test
    fun summarize_year_hasTwelveBars() {
        val now = at(2026, Calendar.OCTOBER, 14)
        val list = listOf(
            tx(TransactionType.EXPENSE, 100.0, at(2026, Calendar.JANUARY, 5)),
            tx(TransactionType.EXPENSE, 400.0, at(2026, Calendar.OCTOBER, 5))
        )
        val s = MoneyMath.summarize(list, MoneyMath.periodRange(PeriodKind.YEAR, 0, now), now)
        assertEquals(12, s.bars.size)
        assertEquals(100.0, s.bars[0].amount, 0.001)
        assertEquals(400.0, s.bars[9].amount, 0.001)
        assertEquals("Oct", s.busiestBar?.label)
    }

    @Test
    fun summarize_ignoresIncomeAndOtherPeriods() {
        val now = at(2026, Calendar.OCTOBER, 14)
        val list = listOf(
            tx(TransactionType.INCOME, 9999.0, at(2026, Calendar.OCTOBER, 5)),
            tx(TransactionType.EXPENSE, 100.0, at(2026, Calendar.SEPTEMBER, 28)),
            tx(TransactionType.EXPENSE, 250.0, at(2026, Calendar.OCTOBER, 5), category = "Transport")
        )
        val s = MoneyMath.summarize(list, MoneyMath.periodRange(PeriodKind.MONTH, 0, now), now)
        assertEquals(250.0, s.spent, 0.001)
        assertEquals(9999.0, s.income, 0.001)
        assertEquals(1, s.byCategory.size)
        assertEquals(1.0f, s.byCategory[0].share, 0.001f)
    }

    @Test
    fun comparableSpent_comparesTheSameNumberOfDays() {
        val now = at(2026, Calendar.OCTOBER, 10, 23) // 10th day of October is nearly over
        val list = listOf(
            tx(TransactionType.EXPENSE, 100.0, at(2026, Calendar.SEPTEMBER, 3)),  // inside the first 10 days
            tx(TransactionType.EXPENSE, 900.0, at(2026, Calendar.SEPTEMBER, 25)), // later in September: not counted yet
            tx(TransactionType.EXPENSE, 50.0, at(2026, Calendar.OCTOBER, 4))
        )
        assertEquals(100.0, MoneyMath.comparableSpent(list, PeriodKind.MONTH, 0, now), 0.001)
        // October is finished by mid-November, so it is compared with the whole of September (100 + 900)
        val november = at(2026, Calendar.NOVEMBER, 15)
        assertEquals(1000.0, MoneyMath.comparableSpent(list, PeriodKind.MONTH, -1, november), 0.001)
    }

    @Test
    fun greeting_followsTheClockForEveryHour() {
        val expected = mapOf(
            0 to "Good night", 4 to "Good night", 5 to "Good morning", 11 to "Good morning",
            12 to "Good afternoon", 16 to "Good afternoon", 17 to "Good evening", 22 to "Good evening", 23 to "Good night"
        )
        expected.forEach { (hour, words) -> assertEquals("hour $hour", words, MoneyMath.greetingFor(hour)) }
        // Every hour of the day has a greeting and the day is covered without gaps
        assertEquals(24, (0..23).map { MoneyMath.greetingFor(it) }.size)
    }

    @Test
    fun changeText_isSimple() {
        assertEquals("20% more than before", MoneyMath.changeText(120.0, 100.0))
        assertEquals("25% less than before", MoneyMath.changeText(75.0, 100.0))
        assertEquals("same as before", MoneyMath.changeText(100.0, 100.0))
        assertNull(MoneyMath.changeText(100.0, 0.0))
    }

    @Test
    fun noSpendStreak_countsBackFromYesterdayAndStopsAtFirstRecord() {
        val now = at(2026, Calendar.OCTOBER, 14)
        val list = listOf(
            tx(TransactionType.EXPENSE, 100.0, at(2026, Calendar.OCTOBER, 8)),
            tx(TransactionType.EXPENSE, 100.0, at(2026, Calendar.OCTOBER, 14)) // today does not break the streak
        )
        // 9, 10, 11, 12, 13 had no spending
        assertEquals(5, MoneyMath.noSpendStreak(list, now))
        assertEquals(0, MoneyMath.noSpendStreak(emptyList(), now))
        // Days before the very first record are not "no spending" days
        val firstToday = listOf(tx(TransactionType.EXPENSE, 100.0, now))
        assertEquals(0, MoneyMath.noSpendStreak(firstToday, now))
    }

    // ---- First-run plan ------------------------------------------------------------------

    @Test
    fun budgetPlanner_splitsEverydayMoneyBetweenChosenCategories() {
        val profile = UserProfile(monthlyIncome = 100000.0, savePercent = 10, monthlyBills = 30000.0, focusCategories = setOf("Food", "Transport"))
        val plan = BudgetPlanner.suggest(profile)
        assertEquals(90000.0, plan.monthlyLimit, 0.001)
        assertEquals(10000.0, plan.savingsPerMonth, 0.001)
        assertEquals(60000.0, plan.everydayMoney, 0.001)
        assertEquals(37800.0, plan.categoryLimits["Food"]!!, 0.001)
        assertEquals(16200.0, plan.categoryLimits["Transport"]!!, 0.001)
        // Limits together are 90% of everyday money, so there is always a free cushion.
        assertEquals(54000.0, plan.categoryLimits.values.sum(), 0.001)
    }

    @Test
    fun budgetPlanner_usesDefaultCategories_andHandlesNoIncome() {
        val withDefaults = BudgetPlanner.suggest(UserProfile(monthlyIncome = 60000.0))
        assertEquals(setOf("Food", "Transport", "Shopping"), withDefaults.categoryLimits.keys)
        val none = BudgetPlanner.suggest(UserProfile())
        assertEquals(0.0, none.monthlyLimit, 0.001)
        assertTrue(none.categoryLimits.isEmpty())
        // Bills bigger than the income never give negative limits
        val broke = BudgetPlanner.suggest(UserProfile(monthlyIncome = 10000.0, monthlyBills = 50000.0))
        assertEquals(0.0, broke.everydayMoney, 0.001)
    }

    // ---- Budget alerts -------------------------------------------------------------------

    private fun stateWithFoodLimit(spent: Double): KharchUiState {
        val now = System.currentTimeMillis()
        return KharchUiState(
            budgets = listOf(BudgetEntity(category = "Food", monthlyLimit = 10000.0, monthYear = MoneyMath.monthKey(now), alertThresholdPercent = 80)),
            transactions = if (spent > 0) listOf(tx(TransactionType.EXPENSE, spent, now)) else emptyList()
        )
    }

    @Test
    fun budgetAlert_firesOnlyWhenALineIsCrossed() {
        // 7,000 -> 8,500 crosses the 80% line
        val warn = BudgetAlertLogic.alertsFor(stateWithFoodLimit(7000.0), "Food", 1500.0)
        assertEquals(1, warn.size)
        assertTrue(warn[0].title.contains("almost full"))
        // 8,500 -> 8,700 is already past the line: stay quiet
        assertTrue(BudgetAlertLogic.alertsFor(stateWithFoodLimit(8500.0), "Food", 200.0).isEmpty())
        // 9,500 -> 10,500 goes over the limit
        val over = BudgetAlertLogic.alertsFor(stateWithFoodLimit(9500.0), "Food", 1000.0)
        assertEquals(1, over.size)
        assertTrue(over[0].title.contains("over"))
        // After it is over, another expense does not repeat the same message
        assertTrue(BudgetAlertLogic.alertsFor(stateWithFoodLimit(10500.0), "Food", 100.0).isEmpty())
        // Other categories without a limit do not alert
        assertTrue(BudgetAlertLogic.alertsFor(stateWithFoodLimit(0.0), "Transport", 99999.0).isEmpty())
    }

    @Test
    fun budgetAlert_watchesTheWholeMonthToo() {
        val now = System.currentTimeMillis()
        val state = KharchUiState(
            budgets = listOf(BudgetEntity(category = "OVERALL", monthlyLimit = 20000.0, monthYear = MoneyMath.monthKey(now))),
            transactions = listOf(tx(TransactionType.EXPENSE, 15000.0, now))
        )
        val alerts = BudgetAlertLogic.alertsFor(state, "Food", 2000.0) // 15,000 -> 17,000 = 85%
        assertEquals(1, alerts.size)
        assertNotNull(alerts.firstOrNull { it.title.contains("almost used up") })
    }
}
