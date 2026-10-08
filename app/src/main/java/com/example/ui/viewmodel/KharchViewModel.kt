package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BillReminderEntity
import com.example.data.model.BudgetEntity
import com.example.data.model.CommitteeEntity
import com.example.data.model.DebtEntity
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.WishItemEntity
import com.example.data.profile.BudgetPlanner
import com.example.data.profile.ProfileStore
import com.example.data.profile.UserProfile
import com.example.data.repository.KharchRepository
import com.example.domain.BudgetAlert
import com.example.domain.BudgetAlertLogic
import com.example.domain.MoneyMath
import com.example.domain.PeriodKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

/** One-time things the screen should react to (a message, or a budget notification). */
sealed interface AppEvent {
    data class Message(val text: String) : AppEvent
    data class BudgetNotice(val alert: BudgetAlert) : AppEvent
}

class KharchViewModel(
    private val repository: KharchRepository,
    val profileStore: ProfileStore = ProfileStore.memory()
) : ViewModel() {

    private val _isBalanceHidden = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedFilterType = MutableStateFlow("ALL")
    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    private val _periodKind = MutableStateFlow(PeriodKind.MONTH)
    private val _periodOffset = MutableStateFlow(0)
    private val _affordabilityBasisType = MutableStateFlow("INCOME")
    private val _affordabilityCustomBasis = MutableStateFlow(75000.0)
    private val _disabledFixedCostIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _customFixedCosts = MutableStateFlow<List<FixedCostItem>>(emptyList())

    private val _events = MutableSharedFlow<AppEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<AppEvent> = _events.asSharedFlow()

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<KharchUiState> = combine(
        listOf<Flow<*>>(
            repository.allTransactions,      // 0
            repository.allBudgets,           // 1
            repository.allSavingsGoals,      // 2
            repository.allBillReminders,     // 3
            repository.allWishItems,         // 4
            repository.allDebts,             // 5
            repository.allCommittees,        // 6
            profileStore.profile,            // 7
            _isBalanceHidden,                // 8
            _searchQuery,                    // 9
            _selectedFilterType,             // 10
            _selectedCategoryFilter,         // 11
            _periodKind,                     // 12
            _periodOffset,                   // 13
            _affordabilityBasisType,         // 14
            _affordabilityCustomBasis,       // 15
            _disabledFixedCostIds,           // 16
            _customFixedCosts                // 17
        )
    ) { v ->
        KharchUiState(
            transactions = v[0] as List<TransactionEntity>,
            budgets = v[1] as List<BudgetEntity>,
            savingsGoals = v[2] as List<SavingsGoalEntity>,
            billReminders = v[3] as List<BillReminderEntity>,
            wishItems = v[4] as List<WishItemEntity>,
            debts = v[5] as List<DebtEntity>,
            committees = v[6] as List<CommitteeEntity>,
            profile = v[7] as UserProfile,
            isBalanceHidden = v[8] as Boolean,
            searchQuery = v[9] as String,
            selectedFilterType = v[10] as String,
            selectedCategoryFilter = v[11] as String?,
            periodKind = v[12] as PeriodKind,
            periodOffset = v[13] as Int,
            affordabilityBasisType = v[14] as String,
            affordabilityCustomBasis = v[15] as Double,
            disabledFixedCostIds = v[16] as Set<Long>,
            customFixedCosts = v[17] as List<FixedCostItem>
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = KharchUiState(profile = profileStore.profile.value)
    )

    // ---- Small screen settings -------------------------------------------------------------

    fun toggleBalanceVisibility() = _isBalanceHidden.update { !it }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterType(filter: String) {
        _selectedFilterType.value = filter
    }

    fun selectCategoryFilter(category: String?) {
        _selectedCategoryFilter.value = if (_selectedCategoryFilter.value == category) null else category
    }

    fun setPeriodKind(kind: PeriodKind) {
        _periodKind.value = kind
        _periodOffset.value = 0
    }

    /** Moves to an earlier (negative) or later period. It never goes into the future. */
    fun changePeriodOffset(delta: Int) {
        _periodOffset.update { (it + delta).coerceAtMost(0) }
    }

    // ---- Records ---------------------------------------------------------------------------

    fun addExpense(
        title: String,
        amount: Double,
        category: String,
        paymentMethod: String,
        timestamp: Long = System.currentTimeMillis(),
        note: String = "",
        receiptUri: String? = null
    ) {
        // Work out the alerts before saving, so they compare "before" and "after".
        val alerts = if (profileStore.profile.value.budgetAlerts) {
            BudgetAlertLogic.alertsFor(uiState.value, category, amount)
        } else emptyList()
        viewModelScope.launch {
            repository.insertTransaction(
                TransactionEntity(
                    title = title.ifBlank { "Spending" },
                    amount = amount,
                    type = TransactionType.EXPENSE.name,
                    category = category,
                    paymentMethod = paymentMethod,
                    timestamp = timestamp,
                    note = note,
                    receiptUri = receiptUri
                )
            )
            alerts.forEach { _events.emit(AppEvent.BudgetNotice(it)) }
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
            repository.insertTransaction(
                TransactionEntity(
                    title = title.ifBlank { source },
                    amount = amount,
                    type = TransactionType.INCOME.name,
                    category = source,
                    paymentMethod = paymentMethod,
                    timestamp = timestamp,
                    note = note
                )
            )
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
            repository.insertTransaction(
                TransactionEntity(
                    title = "$fromMethod to $toMethod",
                    amount = amount,
                    type = TransactionType.TRANSFER.name,
                    category = "Transfer",
                    paymentMethod = fromMethod,
                    toPaymentMethod = toMethod,
                    timestamp = timestamp,
                    note = note
                )
            )
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch { repository.updateTransaction(transaction) }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch { repository.deleteTransaction(transaction) }
    }

    // ---- Limits ----------------------------------------------------------------------------

    private fun currentMonthKey(): String = MoneyMath.monthKey(System.currentTimeMillis())

    fun setMonthlySpendingLimit(limit: Double, alertThresholdPercent: Int = 80) {
        saveBudget("OVERALL", limit, alertThresholdPercent)
    }

    fun saveBudget(category: String, monthlyLimit: Double, alertThresholdPercent: Int = 80) {
        viewModelScope.launch { upsertBudget(category, monthlyLimit, alertThresholdPercent) }
    }

    private suspend fun upsertBudget(category: String, monthlyLimit: Double, alertThresholdPercent: Int) {
        val existing = repository.budgetsNow().firstOrNull {
            it.category.equals(category, ignoreCase = true) ||
                (category == "OVERALL" && it.category.equals("TOTAL", ignoreCase = true))
        }
        if (existing != null) {
            repository.updateBudget(
                existing.copy(
                    monthlyLimit = monthlyLimit,
                    alertThresholdPercent = alertThresholdPercent,
                    monthYear = currentMonthKey()
                )
            )
        } else {
            repository.insertBudget(
                BudgetEntity(
                    category = category,
                    monthlyLimit = monthlyLimit,
                    alertThresholdPercent = alertThresholdPercent,
                    monthYear = currentMonthKey()
                )
            )
        }
    }

    fun deleteBudget(budget: BudgetEntity) {
        viewModelScope.launch { repository.deleteBudget(budget) }
    }

    // ---- Goals -----------------------------------------------------------------------------

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
            val added = repository.depositToSavingsGoal(goal, amount)
            if (added <= 0.0) _events.emit(AppEvent.Message("This goal is already full."))
        }
    }

    fun deleteSavingsGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch { repository.deleteSavingsGoal(goal) }
    }

    // ---- Bills -----------------------------------------------------------------------------

    fun addBillReminder(
        title: String,
        amount: Double,
        dueDate: Long,
        category: String,
        paymentMethod: String,
        repeatMonthly: Boolean = false
    ) {
        viewModelScope.launch {
            repository.insertBillReminder(
                BillReminderEntity(
                    title = title,
                    amount = amount,
                    dueDate = dueDate,
                    category = category,
                    paymentMethod = paymentMethod,
                    repeatMonthly = repeatMonthly
                )
            )
        }
    }

    fun markBillAsPaid(bill: BillReminderEntity) {
        viewModelScope.launch { repository.markBillAsPaid(bill) }
    }

    fun deleteBillReminder(bill: BillReminderEntity) {
        viewModelScope.launch { repository.deleteBillReminder(bill) }
    }

    // ---- Wish list -------------------------------------------------------------------------

    fun addWish(title: String, price: Double, note: String = "") {
        val days = MoneyMath.waitDaysFor(price, profileStore.profile.value.monthlyIncome)
        viewModelScope.launch {
            repository.addWishItem(WishItemEntity(title = title.ifBlank { "Something I want" }, price = price, waitDays = days, note = note))
        }
    }

    fun buyWish(item: WishItemEntity, category: String = "Shopping", paymentMethod: String = "Cash") {
        viewModelScope.launch { repository.buyWishItem(item, category, paymentMethod) }
    }

    fun dropWish(item: WishItemEntity) {
        viewModelScope.launch { repository.dropWishItem(item) }
    }

    fun deleteWish(item: WishItemEntity) {
        viewModelScope.launch { repository.deleteWishItem(item) }
    }

    // ---- Udhaar ----------------------------------------------------------------------------

    fun addDebt(person: String, amount: Double, direction: String, note: String = "", dueDate: Long? = null) {
        viewModelScope.launch {
            repository.addDebt(DebtEntity(person = person.trim(), amount = amount, direction = direction, note = note, dueDate = dueDate))
        }
    }

    fun payDebt(debt: DebtEntity, amount: Double) {
        viewModelScope.launch { repository.payDebt(debt, amount) }
    }

    fun deleteDebt(debt: DebtEntity) {
        viewModelScope.launch { repository.deleteDebt(debt) }
    }

    // ---- Committees ------------------------------------------------------------------------

    fun addCommittee(name: String, monthlyAmount: Double, members: Int, myTurn: Int, startMonth: String) {
        viewModelScope.launch {
            repository.addCommittee(
                CommitteeEntity(
                    name = name.ifBlank { "My committee" },
                    monthlyAmount = monthlyAmount,
                    totalMembers = members.coerceAtLeast(2),
                    myTurn = myTurn.coerceIn(1, members.coerceAtLeast(2)),
                    startMonth = startMonth
                )
            )
        }
    }

    fun payCommitteeMonth(committee: CommitteeEntity, paymentMethod: String = "Cash") {
        viewModelScope.launch { repository.payCommitteeMonth(committee, paymentMethod) }
    }

    fun receiveCommitteePayout(committee: CommitteeEntity, paymentMethod: String = "Cash") {
        viewModelScope.launch { repository.receiveCommitteePayout(committee, paymentMethod) }
    }

    fun deleteCommittee(committee: CommitteeEntity) {
        viewModelScope.launch { repository.deleteCommittee(committee) }
    }

    // ---- The person's answers --------------------------------------------------------------

    /** Saves the answers and builds the first plan (monthly limit, category limits, monthly bills) from them. */
    fun completeOnboarding(answers: UserProfile) {
        val profile = answers.copy(onboardingDone = true)
        profileStore.save(profile)
        viewModelScope.launch {
            if (profile.monthlyIncome > 0) {
                val plan = BudgetPlanner.suggest(profile)
                upsertBudget("OVERALL", plan.monthlyLimit, 80)
                plan.categoryLimits.forEach { (category, limit) -> upsertBudget(category, limit, 80) }
            }
            if (profile.monthlyBills > 0 && repository.billsNow().none { it.repeatMonthly }) {
                val due = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, profile.billsDay.coerceIn(1, 28))
                    set(Calendar.HOUR_OF_DAY, 9)
                    set(Calendar.MINUTE, 0)
                    if (timeInMillis < System.currentTimeMillis()) add(Calendar.MONTH, 1)
                }.timeInMillis
                repository.insertBillReminder(
                    BillReminderEntity(
                        title = "Monthly bills",
                        amount = profile.monthlyBills,
                        dueDate = due,
                        category = "Bills",
                        paymentMethod = "Cash",
                        repeatMonthly = true
                    )
                )
            }
        }
    }

    fun updateProfile(change: (UserProfile) -> UserProfile) = profileStore.update(change)

    // ---- Removing data ---------------------------------------------------------------------

    /** [month] is 0-based like Calendar.MONTH. */
    fun deleteMonth(year: Int, month: Int) {
        viewModelScope.launch {
            repository.deleteTransactionsInMonth(year, month)
            _events.emit(AppEvent.Message("That month was deleted."))
        }
    }

    fun deleteAllTransactions() {
        viewModelScope.launch {
            repository.deleteAllTransactions()
            _events.emit(AppEvent.Message("All records were deleted."))
        }
    }

    /** Deletes every record, limit, bill, goal and answer. The app starts again from the questions. */
    fun deleteEverything() {
        viewModelScope.launch {
            repository.clearAllData()
            profileStore.reset()
            _events.emit(AppEvent.Message("Everything was deleted."))
        }
    }

    fun clearAllData() = deleteAllTransactions()

    fun seedRandomTestData() {
        viewModelScope.launch { repository.seedRandomTestingData() }
    }

    // ---- "Can I buy it?" -------------------------------------------------------------------

    fun setAffordabilityBasisType(basisType: String, customAmount: Double? = null) {
        _affordabilityBasisType.value = basisType
        if (customAmount != null) _affordabilityCustomBasis.value = customAmount
    }

    fun toggleAffordabilityFixedCost(id: Long) {
        _disabledFixedCostIds.update { if (it.contains(id)) it - id else it + id }
    }

    fun addAffordabilityCustomFixedCost(title: String, amount: Double, category: String = "Bills") {
        if (amount <= 0) return
        _customFixedCosts.update {
            it + FixedCostItem(
                id = System.currentTimeMillis(),
                title = title.ifBlank { "Another bill" },
                amount = amount,
                category = category,
                isPaid = false,
                isCustom = true,
                isEnabled = true
            )
        }
    }

    fun removeAffordabilityCustomFixedCost(id: Long) {
        _customFixedCosts.update { list -> list.filter { it.id != id } }
    }

    fun simulatePurchase(itemName: String, price: Double, category: String = "Shopping", tenureMonths: Int = 1): PurchaseSimulation {
        val state = uiState.value
        return state.simulatePurchaseAffordability(state.affordabilityAnalysis, itemName, price, category, tenureMonths)
    }
}
