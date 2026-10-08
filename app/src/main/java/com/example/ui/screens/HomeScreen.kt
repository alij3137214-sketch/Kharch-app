package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.BillReminderEntity
import com.example.data.model.CommitteeEntity
import com.example.data.model.DebtDirection
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.WishItemEntity
import com.example.data.model.WishStatus
import com.example.data.profile.Goal
import com.example.data.profile.IncomeType
import com.example.domain.DAY_MS
import com.example.domain.MoneyMath
import com.example.ui.components.BalanceCard
import com.example.ui.components.DayHeader
import com.example.ui.components.HomeBudgetAlertBanner
import com.example.ui.components.IconBadge
import com.example.ui.components.KharchCard
import com.example.ui.components.QuickActions
import com.example.ui.components.ScreenPadding
import com.example.ui.components.SectionHeader
import com.example.ui.components.TransactionDetailSheet
import com.example.ui.components.TransactionItem
import com.example.ui.components.dayLabel
import com.example.ui.components.entrance
import com.example.ui.components.formatRs
import com.example.ui.components.pressable
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.TransferBlue
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.KharchUiState
import java.util.Calendar

/** The cards that can appear on Home. Their order depends on what the person said they want. */
enum class HomeCard { SET_LIMIT, TODAY, LASTS, BILLS, COMMITTEE, GOAL, WAIT, ACCOUNTS, UDHAAR, STREAK }

fun dashboardOrder(goal: Goal, incomeType: IncomeType): List<HomeCard> {
    val base = when (goal) {
        Goal.TRACK -> listOf(HomeCard.SET_LIMIT, HomeCard.TODAY, HomeCard.ACCOUNTS, HomeCard.STREAK, HomeCard.BILLS, HomeCard.COMMITTEE, HomeCard.WAIT, HomeCard.UDHAAR, HomeCard.GOAL)
        Goal.SAVE -> listOf(HomeCard.GOAL, HomeCard.SET_LIMIT, HomeCard.TODAY, HomeCard.STREAK, HomeCard.ACCOUNTS, HomeCard.WAIT, HomeCard.BILLS, HomeCard.COMMITTEE, HomeCard.UDHAAR)
        Goal.CONTROL -> listOf(HomeCard.SET_LIMIT, HomeCard.TODAY, HomeCard.STREAK, HomeCard.WAIT, HomeCard.ACCOUNTS, HomeCard.BILLS, HomeCard.COMMITTEE, HomeCard.GOAL, HomeCard.UDHAAR)
        Goal.BILLS -> listOf(HomeCard.BILLS, HomeCard.COMMITTEE, HomeCard.SET_LIMIT, HomeCard.TODAY, HomeCard.ACCOUNTS, HomeCard.GOAL, HomeCard.UDHAAR, HomeCard.WAIT, HomeCard.STREAK)
    }
    // People paid daily or with an irregular income care more about "how long will my money last".
    return if (incomeType == IncomeType.MONTHLY) base
    else base.toMutableList().also { it.add(1.coerceAtMost(it.size), HomeCard.LASTS) }
}

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    uiState: KharchUiState,
    onToggleBalanceVisibility: () -> Unit,
    onQuickExpense: () -> Unit,
    onQuickIncome: () -> Unit,
    onQuickTransfer: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onNavigateToActivity: () -> Unit = {},
    onNavigateToPlan: () -> Unit = {},
    onOpenUdhaar: () -> Unit = {},
    onOpenCommittee: () -> Unit = {},
    onOpenWishList: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenDeleteData: () -> Unit = {},
    onPayBill: (BillReminderEntity) -> Unit = {},
    onPayCommittee: (CommitteeEntity) -> Unit = {},
    onCommitteePayout: (CommitteeEntity) -> Unit = {},
    onBuyWish: (WishItemEntity) -> Unit = {},
    onDropWish: (WishItemEntity) -> Unit = {}
) {
    var selectedTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    val recent = remember(uiState.transactions) { uiState.transactions.take(6) }
    val grouped = remember(recent) { recent.groupBy { dayLabel(it.timestamp) } }
    val order = remember(uiState.profile.goal, uiState.profile.incomeType) {
        dashboardOrder(uiState.profile.goal, uiState.profile.incomeType)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            HomeHeader(name = uiState.profile.name, onOpenSettings = onOpenSettings, onOpenDeleteData = onOpenDeleteData)
        }

        item {
            Box(modifier = Modifier.entrance(0)) {
                BalanceCard(
                    totalBalance = uiState.totalBalance,
                    totalIncome = uiState.totalIncome,
                    totalExpense = uiState.totalExpense,
                    isBalanceHidden = uiState.isBalanceHidden,
                    availablePercentage = uiState.availableBudgetPercentage,
                    showBudget = uiState.hasMonthlyLimit,
                    upcomingBill = null,
                    onToggleVisibility = onToggleBalanceVisibility
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(14.dp))
            Box(modifier = Modifier.entrance(1)) {
                QuickActions(onExpenseClick = onQuickExpense, onIncomeClick = onQuickIncome, onTransferClick = onQuickTransfer)
            }
        }

        item {
            HomeBudgetAlertBanner(
                exceededBudgets = uiState.exceededBudgets,
                warningBudgets = uiState.warningBudgets,
                onManageBudgetsClick = onNavigateToPlan
            )
        }

        var cardIndex = 2
        order.forEach { card ->
            val idx = cardIndex
            item(key = "card_${card.name}") {
                val shown = DashboardCard(
                    card = card,
                    state = uiState,
                    index = idx,
                    onNavigateToPlan = onNavigateToPlan,
                    onOpenUdhaar = onOpenUdhaar,
                    onOpenCommittee = onOpenCommittee,
                    onOpenWishList = onOpenWishList,
                    onPayBill = onPayBill,
                    onPayCommittee = onPayCommittee,
                    onCommitteePayout = onCommitteePayout,
                    onBuyWish = onBuyWish,
                    onDropWish = onDropWish
                )
                if (!shown) Spacer(Modifier.height(0.dp))
            }
            cardIndex++
        }

        item {
            Spacer(modifier = Modifier.height(22.dp))
            SectionHeader(
                title = "Latest",
                actionLabel = if (uiState.transactions.isNotEmpty()) "See all" else null,
                onAction = onNavigateToActivity,
                modifier = Modifier.entrance(cardIndex)
            )
        }

        if (recent.isEmpty()) {
            item { EmptyRecent(onAdd = onQuickExpense) }
        } else {
            grouped.forEach { (label, items) ->
                item(key = "hdr_$label") {
                    DayHeader(
                        label = label,
                        total = items.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
                    )
                }
                items.forEach { tx ->
                    item(key = tx.id) {
                        TransactionItem(transaction = tx, onClick = { selectedTransaction = tx })
                    }
                }
            }
        }
    }

    selectedTransaction?.let { tx ->
        TransactionDetailSheet(
            transaction = tx,
            profile = uiState.profile,
            onDismiss = { selectedTransaction = null },
            onEdit = {
                selectedTransaction = null
                onEditTransaction(it)
            },
            onDelete = {
                selectedTransaction = null
                onDeleteTransaction(it)
            }
        )
    }
}

/** Returns false when the card has nothing to show (so no empty gap is left). */
@Composable
private fun DashboardCard(
    card: HomeCard,
    state: KharchUiState,
    index: Int,
    onNavigateToPlan: () -> Unit,
    onOpenUdhaar: () -> Unit,
    onOpenCommittee: () -> Unit,
    onOpenWishList: () -> Unit,
    onPayBill: (BillReminderEntity) -> Unit,
    onPayCommittee: (CommitteeEntity) -> Unit,
    onCommitteePayout: (CommitteeEntity) -> Unit,
    onBuyWish: (WishItemEntity) -> Unit,
    onDropWish: (WishItemEntity) -> Unit
): Boolean {
    val wrap: @Composable (@Composable () -> Unit) -> Unit = { body ->
        Box(modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 10.dp).entrance(index)) { body() }
    }
    when (card) {
        HomeCard.SET_LIMIT -> {
            if (state.hasMonthlyLimit) return false
            wrap {
                KharchCard(onClick = onNavigateToPlan, contentPadding = 16.dp) {
                    CardRow(
                        icon = Icons.Default.Tune,
                        tint = EmeraldPrimary,
                        title = "Set your monthly limit",
                        subtitle = "Then we can tell you how much to spend each day."
                    )
                }
            }
        }

        HomeCard.TODAY -> {
            val a = state.dailyAllowance ?: return false
            wrap { TodayCard(a.leftToday, a.perDay, a.spentToday, a.reservedForBills) }
        }

        HomeCard.LASTS -> {
            val days = MoneyMath.daysMoneyWillLast(state.totalBalance, state.transactions) ?: return false
            wrap {
                KharchCard(contentPadding = 16.dp) {
                    CardRow(
                        icon = Icons.Default.Today,
                        tint = TransferBlue,
                        title = if (days <= 0) "Your money is finished" else "Your money will last about $days ${if (days == 1) "day" else "days"}",
                        subtitle = "If you spend like in the last 2 weeks."
                    )
                }
            }
        }

        HomeCard.BILLS -> {
            val now = System.currentTimeMillis()
            val bills = state.billReminders.filter { !it.isPaid }.sortedBy { it.dueDate }.take(3)
            if (bills.isEmpty()) return false
            wrap {
                KharchCard(contentPadding = 16.dp) {
                    Text("Bills to pay", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(6.dp))
                    bills.forEach { bill ->
                        val days = ((MoneyMath.startOfDay(bill.dueDate) - MoneyMath.startOfDay(now)) / DAY_MS).toInt()
                        val late = days < 0
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(bill.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    text = when {
                                        late -> "Late by ${-days} ${if (-days == 1) "day" else "days"}"
                                        days == 0 -> "Due today"
                                        days == 1 -> "Due tomorrow"
                                        else -> "Due in $days days"
                                    } + " · " + formatRs(bill.amount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (late) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            SmallAction("Paid", onClick = { onPayBill(bill) })
                        }
                    }
                }
            }
        }

        HomeCard.COMMITTEE -> {
            val active = state.committees.filter { !it.isFinished }
            if (active.isEmpty()) return false
            wrap {
                KharchCard(onClick = onOpenCommittee, contentPadding = 16.dp) {
                    Text("Committee", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(6.dp))
                    active.take(2).forEach { c ->
                        val s = MoneyMath.committeeStatus(c)
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(c.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    text = when {
                                        s.myTurnNow -> "It is your turn! You get ${formatRs(c.pot)}"
                                        s.paymentDue -> "Pay ${formatRs(c.monthlyAmount)} this month"
                                        else -> "Next payment: ${s.nextPaymentLabel}"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (s.myTurnNow) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            when {
                                s.myTurnNow -> SmallAction("Got it", onClick = { onCommitteePayout(c) })
                                s.paymentDue -> SmallAction("Paid", onClick = { onPayCommittee(c) })
                            }
                        }
                    }
                }
            }
        }

        HomeCard.GOAL -> {
            val goal = state.savingsGoals.firstOrNull { it.currentAmount < it.targetAmount } ?: return false
            wrap {
                KharchCard(onClick = onNavigateToPlan, contentPadding = 16.dp) {
                    CardRow(
                        icon = Icons.Default.Savings,
                        tint = EmeraldPrimary,
                        title = goal.title,
                        subtitle = "${formatRs(goal.currentAmount)} of ${formatRs(goal.targetAmount)}"
                    )
                    Spacer(Modifier.height(12.dp))
                    ProgressBar((goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f), EmeraldPrimary)
                }
            }
        }

        HomeCard.WAIT -> {
            val waiting = state.wishItems.filter { it.status == WishStatus.WAITING.name }
            if (waiting.isEmpty()) return false
            val ready = waiting.filter { MoneyMath.daysLeftToWait(it.addedAt, it.waitDays) == 0 }
            wrap {
                KharchCard(onClick = onOpenWishList, contentPadding = 16.dp) {
                    if (ready.isEmpty()) {
                        CardRow(
                            icon = Icons.Default.HourglassEmpty,
                            tint = WarningAmber,
                            title = "${waiting.size} ${if (waiting.size == 1) "thing is" else "things are"} waiting",
                            subtitle = "Wait before you buy. Many things are not needed later."
                        )
                    } else {
                        val item = ready.first()
                        Text("Still want it?", style = MaterialTheme.typography.labelLarge, color = WarningAmber)
                        Spacer(Modifier.height(4.dp))
                        Text(item.title + " · " + formatRs(item.price), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text("You waited ${item.waitDays} ${if (item.waitDays == 1) "day" else "days"}.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SmallAction("Buy it", onClick = { onBuyWish(item) })
                            SmallAction("No, skip", onClick = { onDropWish(item) }, quiet = true)
                        }
                    }
                }
            }
        }

        HomeCard.ACCOUNTS -> {
            val accounts = state.accounts.filter { kotlin.math.abs(it.balance) >= 1.0 }
            if (accounts.size < 2) return false
            wrap {
                KharchCard(contentPadding = 16.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Where your money is", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(Modifier.height(8.dp))
                    accounts.take(5).forEach { acc ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(acc.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = (if (acc.balance < 0) "−" else "") + formatRs(kotlin.math.abs(acc.balance)),
                                style = MaterialTheme.typography.titleSmall,
                                color = if (acc.balance < 0) ExpenseRed else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        HomeCard.UDHAAR -> {
            val open = state.debts.filter { !it.isSettled }
            if (open.isEmpty()) return false
            val owedToMe = open.filter { it.direction == DebtDirection.LENT.name }.sumOf { it.remaining }
            val iOwe = open.filter { it.direction == DebtDirection.BORROWED.name }.sumOf { it.remaining }
            wrap {
                KharchCard(onClick = onOpenUdhaar, contentPadding = 16.dp) {
                    CardRow(
                        icon = Icons.Default.People,
                        tint = TransferBlue,
                        title = "Udhaar",
                        subtitle = buildList {
                            if (owedToMe > 0) add("People owe you ${formatRs(owedToMe)}")
                            if (iOwe > 0) add("You owe ${formatRs(iOwe)}")
                        }.joinToString(" · ")
                    )
                }
            }
        }

        HomeCard.STREAK -> {
            val streak = MoneyMath.noSpendStreak(state.transactions)
            if (streak < 1) return false
            wrap {
                KharchCard(contentPadding = 16.dp) {
                    CardRow(
                        icon = Icons.Default.LocalFireDepartment,
                        tint = WarningAmber,
                        title = "$streak ${if (streak == 1) "day" else "days"} without spending",
                        subtitle = "Keep it going!"
                    )
                }
            }
        }
    }
    return true
}

@Composable
private fun TodayCard(leftToday: Double, perDay: Double, spentToday: Double, reserved: Double) {
    val over = leftToday < 0
    val color = when {
        over -> ExpenseRed
        perDay > 0 && leftToday < perDay * 0.25 -> WarningAmber
        else -> EmeraldPrimary
    }
    KharchCard(contentPadding = 18.dp, modifier = Modifier.testTag("home_today_card")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon = Icons.Default.Today, tint = color, size = 36.dp, iconSize = 18.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                text = if (over) "You went over today" else "You can spend today",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = formatRs(kotlin.math.abs(leftToday)),
            style = MaterialTheme.typography.displaySmall,
            color = color
        )
        Spacer(Modifier.height(12.dp))
        ProgressBar(if (perDay > 0) (spentToday / perDay).toFloat().coerceIn(0f, 1f) else if (spentToday > 0) 1f else 0f, color)
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Every day gets ${formatRs(perDay)}. Money you don't use today moves to tomorrow." +
                if (reserved > 0) " ${formatRs(reserved)} is kept for bills." else "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CardRow(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon = icon, tint = tint, size = 42.dp, iconSize = 20.dp)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ProgressBar(progress: Float, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(8.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
private fun SmallAction(text: String, onClick: () -> Unit, quiet: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = if (quiet) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier
            .pressable(pressedScale = 0.94f, onClick = onClick)
            .clip(RoundedCornerShape(12.dp))
            .background(if (quiet) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f) else EmeraldPrimary)
            .padding(horizontal = 16.dp, vertical = 9.dp)
    )
}

@Composable
private fun HomeHeader(name: String, onOpenSettings: () -> Unit, onOpenDeleteData: () -> Unit) {
    // Follows the phone clock, and keeps following it if the app stays open past the hour.
    val hour by produceState(initialValue = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        while (true) {
            delay(30_000)
            value = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        }
    }
    val greeting = MoneyMath.greetingFor(hour)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = ScreenPadding, end = 4.dp, top = 14.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = greeting, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            // The name is whatever the person typed. If they skipped it, a gentle way to add it.
            Text(
                text = if (name.isBlank()) "Add your name" else name,
                style = MaterialTheme.typography.headlineMedium,
                color = if (name.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier
                    .pressable(pressedScale = 0.97f, onClick = onOpenSettings)
                    .testTag("home_name")
            )
        }
        IconButton(onClick = onOpenDeleteData, modifier = Modifier.testTag("home_delete_data_button")) {
            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete data", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        }
        IconButton(onClick = onOpenSettings, modifier = Modifier.testTag("home_settings_button")) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun EmptyRecent(onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
        }
        Text("Nothing here yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text("Write down the first thing you spend and it will show here.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Button(
            onClick = onAdd,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = MaterialTheme.colorScheme.onPrimary)
        ) { Text("Add what I spent") }
    }
}
