package com.example.domain

import com.example.data.model.BillReminderEntity
import com.example.data.model.CommitteeEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.profile.UserProfile
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

const val DAY_MS = 86_400_000L

data class AccountBalance(val name: String, val balance: Double)

/** How much can be spent today, and how much of that is still left. */
data class DailyAllowance(
    val monthLimit: Double,
    /** Money for the month that is already promised to unpaid bills. */
    val reservedForBills: Double,
    val daysLeft: Int,
    /** Money for each remaining day, including today. Grows when earlier days were cheap. */
    val perDay: Double,
    val spentToday: Double,
    val leftToday: Double
) {
    val isOverToday: Boolean get() = leftToday < 0
}

enum class PeriodKind { WEEK, MONTH, YEAR }

data class PeriodRange(val kind: PeriodKind, val start: Long, val endExclusive: Long, val label: String)

data class DayTotal(val dayStart: Long, val label: String, val amount: Double)

data class CategoryTotal(val category: String, val amount: Double, val share: Float)

data class PeriodSummary(
    val range: PeriodRange,
    val spent: Double,
    val income: Double,
    val transactionCount: Int,
    val byCategory: List<CategoryTotal>,
    /** One bar per day (week and month) or per month (year). */
    val bars: List<DayTotal>,
    val biggestExpense: TransactionEntity?,
    val busiestBar: DayTotal?,
    /** Days with no spending, only counting days that are over. */
    val noSpendDays: Int,
    val daysCounted: Int
) {
    val averagePerDay: Double get() = if (daysCounted > 0) spent / daysCounted else 0.0
}

object MoneyMath {

    // ---- Greeting --------------------------------------------------------------------------

    /** The words for the time of day. [hour] is 0 to 23 on the phone's clock. */
    fun greetingFor(hour: Int): String = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..22 -> "Good evening"
        else -> "Good night"
    }

    // ---- Dates -----------------------------------------------------------------------------

    fun startOfDay(timestamp: Long): Long = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun startOfMonth(timestamp: Long): Long = Calendar.getInstance().apply {
        timeInMillis = startOfDay(timestamp)
        set(Calendar.DAY_OF_MONTH, 1)
    }.timeInMillis

    fun monthKey(timestamp: Long): String = SimpleDateFormat("yyyy-MM", Locale.US).format(timestamp)

    private fun addDays(timestamp: Long, days: Int): Long =
        Calendar.getInstance().apply { timeInMillis = timestamp; add(Calendar.DAY_OF_YEAR, days) }.timeInMillis

    // ---- Where the money is ----------------------------------------------------------------

    /** Money in each place (Cash, Bank, Easypaisa...). A transfer moves money from one place to another. */
    fun accountBalances(transactions: List<TransactionEntity>): List<AccountBalance> {
        val map = linkedMapOf<String, Double>()
        fun add(name: String, delta: Double) {
            map[name] = (map[name] ?: 0.0) + delta
        }
        for (tx in transactions) {
            when (tx.type) {
                TransactionType.INCOME.name -> add(tx.paymentMethod, tx.amount)
                TransactionType.EXPENSE.name -> add(tx.paymentMethod, -tx.amount)
                TransactionType.TRANSFER.name -> {
                    add(tx.paymentMethod, -tx.amount)
                    tx.toPaymentMethod?.let { add(it, tx.amount) }
                }
            }
        }
        return map.map { AccountBalance(it.key, it.value) }.sortedByDescending { it.balance }
    }

    // ---- Daily allowance -------------------------------------------------------------------

    /**
     * "Today you can spend X." Money not spent on earlier days is shared across the days that
     * are left, so a cheap day makes tomorrow's allowance bigger. Unpaid bills are set aside first.
     */
    fun dailyAllowance(
        transactions: List<TransactionEntity>,
        bills: List<BillReminderEntity>,
        monthLimit: Double,
        now: Long = System.currentTimeMillis()
    ): DailyAllowance {
        val monthStart = startOfMonth(now)
        val todayStart = startOfDay(now)
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val daysLeft = (daysInMonth - cal.get(Calendar.DAY_OF_MONTH) + 1).coerceAtLeast(1)
        val nextMonthStart = Calendar.getInstance().apply {
            timeInMillis = monthStart
            add(Calendar.MONTH, 1)
        }.timeInMillis

        val expenses = transactions.filter { it.type == TransactionType.EXPENSE.name && it.timestamp >= monthStart }
        val spentBeforeToday = expenses.filter { it.timestamp < todayStart }.sumOf { it.amount }
        val spentToday = expenses.filter { it.timestamp >= todayStart }.sumOf { it.amount }
        val reserved = bills.filter { !it.isPaid && it.dueDate < nextMonthStart }.sumOf { it.amount }

        val free = monthLimit - spentBeforeToday - reserved
        val perDay = (free / daysLeft).coerceAtLeast(0.0)
        return DailyAllowance(
            monthLimit = monthLimit,
            reservedForBills = reserved,
            daysLeft = daysLeft,
            perDay = perDay,
            spentToday = spentToday,
            leftToday = perDay - spentToday
        )
    }

    /** About how many days the money in hand will last at the speed of the last 14 days. Null if nothing was spent. */
    fun daysMoneyWillLast(
        balance: Double,
        transactions: List<TransactionEntity>,
        now: Long = System.currentTimeMillis()
    ): Int? {
        if (balance <= 0) return 0
        val from = addDays(startOfDay(now), -13)
        val spent = transactions
            .filter { it.type == TransactionType.EXPENSE.name && it.timestamp >= from }
            .sumOf { it.amount }
        if (spent <= 0) return null
        return (balance / (spent / 14.0)).toInt()
    }

    // ---- Price in hours of work ------------------------------------------------------------

    /** "about 2 hours of work". Null when the person did not tell their income. */
    fun priceInWork(amount: Double, profile: UserProfile): String? {
        val rate = profile.hourlyRate
        if (rate <= 0 || amount <= 0) return null
        val hours = amount / rate
        val dayHours = profile.hoursPerDay.coerceAtLeast(1)
        return when {
            hours < 1.0 -> {
                val minutes = (hours * 60).roundToInt().coerceAtLeast(1)
                "about $minutes ${if (minutes == 1) "minute" else "minutes"} of work"
            }
            hours <= dayHours -> {
                val h = oneDecimal(hours)
                "about $h ${if (h == "1") "hour" else "hours"} of work"
            }
            else -> {
                val d = oneDecimal(hours / dayHours)
                "about $d ${if (d == "1") "day" else "days"} of work"
            }
        }
    }

    private fun oneDecimal(v: Double): String {
        val r = (v * 10).roundToInt() / 10.0
        return if (r % 1.0 == 0.0) r.toInt().toString() else r.toString()
    }

    // ---- Wish list -------------------------------------------------------------------------

    /** Bigger prices wait longer. */
    fun waitDaysFor(price: Double, monthlyIncome: Double): Int {
        if (monthlyIncome <= 0) return 3
        val ratio = price / monthlyIncome
        return when {
            ratio < 0.02 -> 1
            ratio < 0.10 -> 3
            ratio < 0.30 -> 7
            else -> 14
        }
    }

    fun daysLeftToWait(addedAt: Long, waitDays: Int, now: Long = System.currentTimeMillis()): Int {
        val ready = addedAt + waitDays * DAY_MS
        val left = ready - now
        return if (left <= 0) 0 else ((left + DAY_MS - 1) / DAY_MS).toInt()
    }

    // ---- Committees ------------------------------------------------------------------------

    data class CommitteeStatus(
        /** True when this month's payment has not been made yet. */
        val paymentDue: Boolean,
        /** True when it is the person's month and the pot is not collected yet. */
        val myTurnNow: Boolean,
        val payoutMonthLabel: String,
        val nextPaymentLabel: String,
        val monthNumberNow: Int
    )

    fun committeeStatus(c: CommitteeEntity, now: Long = System.currentTimeMillis()): CommitteeStatus {
        val fmt = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val start = Calendar.getInstance().apply {
            clear()
            val parts = c.startMonth.split("-")
            set(parts[0].toInt(), parts[1].toInt() - 1, 1)
        }
        val nowCal = Calendar.getInstance().apply { timeInMillis = now }
        val monthsPassed = (nowCal.get(Calendar.YEAR) - start.get(Calendar.YEAR)) * 12 +
            (nowCal.get(Calendar.MONTH) - start.get(Calendar.MONTH))
        fun monthLabel(index: Int) = fmt.format((start.clone() as Calendar).apply { add(Calendar.MONTH, index) }.time)
        return CommitteeStatus(
            paymentDue = c.paidMonths < c.totalMembers && monthsPassed >= c.paidMonths,
            myTurnNow = !c.payoutReceived && monthsPassed >= c.myTurn - 1 && monthsPassed >= 0,
            payoutMonthLabel = monthLabel(c.myTurn - 1),
            nextPaymentLabel = if (c.paidMonths < c.totalMembers) monthLabel(c.paidMonths) else "All paid",
            monthNumberNow = (monthsPassed + 1).coerceIn(1, c.totalMembers)
        )
    }

    // ---- Charts ----------------------------------------------------------------------------

    /** Calendar-based range. [offset] 0 is now, -1 is the one before, and so on. Weeks start on Monday. */
    fun periodRange(kind: PeriodKind, offset: Int, now: Long = System.currentTimeMillis()): PeriodRange {
        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            timeInMillis = startOfDay(now)
        }
        return when (kind) {
            PeriodKind.WEEK -> {
                cal.add(Calendar.WEEK_OF_YEAR, offset)
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                val start = cal.timeInMillis
                val end = addDays(start, 7)
                val f = SimpleDateFormat("d MMM", Locale.getDefault())
                val label = when (offset) {
                    0 -> "This week"
                    -1 -> "Last week"
                    else -> "${f.format(start)} - ${f.format(end - DAY_MS)}"
                }
                PeriodRange(kind, start, end, label)
            }
            PeriodKind.MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.add(Calendar.MONTH, offset)
                val start = cal.timeInMillis
                val end = (cal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }.timeInMillis
                val label = when (offset) {
                    0 -> "This month"
                    -1 -> "Last month"
                    else -> SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(start)
                }
                PeriodRange(kind, start, end, label)
            }
            PeriodKind.YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.add(Calendar.YEAR, offset)
                val start = cal.timeInMillis
                val end = (cal.clone() as Calendar).apply { add(Calendar.YEAR, 1) }.timeInMillis
                val label = when (offset) {
                    0 -> "This year"
                    -1 -> "Last year"
                    else -> SimpleDateFormat("yyyy", Locale.getDefault()).format(start)
                }
                PeriodRange(kind, start, end, label)
            }
        }
    }

    fun summarize(
        transactions: List<TransactionEntity>,
        range: PeriodRange,
        now: Long = System.currentTimeMillis()
    ): PeriodSummary {
        val inRange = transactions.filter { it.timestamp >= range.start && it.timestamp < range.endExclusive }
        val expenses = inRange.filter { it.type == TransactionType.EXPENSE.name }
        val spent = expenses.sumOf { it.amount }
        val income = inRange.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }

        val byCategory = expenses.groupBy { it.category }
            .map { (cat, list) ->
                val amount = list.sumOf { it.amount }
                CategoryTotal(cat, amount, if (spent > 0) (amount / spent).toFloat() else 0f)
            }
            .sortedByDescending { it.amount }

        val bars: List<DayTotal>
        val daysCounted: Int
        val noSpendDays: Int
        if (range.kind == PeriodKind.YEAR) {
            val monthFmt = SimpleDateFormat("MMM", Locale.getDefault())
            val monthStarts = (0 until 12).map { m ->
                Calendar.getInstance().apply { timeInMillis = range.start; add(Calendar.MONTH, m) }.timeInMillis
            }
            bars = monthStarts.mapIndexed { i, ms ->
                val me = if (i == 11) range.endExclusive else monthStarts[i + 1]
                DayTotal(ms, monthFmt.format(ms), expenses.filter { it.timestamp in ms until me }.sumOf { it.amount })
            }
            val daysInYear = ((minOf(now, range.endExclusive) - range.start) / DAY_MS).toInt().coerceAtLeast(1)
            daysCounted = daysInYear
            noSpendDays = 0
        } else {
            val totalDays = ((range.endExclusive - range.start) / DAY_MS).toInt()
            val labelFmt = SimpleDateFormat(if (range.kind == PeriodKind.WEEK) "EEE" else "d", Locale.getDefault())
            bars = (0 until totalDays).map { i ->
                val ds = addDays(range.start, i)
                val de = addDays(ds, 1)
                DayTotal(ds, labelFmt.format(ds), expenses.filter { it.timestamp in ds until de }.sumOf { it.amount })
            }
            val todayStart = startOfDay(now)
            val finished = bars.filter { it.dayStart < todayStart }
            // Only days after the very first record can count as "no spending" days.
            val firstRecord = transactions.minOfOrNull { it.timestamp }?.let { startOfDay(it) } ?: Long.MAX_VALUE
            noSpendDays = finished.count { it.amount == 0.0 && it.dayStart >= firstRecord }
            daysCounted = if (now >= range.endExclusive) totalDays else finished.size + 1
        }

        return PeriodSummary(
            range = range,
            spent = spent,
            income = income,
            transactionCount = inRange.size,
            byCategory = byCategory,
            bars = bars,
            biggestExpense = expenses.maxByOrNull { it.amount },
            busiestBar = bars.maxByOrNull { it.amount }?.takeIf { it.amount > 0 },
            noSpendDays = noSpendDays,
            daysCounted = daysCounted.coerceAtLeast(1)
        )
    }

    /**
     * What was spent in the period before, for a fair comparison. While the current period is still going on,
     * only the same number of days of the earlier period are counted ("first 9 days against first 9 days").
     */
    fun comparableSpent(
        transactions: List<TransactionEntity>,
        kind: PeriodKind,
        offset: Int,
        now: Long = System.currentTimeMillis()
    ): Double {
        val current = periodRange(kind, offset, now)
        val previous = periodRange(kind, offset - 1, now)
        val elapsed = if (now < current.endExclusive) (now - current.start).coerceAtLeast(0) else Long.MAX_VALUE
        val until = if (elapsed == Long.MAX_VALUE) previous.endExclusive else minOf(previous.endExclusive, previous.start + elapsed)
        return transactions
            .filter { it.type == TransactionType.EXPENSE.name && it.timestamp >= previous.start && it.timestamp < until }
            .sumOf { it.amount }
    }

    /** "20% more" / "15% less" compared with before. Null if there is nothing to compare with. */
    fun changeText(now: Double, before: Double): String? {
        if (before <= 0) return null
        val pct = ((now - before) / before * 100).roundToInt()
        return when {
            abs(pct) < 1 -> "same as before"
            pct > 0 -> "$pct% more than before"
            else -> "${-pct}% less than before"
        }
    }

    /** Days in a row, ending yesterday, with no spending. Starts counting from the first record. */
    fun noSpendStreak(transactions: List<TransactionEntity>, now: Long = System.currentTimeMillis()): Int {
        val first = transactions.minOfOrNull { it.timestamp } ?: return 0
        val firstDay = startOfDay(first)
        val spendDays = transactions.filter { it.type == TransactionType.EXPENSE.name }
            .map { startOfDay(it.timestamp) }.toSet()
        var day = addDays(startOfDay(now), -1)
        var streak = 0
        while (day >= firstDay && day !in spendDays) {
            streak++
            day = addDays(day, -1)
        }
        return streak
    }
}
