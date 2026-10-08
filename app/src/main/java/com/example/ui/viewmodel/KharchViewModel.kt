package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BillReminderEntity
import com.example.data.model.BudgetEntity
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.repository.KharchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class KharchViewModel(
    private val repository: KharchRepository
) : ViewModel() {

    private val _isBalanceHidden = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedFilterType = MutableStateFlow("ALL")
    private val _selectedSpendingPeriod = MutableStateFlow("Week")
    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    private val _selectedReportPeriod = MutableStateFlow("Daily")
    private val _reportDateOffset = MutableStateFlow(0)
    private val _affordabilityBasisType = MutableStateFlow("INCOME")
    private val _affordabilityCustomBasis = MutableStateFlow(75000.0)
    private val _disabledFixedCostIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _customFixedCosts = MutableStateFlow<List<FixedCostItem>>(emptyList())

    val uiState: StateFlow<KharchUiState> = combine(
        repository.allTransactions,
        repository.allBudgets,
        repository.allSavingsGoals,
        repository.allBillReminders,
        _isBalanceHidden,
        _searchQuery,
        _selectedFilterType,
        _selectedSpendingPeriod,
        _selectedCategoryFilter,
        _selectedReportPeriod,
        _reportDateOffset,
        _affordabilityBasisType,
        _affordabilityCustomBasis,
        _disabledFixedCostIds,
        _customFixedCosts
    ) { rawArgs ->
        @Suppress("UNCHECKED_CAST")
        val txs = rawArgs[0] as List<TransactionEntity>
        @Suppress("UNCHECKED_CAST")
        val bgt = rawArgs[1] as List<BudgetEntity>
        @Suppress("UNCHECKED_CAST")
        val goals = rawArgs[2] as List<SavingsGoalEntity>
        @Suppress("UNCHECKED_CAST")
        val bills = rawArgs[3] as List<BillReminderEntity>
        val hidden = rawArgs[4] as Boolean
        val query = rawArgs[5] as String
        val filter = rawArgs[6] as String
        val spendPeriod = rawArgs[7] as String
        val catFilter = rawArgs[8] as String?
        val reportPeriod = rawArgs[9] as String
        val offset = rawArgs[10] as Int
        val affBasisType = rawArgs[11] as String
        val affCustomBasis = rawArgs[12] as Double
        @Suppress("UNCHECKED_CAST")
        val affDisabledIds = rawArgs[13] as Set<Long>
        @Suppress("UNCHECKED_CAST")
        val affCustomCosts = rawArgs[14] as List<FixedCostItem>

        KharchUiState(
            transactions = txs,
            budgets = bgt,
            savingsGoals = goals,
            billReminders = bills,
            isBalanceHidden = hidden,
            searchQuery = query,
            selectedFilterType = filter,
            selectedSpendingPeriod = spendPeriod,
            selectedCategoryFilter = catFilter,
            selectedReportPeriod = reportPeriod,
            reportDateOffset = offset,
            affordabilityBasisType = affBasisType,
            affordabilityCustomBasis = affCustomBasis,
            disabledFixedCostIds = affDisabledIds,
            customFixedCosts = affCustomCosts
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = KharchUiState()
    )

    init {
        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
        }
    }

    fun toggleBalanceVisibility() {
        _isBalanceHidden.update { !it }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterType(filter: String) {
        _selectedFilterType.value = filter
    }

    fun setSpendingPeriod(period: String) {
        _selectedSpendingPeriod.value = period
    }

    fun selectCategoryFilter(category: String?) {
        _selectedCategoryFilter.value = if (_selectedCategoryFilter.value == category) null else category
    }

    fun setReportPeriod(period: String) {
        _selectedReportPeriod.value = period
        _reportDateOffset.value = 0
    }

    fun changeReportOffset(delta: Int) {
        _reportDateOffset.update { it + delta }
    }

    // Transactions CRUD
    fun addExpense(
        title: String,
        amount: Double,
        category: String,
        paymentMethod: String,
        timestamp: Long = System.currentTimeMillis(),
        note: String = "",
        receiptUri: String? = null
    ) {
        viewModelScope.launch {
            val entity = TransactionEntity(
                title = title.ifBlank { "Expense" },
                amount = amount,
                type = TransactionType.EXPENSE.name,
                category = category,
                paymentMethod = paymentMethod,
                timestamp = timestamp,
                note = note,
                receiptUri = receiptUri
            )
            repository.insertTransaction(entity)
        }
    }

    fun addIncome(
        title: String,
        amount: Double,
        source: String,
        paymentMethod: String,
        timestamp: Long = System.currentTimeMillis(),
        note: String = ""
    ) {
        viewModelScope.launch {
            val entity = TransactionEntity(
                title = title.ifBlank { source },
                amount = amount,
                type = TransactionType.INCOME.name,
                category = source,
                paymentMethod = paymentMethod,
                timestamp = timestamp,
                note = note
            )
            repository.insertTransaction(entity)
        }
    }

    fun addTransfer(
        amount: Double,
        fromMethod: String,
        toMethod: String,
        timestamp: Long = System.currentTimeMillis(),
        note: String = ""
    ) {
        viewModelScope.launch {
            val entity = TransactionEntity(
                title = "$fromMethod to $toMethod",
                amount = amount,
                type = TransactionType.TRANSFER.name,
                category = "Transfer",
                paymentMethod = fromMethod,
                toPaymentMethod = toMethod,
                timestamp = timestamp,
                note = note
            )
            repository.insertTransaction(entity)
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    // Budgets CRUD
    fun setMonthlySpendingLimit(limit: Double, alertThresholdPercent: Int = 80) {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            val monthYear = "${cal.get(Calendar.YEAR)}-${String.format("%02d", cal.get(Calendar.MONTH) + 1)}"
            val existing = uiState.value.budgets.firstOrNull {
                it.category.equals("OVERALL", ignoreCase = true) || it.category.equals("TOTAL", ignoreCase = true)
            }
            if (existing != null) {
                repository.updateBudget(
                    existing.copy(
                        monthlyLimit = limit,
                        alertThresholdPercent = alertThresholdPercent,
                        monthYear = monthYear
                    )
                )
            } else {
                repository.insertBudget(
                    BudgetEntity(
                        category = "OVERALL",
                        monthlyLimit = limit,
                        alertThresholdPercent = alertThresholdPercent,
                        monthYear = monthYear
                    )
                )
            }
        }
    }

    fun saveBudget(category: String, monthlyLimit: Double, alertThresholdPercent: Int = 80) {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            val monthYear = "${cal.get(Calendar.YEAR)}-${String.format("%02d", cal.get(Calendar.MONTH) + 1)}"
            val existing = uiState.value.budgets.firstOrNull { it.category.equals(category, ignoreCase = true) }
            if (existing != null) {
                repository.updateBudget(
                    existing.copy(
                        monthlyLimit = monthlyLimit,
                        alertThresholdPercent = alertThresholdPercent,
                        monthYear = monthYear
                    )
                )
            } else {
                repository.insertBudget(
                    BudgetEntity(
                        category = category,
                        monthlyLimit = monthlyLimit,
                        alertThresholdPercent = alertThresholdPercent,
                        monthYear = monthYear
                    )
                )
            }
        }
    }

    fun deleteBudget(budget: BudgetEntity) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
        }
    }

    // Savings Goals CRUD
    fun addSavingsGoal(title: String, targetAmount: Double, targetDate: String, icon: String = "SAVINGS") {
        viewModelScope.launch {
            repository.insertSavingsGoal(
                SavingsGoalEntity(
                    title = title,
                    targetAmount = targetAmount,
                    currentAmount = 0.0,
                    targetDate = targetDate,
                    iconName = icon
                )
            )
        }
    }

    fun depositToSavingsGoal(goal: SavingsGoalEntity, amount: Double) {
        viewModelScope.launch {
            val updated = goal.copy(currentAmount = (goal.currentAmount + amount).coerceAtMost(goal.targetAmount))
            repository.updateSavingsGoal(updated)
            // Log as expense or transfer
            repository.insertTransaction(
                TransactionEntity(
                    title = "Deposit: ${goal.title}",
                    amount = amount,
                    type = TransactionType.EXPENSE.name,
                    category = "Savings",
                    paymentMethod = "Bank",
                    timestamp = System.currentTimeMillis(),
                    note = "Allocated to savings goal"
                )
            )
        }
    }

    fun deleteSavingsGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(goal)
        }
    }

    // Bill Reminders
    fun addBillReminder(title: String, amount: Double, dueDate: Long, category: String, paymentMethod: String) {
        viewModelScope.launch {
            repository.insertBillReminder(
                BillReminderEntity(
                    title = title,
                    amount = amount,
                    dueDate = dueDate,
                    category = category,
                    paymentMethod = paymentMethod
                )
            )
        }
    }

    fun markBillAsPaid(bill: BillReminderEntity) {
        viewModelScope.launch {
            repository.markBillAsPaid(bill)
        }
    }

    fun deleteBillReminder(bill: BillReminderEntity) {
        viewModelScope.launch {
            repository.deleteBillReminder(bill)
        }
    }

    fun resetData() {
        viewModelScope.launch {
            repository.resetAllData()
        }
    }

    fun seedRandomTestData() {
        viewModelScope.launch {
            repository.seedRandomTestingData()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
        }
    }

    // Affordability Simulator Controls & Simulation Engine
    fun setAffordabilityBasisType(basisType: String, customAmount: Double? = null) {
        _affordabilityBasisType.value = basisType
        if (customAmount != null) {
            _affordabilityCustomBasis.value = customAmount
        }
    }

    fun toggleAffordabilityFixedCost(id: Long) {
        _disabledFixedCostIds.update { current ->
            if (current.contains(id)) current - id else current + id
        }
    }

    fun addAffordabilityCustomFixedCost(title: String, amount: Double, category: String = "Bills") {
        if (amount <= 0) return
        val newCustom = FixedCostItem(
            id = System.currentTimeMillis(),
            title = title.ifBlank { "Custom Fixed Commitment" },
            amount = amount,
            category = category,
            isPaid = false,
            isCustom = true,
            isEnabled = true
        )
        _customFixedCosts.update { list -> list + newCustom }
    }

    fun removeAffordabilityCustomFixedCost(id: Long) {
        _customFixedCosts.update { list -> list.filter { it.id != id } }
    }

    fun simulatePurchase(
        itemName: String,
        price: Double,
        category: String = "Shopping",
        tenureMonths: Int = 1
    ): PurchaseSimulation {
        val analysis = uiState.value.affordabilityAnalysis
        return uiState.value.simulatePurchaseAffordability(
            analysis = analysis,
            itemName = itemName,
            price = price,
            category = category,
            tenureMonths = tenureMonths
        )
    }

    // "What Can I Afford?" Legacy Compatibility Simulator logic
    fun simulateAffordability(itemName: String, itemPrice: Double): AffordabilityResult {
        val currentState = uiState.value
        val sim = simulatePurchase(itemName = itemName, price = itemPrice, category = "Shopping", tenureMonths = 1)
        val analysis = currentState.affordabilityAnalysis

        return AffordabilityResult(
            itemName = sim.itemName,
            itemPrice = itemPrice,
            currentBalance = analysis.budgetBasis,
            balanceAfterPurchase = sim.remainingAfter,
            monthlyDiscretionaryRemaining = sim.remainingAfter,
            upcomingBillsTotal = analysis.totalRecurringFixedCosts,
            isAffordable = sim.verdict != SimVerdict.DEFICIT,
            cautionLevel = when (sim.verdict) {
                SimVerdict.COMFORTABLE -> "SAFE"
                SimVerdict.MODERATE -> "MODERATE"
                SimVerdict.STRETCHED -> "MODERATE"
                SimVerdict.DEFICIT -> "DANGEROUS"
            },
            verdictTitle = sim.verdictTitle,
            verdictMessage = "${sim.verdictDescription} ${sim.recommendation}"
        )
    }

    // Reports Helpers
    fun getDailyReport(offset: Int): DailyReport {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, offset)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val endOfDay = cal.timeInMillis

        val sdf = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault())
        val dateFormatted = sdf.format(startOfDay)

        val txs = uiState.value.transactions
        val dayTxs = txs.filter { it.timestamp in startOfDay..endOfDay }

        val dayIncome = dayTxs.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
        val dayExpense = dayTxs.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }

        val priorIncome = txs.filter { it.timestamp < startOfDay && it.type == TransactionType.INCOME.name }.sumOf { it.amount }
        val priorExpense = txs.filter { it.timestamp < startOfDay && it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
        val startingBalance = (priorIncome - priorExpense).coerceAtLeast(0.0)
        val remaining = (startingBalance + dayIncome - dayExpense).coerceAtLeast(0.0)

        val expenseTxs = dayTxs.filter { it.type == TransactionType.EXPENSE.name }
        val catGroups = expenseTxs.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
        val topCategoryEntry = catGroups.maxByOrNull { it.value }

        val largestExpense = expenseTxs.maxByOrNull { it.amount }

        return DailyReport(
            dateMillis = startOfDay,
            dateFormatted = dateFormatted,
            startingBalance = startingBalance,
            income = dayIncome,
            expenses = dayExpense,
            remaining = remaining,
            transactionCount = dayTxs.size,
            topCategory = topCategoryEntry?.key,
            topCategoryAmount = topCategoryEntry?.value ?: 0.0,
            largestExpense = largestExpense,
            timeline = dayTxs.sortedByDescending { it.timestamp }
        )
    }

    fun getWeeklyReport(offset: Int): WeeklyReport {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.add(Calendar.WEEK_OF_YEAR, offset)
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val startOfWeek = cal.timeInMillis

        val sdfLabel = SimpleDateFormat("MMM d", Locale.getDefault())
        val weekStartStr = sdfLabel.format(startOfWeek)

        val days = mutableListOf<DaySpending>()
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())

        val txs = uiState.value.transactions
        for (i in 0..6) {
            val dCal = Calendar.getInstance().apply {
                timeInMillis = startOfWeek
                add(Calendar.DAY_OF_YEAR, i)
            }
            val dStart = dCal.timeInMillis
            val dEnd = dStart + 86399999L
            val daySpent = txs.filter { it.type == TransactionType.EXPENSE.name && it.timestamp in dStart..dEnd }.sumOf { it.amount }
            days.add(DaySpending(dayLabel = dayFormat.format(dStart), dateMillis = dStart, amount = daySpent))
        }

        val weekEndStr = sdfLabel.format(days.last().dateMillis)
        val totalSpent = days.sumOf { it.amount }
        val avgDaily = totalSpent / 7.0

        val endOfWeek = days.last().dateMillis + 86399999L
        val weekExpenseTxs = txs.filter { it.type == TransactionType.EXPENSE.name && it.timestamp in startOfWeek..endOfWeek }

        val catShares = weekExpenseTxs.groupBy { it.category }
            .map { (cat, list) ->
                val amt = list.sumOf { it.amount }
                val pct = if (totalSpent > 0) (amt / totalSpent).toFloat() else 0f
                CategoryShare(cat, amt, pct)
            }.sortedByDescending { it.amount }

        return WeeklyReport(
            weekLabel = "$weekStartStr – $weekEndStr",
            totalSpent = totalSpent,
            averageDaily = avgDaily,
            dailyBreakdown = days,
            topCategories = catShares,
            transactionCount = weekExpenseTxs.size
        )
    }

    fun getMonthlyExpenses(offset: Int = 0): List<TransactionEntity> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, offset)
        val month = cal.get(Calendar.MONTH)
        val year = cal.get(Calendar.YEAR)

        return uiState.value.transactions.filter {
            if (it.type != TransactionType.EXPENSE.name) return@filter false
            val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            c.get(Calendar.MONTH) == month && c.get(Calendar.YEAR) == year
        }.sortedByDescending { it.timestamp }
    }

    /**
     * Calculates total daily spending trends and pattern insights over the last 30 days.
     */
    fun getThirtyDaySpendingTrend(txs: List<TransactionEntity> = uiState.value.transactions): ThirtyDaySpendingTrend {
        val points = mutableListOf<DailySpendingPoint>()
        val dayFmt = SimpleDateFormat("d MMM", Locale.getDefault())
        val fullFmt = SimpleDateFormat("EEE, d MMM", Locale.getDefault())

        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        // 30 days ending today (from today - 29 days up to today)
        for (i in 29 downTo 0) {
            val dCal = Calendar.getInstance().apply {
                timeInMillis = cal.timeInMillis
                add(Calendar.DAY_OF_YEAR, -i)
            }
            val startMillis = dCal.timeInMillis
            val endMillis = startMillis + 86399999L
            val dayOfWeek = dCal.get(Calendar.DAY_OF_WEEK)
            val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY

            val dayTxs = txs.filter {
                it.type == TransactionType.EXPENSE.name && it.timestamp in startMillis..endMillis
            }
            val dayTotal = dayTxs.sumOf { it.amount }

            points.add(
                DailySpendingPoint(
                    dayLabel = dayFmt.format(dCal.time),
                    fullDateLabel = fullFmt.format(dCal.time),
                    dateMillis = startMillis,
                    amount = dayTotal,
                    transactionCount = dayTxs.size,
                    isWeekend = isWeekend
                )
            )
        }

        val totalAmount = points.sumOf { it.amount }
        val avgDaily = if (points.isNotEmpty()) totalAmount / points.size else 0.0
        val peakDay = points.maxByOrNull { it.amount }

        val weekendAvg = points.filter { it.isWeekend }.map { it.amount }.average().let { if (it.isNaN()) 0.0 else it }
        val weekdayAvg = points.filter { !it.isWeekend }.map { it.amount }.average().let { if (it.isNaN()) 0.0 else it }

        val patternInsight = when {
            points.all { it.amount == 0.0 } -> "No expenses recorded in the last 30 days."
            weekendAvg > weekdayAvg * 1.35 && weekdayAvg > 0 -> {
                val ratio = ((weekendAvg / weekdayAvg - 1.0) * 100).toInt()
                "Weekend spending spikes: You spend ~$ratio% more on weekends compared to weekdays."
            }
            weekdayAvg > weekendAvg * 1.30 && weekendAvg > 0 -> {
                val ratio = ((weekdayAvg / weekendAvg - 1.0) * 100).toInt()
                "Weekday driven: Spending is concentrated during workdays (+$ratio% vs weekends)."
            }
            peakDay != null && peakDay.amount > (avgDaily * 2.5) && avgDaily > 0 -> {
                "Highest outlier spike occurred on ${peakDay.fullDateLabel} (Rs. ${String.format(Locale.getDefault(), "%,.0f", peakDay.amount)})."
            }
            else -> "Steady spending velocity: Daily spending remains balanced around Rs. ${String.format(Locale.getDefault(), "%,.0f", avgDaily)}/day."
        }

        return ThirtyDaySpendingTrend(
            dailyPoints = points,
            totalAmount = totalAmount,
            averageDaily = avgDaily,
            peakDay = peakDay,
            patternInsight = patternInsight
        )
    }

    fun getMonthlyReport(offset: Int): MonthlyReport {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, offset)
        val month = cal.get(Calendar.MONTH)
        val year = cal.get(Calendar.YEAR)

        val sdfMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val monthLabel = sdfMonth.format(cal.time)

        val txs = uiState.value.transactions
        val monthTxs = txs.filter {
            val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            c.get(Calendar.MONTH) == month && c.get(Calendar.YEAR) == year
        }

        val monthIncome = monthTxs.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
        val monthExpense = monthTxs.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
        val netSavings = monthIncome - monthExpense
        val savingsRate = if (monthIncome > 0) ((netSavings / monthIncome) * 100).toFloat().coerceAtLeast(0f) else 0f

        val totalBudget = uiState.value.budgets.sumOf { it.monthlyLimit }
        val budgetAdherence = if (totalBudget > 0) {
            (((totalBudget - monthExpense) / totalBudget) * 100).toFloat().coerceIn(0f, 100f)
        } else 100f

        val catShares = monthTxs.filter { it.type == TransactionType.EXPENSE.name }
            .groupBy { it.category }
            .map { (cat, list) ->
                val amt = list.sumOf { it.amount }
                val pct = if (monthExpense > 0) (amt / monthExpense).toFloat() else 0f
                CategoryShare(cat, amt, pct)
            }.sortedByDescending { it.amount }

        return MonthlyReport(
            monthLabel = monthLabel,
            totalIncome = monthIncome,
            totalExpense = monthExpense,
            netSavings = netSavings,
            savingsRate = savingsRate,
            totalBudget = totalBudget,
            budgetAdherencePercent = budgetAdherence,
            topCategories = catShares
        )
    }

    fun getYearlyReport(offset: Int): YearlyReport {
        val cal = Calendar.getInstance()
        cal.add(Calendar.YEAR, offset)
        val year = cal.get(Calendar.YEAR)
        val yearLabel = year.toString()

        val txs = uiState.value.transactions
        val yearTxs = txs.filter {
            val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            c.get(Calendar.YEAR) == year
        }

        val yearIncome = yearTxs.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
        val yearExpense = yearTxs.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
        val netSavings = yearIncome - yearExpense

        val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val monthlyBreakdown = (0..11).map { m ->
            val spentInMonth = yearTxs.filter {
                it.type == TransactionType.EXPENSE.name &&
                Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.MONTH) == m
            }.sumOf { it.amount }
            DaySpending(dayLabel = monthNames[m], dateMillis = 0L, amount = spentInMonth)
        }

        val highestMonth = monthlyBreakdown.maxByOrNull { it.amount }

        return YearlyReport(
            yearLabel = yearLabel,
            totalIncome = yearIncome,
            totalExpense = yearExpense,
            netSavings = netSavings,
            highestSpendingMonth = highestMonth?.dayLabel ?: "N/A",
            highestMonthExpense = highestMonth?.amount ?: 0.0,
            monthlyBreakdown = monthlyBreakdown
        )
    }

    // Helper for Spending Page
    fun getSpendingPeriodData(period: String): Pair<List<DaySpending>, List<CategoryShare>> {
        val txs = uiState.value.transactions.filter { it.type == TransactionType.EXPENSE.name }
        val now = System.currentTimeMillis()

        return when (period) {
            "Today" -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                val start = cal.timeInMillis
                val filtered = txs.filter { it.timestamp >= start }
                val total = filtered.sumOf { it.amount }

                // Group by hour blocks (Morning, Afternoon, Evening, Night)
                val blocks = listOf(
                    DaySpending("Morning (6-12)", 0L, filtered.filter {
                        val h = Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.HOUR_OF_DAY)
                        h in 6..11
                    }.sumOf { it.amount }),
                    DaySpending("Afternoon (12-17)", 0L, filtered.filter {
                        val h = Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.HOUR_OF_DAY)
                        h in 12..16
                    }.sumOf { it.amount }),
                    DaySpending("Evening (17-21)", 0L, filtered.filter {
                        val h = Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.HOUR_OF_DAY)
                        h in 17..20
                    }.sumOf { it.amount }),
                    DaySpending("Night (21-6)", 0L, filtered.filter {
                        val h = Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.HOUR_OF_DAY)
                        h >= 21 || h < 6
                    }.sumOf { it.amount })
                )

                val shares = filtered.groupBy { it.category }
                    .map { (cat, list) ->
                        val amt = list.sumOf { it.amount }
                        CategoryShare(cat, amt, if (total > 0) (amt / total).toFloat() else 0f)
                    }.sortedByDescending { it.amount }

                Pair(blocks, shares)
            }
            "Week" -> {
                val cal = Calendar.getInstance().apply {
                    firstDayOfWeek = Calendar.MONDAY
                    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }
                val start = cal.timeInMillis
                val end = start + (7 * 86400000L)
                val weekTxs = txs.filter { it.timestamp in start until end }
                val total = weekTxs.sumOf { it.amount }

                val daysFmt = SimpleDateFormat("EEE", Locale.getDefault())
                val days = (0..6).map { i ->
                    val dStart = start + (i * 86400000L)
                    val dEnd = dStart + 86400000L
                    val amt = weekTxs.filter { it.timestamp in dStart until dEnd }.sumOf { it.amount }
                    DaySpending(daysFmt.format(dStart), dStart, amt)
                }

                val shares = weekTxs.groupBy { it.category }
                    .map { (cat, list) ->
                        val amt = list.sumOf { it.amount }
                        CategoryShare(cat, amt, if (total > 0) (amt / total).toFloat() else 0f)
                    }.sortedByDescending { it.amount }

                Pair(days, shares)
            }
            "Month" -> {
                val cal = Calendar.getInstance()
                val month = cal.get(Calendar.MONTH)
                val year = cal.get(Calendar.YEAR)
                val monthTxs = txs.filter {
                    val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                    c.get(Calendar.MONTH) == month && c.get(Calendar.YEAR) == year
                }
                val total = monthTxs.sumOf { it.amount }

                // 4 weeks of the month
                val weeks = (1..4).map { w ->
                    val wAmt = monthTxs.filter {
                        val day = Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.DAY_OF_MONTH)
                        day in ((w - 1) * 7 + 1)..(w * 7 + (if (w == 4) 3 else 0))
                    }.sumOf { it.amount }
                    DaySpending("Week $w", 0L, wAmt)
                }

                val shares = monthTxs.groupBy { it.category }
                    .map { (cat, list) ->
                        val amt = list.sumOf { it.amount }
                        CategoryShare(cat, amt, if (total > 0) (amt / total).toFloat() else 0f)
                    }.sortedByDescending { it.amount }

                Pair(weeks, shares)
            }
            else -> { // "Year"
                val cal = Calendar.getInstance()
                val year = cal.get(Calendar.YEAR)
                val yearTxs = txs.filter {
                    Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.YEAR) == year
                }
                val total = yearTxs.sumOf { it.amount }
                val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                val months = (0..11).map { m ->
                    val mAmt = yearTxs.filter {
                        Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.MONTH) == m
                    }.sumOf { it.amount }
                    DaySpending(monthNames[m], 0L, mAmt)
                }

                val shares = yearTxs.groupBy { it.category }
                    .map { (cat, list) ->
                        val amt = list.sumOf { it.amount }
                        CategoryShare(cat, amt, if (total > 0) (amt / total).toFloat() else 0f)
                    }.sortedByDescending { it.amount }

                Pair(months, shares)
            }
        }
    }
}
