package com.example.ui.viewmodel

import com.example.data.model.BillReminderEntity
import com.example.data.model.BudgetEntity
import com.example.data.model.CommitteeEntity
import com.example.data.model.DebtEntity
import com.example.data.model.WishItemEntity
import com.example.data.profile.UserProfile
import com.example.domain.AccountBalance
import com.example.domain.DailyAllowance
import com.example.domain.MoneyMath
import com.example.domain.PeriodKind
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

data class CategoryShare(
    val category: String,
    val amount: Double,
    val percentage: Float
)

data class DaySpending(
    val dayLabel: String, // e.g. "Mon", "Tue" or "Sep 20"
    val dateMillis: Long,
    val amount: Double
)

data class DailyReport(
    val dateMillis: Long,
    val dateFormatted: String,
    val startingBalance: Double,
    val income: Double,
    val expenses: Double,
    val remaining: Double,
    val transactionCount: Int,
    val topCategory: String?,
    val topCategoryAmount: Double,
    val largestExpense: TransactionEntity?,
    val timeline: List<TransactionEntity>
)

data class WeeklyReport(
    val weekLabel: String,
    val totalSpent: Double,
    val averageDaily: Double,
    val dailyBreakdown: List<DaySpending>,
    val topCategories: List<CategoryShare>,
    val transactionCount: Int
)

data class MonthlyReport(
    val monthLabel: String,
    val totalIncome: Double,
    val totalExpense: Double,
    val netSavings: Double,
    val savingsRate: Float,
    val totalBudget: Double,
    val budgetAdherencePercent: Float,
    val topCategories: List<CategoryShare>
)

data class YearlyReport(
    val yearLabel: String,
    val totalIncome: Double,
    val totalExpense: Double,
    val netSavings: Double,
    val highestSpendingMonth: String,
    val highestMonthExpense: Double,
    val monthlyBreakdown: List<DaySpending>
)

data class AffordabilityResult(
    val itemName: String,
    val itemPrice: Double,
    val currentBalance: Double,
    val balanceAfterPurchase: Double,
    val monthlyDiscretionaryRemaining: Double,
    val upcomingBillsTotal: Double,
    val isAffordable: Boolean,
    val cautionLevel: String, // "SAFE", "MODERATE", "DANGEROUS"
    val verdictTitle: String,
    val verdictMessage: String
)

data class FixedCostItem(
    val id: Long,
    val title: String,
    val amount: Double,
    val category: String,
    val isPaid: Boolean,
    val isCustom: Boolean = false,
    val isEnabled: Boolean = true
)

enum class SimVerdict {
    COMFORTABLE,   // Remaining discretionary buffer > 35%
    MODERATE,      // Remaining discretionary buffer between 15% and 35%
    STRETCHED,     // Remaining buffer between 0% and 15% (very tight daily spend)
    DEFICIT        // Exceeds remaining budget after fixed costs
}

data class PurchaseSimulation(
    val itemName: String,
    val totalCost: Double,
    val category: String,
    val tenureMonths: Int,
    val monthlyCost: Double,
    val remainingBefore: Double,
    val remainingAfter: Double,
    val dailySpendBefore: Double,
    val dailySpendAfter: Double,
    val dailyDropAmount: Double,
    val dailyDropPercentage: Float,
    val daysOfDailyBudgetConsumed: Double,
    val verdict: SimVerdict,
    val verdictTitle: String,
    val verdictDescription: String,
    val recommendation: String
)

data class AffordabilityAnalysis(
    val budgetBasis: Double,
    val budgetBasisType: String, // "INCOME", "BUDGETS", "CUSTOM"
    val totalRecurringFixedCosts: Double,
    val fixedCostsList: List<FixedCostItem>,
    val alreadySpentVariable: Double,
    val remainingDiscretionary: Double,
    val daysRemainingInMonth: Int,
    val safeDailySpend: Double,
    val safeWeeklySpend: Double,
    val fixedCostsShare: Float,
    val variableSpendShare: Float,
    val remainingBufferShare: Float,
    val isDeficit: Boolean
)

enum class BudgetAlertStatus {
    SAFE,       // spent < alertThresholdPercent
    WARNING,    // alertThresholdPercent <= spent < 100%
    EXCEEDED    // spent >= 100%
}

data class CategoryBudgetStatus(
    val budget: BudgetEntity,
    val category: String,
    val monthlyLimit: Double,
    val currentSpent: Double,
    val remaining: Double,
    val spentPercentage: Float, // 0.0 to 1.0+
    val alertThresholdPercent: Int, // e.g. 80
    val alertStatus: BudgetAlertStatus,
    val overAmount: Double // amount over monthlyLimit, or 0.0
)

data class BudgetImpactInfo(
    val category: String,
    val monthlyLimit: Double,
    val currentSpent: Double,
    val addedAmount: Double,
    val projectedSpent: Double,
    val projectedPercentage: Float,
    val alertThresholdPercent: Int,
    val isAlreadyExceeded: Boolean,
    val willExceed: Boolean,
    val willReachWarning: Boolean,
    val excessAmount: Double,
    val remainingBefore: Double,
    val remainingAfter: Double
)

data class KharchUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val savingsGoals: List<SavingsGoalEntity> = emptyList(),
    val billReminders: List<BillReminderEntity> = emptyList(),
    val isBalanceHidden: Boolean = false,
    val searchQuery: String = "",
    val selectedFilterType: String = "ALL", // "ALL", "EXPENSE", "INCOME", "TRANSFER"
    val periodKind: PeriodKind = PeriodKind.MONTH,
    /** 0 is the current period, -1 the one before it, and so on. */
    val periodOffset: Int = 0,
    val selectedCategoryFilter: String? = null,
    val affordabilityBasisType: String = "INCOME", // "INCOME", "BUDGETS", "CUSTOM"
    val affordabilityCustomBasis: Double = 75000.0,
    val disabledFixedCostIds: Set<Long> = emptySet(),
    val customFixedCosts: List<FixedCostItem> = emptyList(),
    val wishItems: List<WishItemEntity> = emptyList(),
    val debts: List<DebtEntity> = emptyList(),
    val committees: List<CommitteeEntity> = emptyList(),
    val profile: UserProfile = UserProfile()
) {
    val totalIncome: Double
        get() = transactions.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }

    val totalExpense: Double
        get() = transactions.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }

    /** Money you have now. It can be below zero if more was spent than came in. */
    val totalBalance: Double
        get() = totalIncome - totalExpense

    /** Money in each place: Cash, Bank, Easypaisa... Transfers move money between them. */
    val accounts: List<AccountBalance>
        get() = MoneyMath.accountBalances(transactions)

    /** The monthly limit the person set, or null if they have not set one yet. */
    val monthlyLimitOrNull: Double?
        get() {
            overallMonthlyBudget?.let { return it.monthlyLimit }
            val catTotal = budgets.filter { !it.category.equals("OVERALL", true) && !it.category.equals("TOTAL", true) }
                .sumOf { it.monthlyLimit }
            return catTotal.takeIf { it > 0 }
        }

    val hasMonthlyLimit: Boolean
        get() = monthlyLimitOrNull != null

    /** How much can be spent today. Null until a monthly limit exists. */
    val dailyAllowance: DailyAllowance?
        get() = monthlyLimitOrNull?.let { MoneyMath.dailyAllowance(transactions, billReminders, it) }

    val upcomingBill: BillReminderEntity?
        get() = billReminders
            .filter { !it.isPaid && it.dueDate >= System.currentTimeMillis() - 86400000L }
            .minByOrNull { it.dueDate }

    // Budget available percentage
    val availableBudgetPercentage: Int
        get() {
            val totalMonthlyBudget = monthlyLimitOrNull ?: return 100
            val currentMonthExpense = getCurrentMonthExpense()
            val remaining = (totalMonthlyBudget - currentMonthExpense).coerceAtLeast(0.0)
            return ((remaining / totalMonthlyBudget) * 100).toInt().coerceIn(0, 100)
        }

    val overallMonthlyBudget: BudgetEntity?
        get() = budgets.firstOrNull { it.category.equals("OVERALL", ignoreCase = true) || it.category.equals("TOTAL", ignoreCase = true) }

    val effectiveMonthlySpendingLimit: Double
        get() {
            overallMonthlyBudget?.let { return it.monthlyLimit }
            val catTotal = budgets.filter { !it.category.equals("OVERALL", ignoreCase = true) && !it.category.equals("TOTAL", ignoreCase = true) }.sumOf { it.monthlyLimit }
            return if (catTotal > 0) catTotal else 50000.0
        }

    val isMonthlyLimitCustomSet: Boolean
        get() = overallMonthlyBudget != null

    val currentMonthTotalExpense: Double
        get() = getCurrentMonthExpense()

    val currentMonthBudgetRemaining: Double
        get() = effectiveMonthlySpendingLimit - currentMonthTotalExpense

    val isCurrentMonthBudgetExceeded: Boolean
        get() = currentMonthTotalExpense > effectiveMonthlySpendingLimit

    val currentMonthBudgetSpentRatio: Float
        get() = if (effectiveMonthlySpendingLimit > 0) (currentMonthTotalExpense / effectiveMonthlySpendingLimit).toFloat() else 0f

    val currentMonthBudgetSpentPercentage: Int
        get() = (currentMonthBudgetSpentRatio * 100).toInt()

    val currentMonthBudgetRemainingPercentage: Int
        get() = if (effectiveMonthlySpendingLimit > 0) {
            (((effectiveMonthlySpendingLimit - currentMonthTotalExpense) / effectiveMonthlySpendingLimit) * 100).toInt().coerceIn(0, 100)
        } else 0

    val daysLeftInCurrentMonth: Int
        get() {
            val cal = Calendar.getInstance()
            val currentDay = cal.get(Calendar.DAY_OF_MONTH)
            val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            return (maxDays - currentDay + 1).coerceAtLeast(1)
        }

    val dailyBudgetRemainingAllowance: Double
        get() = if (currentMonthBudgetRemaining > 0) currentMonthBudgetRemaining / daysLeftInCurrentMonth else 0.0

    val currentMonthBudgetAlertStatus: BudgetAlertStatus
        get() {
            val thresholdRatio = ((overallMonthlyBudget?.alertThresholdPercent ?: 80).coerceIn(50, 100) / 100f)
            return when {
                currentMonthBudgetSpentRatio >= 1.0f -> BudgetAlertStatus.EXCEEDED
                currentMonthBudgetSpentRatio >= thresholdRatio -> BudgetAlertStatus.WARNING
                else -> BudgetAlertStatus.SAFE
            }
        }

    fun getCurrentMonthExpense(): Double {
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)
        return transactions.filter {
            if (it.type != TransactionType.EXPENSE.name) return@filter false
            val itemCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            itemCal.get(Calendar.MONTH) == currentMonth && itemCal.get(Calendar.YEAR) == currentYear
        }.sumOf { it.amount }
    }

    fun getCategoryCurrentMonthSpent(category: String): Double {
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)
        return transactions.filter {
            if (it.type != TransactionType.EXPENSE.name) return@filter false
            if (!it.category.equals(category, ignoreCase = true)) return@filter false
            val itemCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            itemCal.get(Calendar.MONTH) == currentMonth && itemCal.get(Calendar.YEAR) == currentYear
        }.sumOf { it.amount }
    }

    val currentMonthCategoryShares: List<CategoryShare>
        get() {
            val cal = Calendar.getInstance()
            val currentMonth = cal.get(Calendar.MONTH)
            val currentYear = cal.get(Calendar.YEAR)
            val monthExpenses = transactions.filter {
                if (it.type != TransactionType.EXPENSE.name) return@filter false
                val itemCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                itemCal.get(Calendar.MONTH) == currentMonth && itemCal.get(Calendar.YEAR) == currentYear
            }
            val total = monthExpenses.sumOf { it.amount }
            if (total <= 0) return emptyList()
            return monthExpenses.groupBy { it.category }
                .map { (cat, list) ->
                    val amt = list.sumOf { it.amount }
                    CategoryShare(
                        category = cat,
                        amount = amt,
                        percentage = if (total > 0) (amt / total).toFloat() else 0f
                    )
                }.sortedByDescending { it.amount }
        }

    val categoryBudgetStatuses: List<CategoryBudgetStatus>
        get() = budgets.filter { !it.category.equals("OVERALL", ignoreCase = true) && !it.category.equals("TOTAL", ignoreCase = true) }.map { b ->
            val spent = getCategoryCurrentMonthSpent(b.category)
            val limit = b.monthlyLimit
            val pct = if (limit > 0) (spent / limit).toFloat() else 0f
            val thresholdRatio = (b.alertThresholdPercent.coerceIn(50, 100) / 100f)
            val status = when {
                pct >= 1.0f -> BudgetAlertStatus.EXCEEDED
                pct >= thresholdRatio -> BudgetAlertStatus.WARNING
                else -> BudgetAlertStatus.SAFE
            }
            val remaining = (limit - spent).coerceAtLeast(0.0)
            val overAmount = (spent - limit).coerceAtLeast(0.0)

            CategoryBudgetStatus(
                budget = b,
                category = b.category,
                monthlyLimit = limit,
                currentSpent = spent,
                remaining = remaining,
                spentPercentage = pct,
                alertThresholdPercent = b.alertThresholdPercent,
                alertStatus = status,
                overAmount = overAmount
            )
        }

    val exceededBudgets: List<CategoryBudgetStatus>
        get() = categoryBudgetStatuses.filter { it.alertStatus == BudgetAlertStatus.EXCEEDED }

    val warningBudgets: List<CategoryBudgetStatus>
        get() = categoryBudgetStatuses.filter { it.alertStatus == BudgetAlertStatus.WARNING }

    val hasBudgetAlerts: Boolean
        get() = exceededBudgets.isNotEmpty() || warningBudgets.isNotEmpty()

    fun getBudgetImpact(category: String, addedAmount: Double): BudgetImpactInfo? {
        val budgetStatus = categoryBudgetStatuses.firstOrNull { it.category.equals(category, ignoreCase = true) } ?: return null
        val newTotal = budgetStatus.currentSpent + addedAmount
        val limit = budgetStatus.monthlyLimit
        val newPct = if (limit > 0) (newTotal / limit).toFloat() else 0f
        val thresholdRatio = (budgetStatus.alertThresholdPercent.coerceIn(50, 100) / 100f)
        val willExceed = newTotal > limit
        val isAlreadyExceeded = budgetStatus.currentSpent >= limit
        val willReachWarning = newPct >= thresholdRatio && !willExceed

        return BudgetImpactInfo(
            category = budgetStatus.category,
            monthlyLimit = limit,
            currentSpent = budgetStatus.currentSpent,
            addedAmount = addedAmount,
            projectedSpent = newTotal,
            projectedPercentage = newPct,
            alertThresholdPercent = budgetStatus.alertThresholdPercent,
            isAlreadyExceeded = isAlreadyExceeded,
            willExceed = willExceed,
            willReachWarning = willReachWarning,
            excessAmount = (newTotal - limit).coerceAtLeast(0.0),
            remainingBefore = budgetStatus.remaining,
            remainingAfter = (limit - newTotal).coerceAtLeast(0.0)
        )
    }

    val filteredTransactions: List<TransactionEntity>
        get() = transactions.filter { tx ->
            val matchesFilter = when (selectedFilterType) {
                "EXPENSE" -> tx.type == TransactionType.EXPENSE.name
                "INCOME" -> tx.type == TransactionType.INCOME.name
                "TRANSFER" -> tx.type == TransactionType.TRANSFER.name
                else -> true
            }
            val matchesCategory = selectedCategoryFilter == null || tx.category.equals(selectedCategoryFilter, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                    tx.title.contains(searchQuery, ignoreCase = true) ||
                    tx.category.contains(searchQuery, ignoreCase = true) ||
                    tx.paymentMethod.contains(searchQuery, ignoreCase = true)

            matchesFilter && matchesCategory && matchesSearch
        }

    val affordabilityAnalysis: AffordabilityAnalysis
        get() = getAffordabilityAnalysis(
            basisType = affordabilityBasisType,
            customBudgetBasis = affordabilityCustomBasis,
            disabledFixedCostIds = disabledFixedCostIds,
            additionalFixedCosts = customFixedCosts
        )

    fun getAffordabilityAnalysis(
        basisType: String = affordabilityBasisType,
        customBudgetBasis: Double? = affordabilityCustomBasis,
        disabledFixedCostIds: Set<Long> = this.disabledFixedCostIds,
        additionalFixedCosts: List<FixedCostItem> = this.customFixedCosts
    ): AffordabilityAnalysis {
        val cal = Calendar.getInstance()
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val daysRemaining = (maxDays - currentDay + 1).coerceAtLeast(1)

        // "Money in" for one month: what the person said they earn, or what came in this month.
        val monthStartForIncome = MoneyMath.startOfMonth(System.currentTimeMillis())
        val totalIncomeAmt = if (profile.monthlyIncome > 0) profile.monthlyIncome
        else transactions.filter { it.type == TransactionType.INCOME.name && it.timestamp >= monthStartForIncome }.sumOf { it.amount }
        val totalCategoryBudgets = budgets.filter { !it.category.equals("OVERALL", true) && !it.category.equals("TOTAL", true) }.sumOf { it.monthlyLimit }

        val basis = when (basisType) {
            "BUDGETS" -> if (totalCategoryBudgets > 0) totalCategoryBudgets else 60000.0
            "CUSTOM" -> (customBudgetBasis ?: 75000.0).coerceAtLeast(1000.0)
            else -> if (totalIncomeAmt > 0) totalIncomeAmt else (if (totalCategoryBudgets > 0) totalCategoryBudgets else 75000.0)
        }

        val defaultBills = billReminders.map { bill ->
            FixedCostItem(
                id = bill.id,
                title = bill.title,
                amount = bill.amount,
                category = bill.category,
                isPaid = bill.isPaid,
                isCustom = false,
                isEnabled = !disabledFixedCostIds.contains(bill.id)
            )
        }
        val allFixedItems = defaultBills + additionalFixedCosts

        // Bills that are already paid are in 'spent' now, so they are not counted again as 'must pay'.
        val activeFixedCostsTotal = allFixedItems
            .filter { it.isEnabled && !it.isPaid }
            .sumOf { it.amount }

        val currentMonthExpenses = getCurrentMonthExpense()
        val monthStartMs = MoneyMath.startOfMonth(System.currentTimeMillis())
        val nextMonthStartMs = Calendar.getInstance().apply { timeInMillis = monthStartMs; add(Calendar.MONTH, 1) }.timeInMillis
        val paidBillsInMonth = billReminders.filter { it.isPaid && it.dueDate in monthStartMs until nextMonthStartMs }.sumOf { it.amount }
        val variableSpent = (currentMonthExpenses - paidBillsInMonth).coerceAtLeast(0.0)

        val remainingDiscretionary = basis - activeFixedCostsTotal - variableSpent
        val isDeficit = remainingDiscretionary < 0

        val safeDailySpend = if (remainingDiscretionary > 0) remainingDiscretionary / daysRemaining else 0.0
        val safeWeeklySpend = if (remainingDiscretionary > 0) remainingDiscretionary / (daysRemaining / 7.0) else 0.0

        val fixedShare = if (basis > 0) (activeFixedCostsTotal / basis).toFloat().coerceIn(0f, 1f) else 0f
        val varShare = if (basis > 0) (variableSpent / basis).toFloat().coerceIn(0f, 1f) else 0f
        val bufferShare = if (basis > 0 && remainingDiscretionary > 0) (remainingDiscretionary / basis).toFloat().coerceIn(0f, 1f) else 0f

        return AffordabilityAnalysis(
            budgetBasis = basis,
            budgetBasisType = basisType,
            totalRecurringFixedCosts = activeFixedCostsTotal,
            fixedCostsList = allFixedItems,
            alreadySpentVariable = variableSpent,
            remainingDiscretionary = remainingDiscretionary,
            daysRemainingInMonth = daysRemaining,
            safeDailySpend = safeDailySpend,
            safeWeeklySpend = safeWeeklySpend,
            fixedCostsShare = fixedShare,
            variableSpendShare = varShare,
            remainingBufferShare = bufferShare,
            isDeficit = isDeficit
        )
    }

    fun simulatePurchaseAffordability(
        analysis: AffordabilityAnalysis,
        itemName: String,
        price: Double,
        category: String = "Shopping",
        tenureMonths: Int = 1
    ): PurchaseSimulation {
        val safeTenure = tenureMonths.coerceAtLeast(1)
        val monthlyImpact = price / safeTenure
        val remainingBefore = analysis.remainingDiscretionary
        val remainingAfter = remainingBefore - monthlyImpact

        val dailyBefore = analysis.safeDailySpend
        val dailyAfter = if (remainingAfter > 0) remainingAfter / analysis.daysRemainingInMonth else 0.0
        val dailyDrop = (dailyBefore - dailyAfter).coerceAtLeast(0.0)
        val dailyDropPct: Float = if (dailyBefore > 0) ((dailyDrop / dailyBefore) * 100.0).coerceIn(0.0, 100.0).toFloat() else 100f
        val daysConsumed = if (dailyBefore > 0) monthlyImpact / dailyBefore else 0.0

        val name = itemName.ifBlank { "Planned Item" }

        val verdict: SimVerdict
        val title: String
        val desc: String
        val recommendation: String

        when {
            remainingAfter < 0 -> {
                val deficit = abs(remainingAfter)
                verdict = SimVerdict.DEFICIT
                title = "Exceeds Remaining Budget"
                desc = "This purchase exceeds your remaining monthly budget by Rs. ${String.format(Locale.getDefault(), "%,.0f", deficit)} after accounting for Rs. ${String.format(Locale.getDefault(), "%,.0f", analysis.totalRecurringFixedCosts)} in recurring fixed costs."
                recommendation = if (safeTenure == 1) "Consider splitting this across 2–3 months or establishing a savings goal before purchasing." else "Even across $safeTenure months, this exceeds your safe monthly spending margin."
            }
            remainingAfter < (analysis.budgetBasis * 0.10) || dailyDropPct > 50f -> {
                verdict = SimVerdict.STRETCHED
                title = "High Impact / Very Tight"
                desc = "This purchase uses ${((monthlyImpact / remainingBefore) * 100).toInt()}% of your remaining spendable funds, lowering your daily allowance from Rs. ${String.format(Locale.getDefault(), "%,.0f", dailyBefore)}/day to Rs. ${String.format(Locale.getDefault(), "%,.0f", dailyAfter)}/day (-${dailyDropPct.toInt()}%)."
                recommendation = "Proceed with caution. Leaves very little discretionary margin for unexpected expenses in the remaining ${analysis.daysRemainingInMonth} days."
            }
            remainingAfter < (analysis.budgetBasis * 0.28) || dailyDropPct > 20f -> {
                verdict = SimVerdict.MODERATE
                title = "Moderate Impact / Manageable"
                desc = "You can afford this. It consumes ${String.format(Locale.getDefault(), "%.1f", daysConsumed)} days of your daily budget, leaving you with Rs. ${String.format(Locale.getDefault(), "%,.0f", remainingAfter)} (Rs. ${String.format(Locale.getDefault(), "%,.0f", dailyAfter)}/day)."
                recommendation = "Affordable with good spending discipline for the rest of the month."
            }
            else -> {
                verdict = SimVerdict.COMFORTABLE
                title = "Comfortably Affordable"
                desc = "Great financial health! Even after this purchase, you retain Rs. ${String.format(Locale.getDefault(), "%,.0f", remainingAfter)} with a safe daily allowance of Rs. ${String.format(Locale.getDefault(), "%,.0f", dailyAfter)}/day."
                recommendation = "All recurring fixed costs (Rs. ${String.format(Locale.getDefault(), "%,.0f", analysis.totalRecurringFixedCosts)}) and safety buffers remain fully intact."
            }
        }

        return PurchaseSimulation(
            itemName = name,
            totalCost = price,
            category = category,
            tenureMonths = safeTenure,
            monthlyCost = monthlyImpact,
            remainingBefore = remainingBefore,
            remainingAfter = remainingAfter,
            dailySpendBefore = dailyBefore,
            dailySpendAfter = dailyAfter,
            dailyDropAmount = dailyDrop,
            dailyDropPercentage = dailyDropPct,
            daysOfDailyBudgetConsumed = daysConsumed,
            verdict = verdict,
            verdictTitle = title,
            verdictDescription = desc,
            recommendation = recommendation
        )
    }
}

/**
 * Data point for an individual day in the 30-day daily spending trend.
 */
data class DailySpendingPoint(
    val dayLabel: String,
    val fullDateLabel: String,
    val dateMillis: Long,
    val amount: Double,
    val transactionCount: Int,
    val isWeekend: Boolean
)

/**
 * 30-day daily spending trend summary with statistical pattern insights.
 */
data class ThirtyDaySpendingTrend(
    val dailyPoints: List<DailySpendingPoint>,
    val totalAmount: Double,
    val averageDaily: Double,
    val peakDay: DailySpendingPoint?,
    val patternInsight: String
)

