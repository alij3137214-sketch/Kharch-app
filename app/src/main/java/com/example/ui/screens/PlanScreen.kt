package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.BillReminderEntity
import com.example.data.model.BudgetEntity
import com.example.data.model.ExpenseCategory
import com.example.data.model.SavingsGoalEntity
import com.example.domain.DAY_MS
import com.example.domain.MoneyMath
import com.example.ui.components.CategoryBudgetSettingDialog
import com.example.ui.components.FormSheet
import com.example.ui.components.IconBadge
import com.example.ui.components.KharchCard
import com.example.ui.components.MoneyField
import com.example.ui.components.PickChip
import com.example.ui.components.PrimaryButton
import com.example.ui.components.ScreenPadding
import com.example.ui.components.SoftButton
import com.example.ui.components.TextBox
import com.example.ui.components.entrance
import com.example.ui.components.formatRs
import com.example.ui.components.pressable
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.TransferBlue
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.BudgetAlertStatus
import com.example.ui.viewmodel.KharchUiState
import com.example.ui.viewmodel.KharchViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class PlanSheet { NONE, LIMIT, BILL, GOAL }

/** Limits, bills and goals. Everything here is something the person decides. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlanScreen(
    modifier: Modifier = Modifier,
    uiState: KharchUiState,
    viewModel: KharchViewModel,
    onOpenCanBuy: () -> Unit = {},
    onOpenWishList: () -> Unit = {},
    onOpenUdhaar: () -> Unit = {},
    onOpenCommittee: () -> Unit = {}
) {
    var sheet by remember { mutableStateOf(PlanSheet.NONE) }
    var editingBudget by remember { mutableStateOf<BudgetEntity?>(null) }
    var showNewBudget by remember { mutableStateOf(false) }
    var depositTo by remember { mutableStateOf<SavingsGoalEntity?>(null) }

    val limit = uiState.monthlyLimitOrNull
    val spent = uiState.currentMonthTotalExpense
    val statuses = uiState.categoryBudgetStatuses
    val unpaidBills = uiState.billReminders.filter { !it.isPaid }.sortedBy { it.dueDate }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("plan_screen"),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            Column(Modifier.padding(horizontal = ScreenPadding, vertical = 14.dp)) {
                Text("Plan", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("Your limits, bills and goals", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // This month
        item {
            Box(Modifier.padding(horizontal = ScreenPadding, vertical = 6.dp).entrance(0)) {
                KharchCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("This month", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        SoftButton(if (limit == null) "Set a limit" else "Change", onClick = { sheet = PlanSheet.LIMIT }, color = EmeraldPrimary)
                    }
                    Spacer(Modifier.height(8.dp))
                    if (limit == null) {
                        Text(
                            "Choose how much you want to spend in one month. We will tell you how much you can spend each day.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val left = limit - spent
                        Text(
                            text = if (left >= 0) formatRs(left) + " left" else formatRs(-left) + " over",
                            style = MaterialTheme.typography.displaySmall,
                            color = if (left >= 0) MaterialTheme.colorScheme.onSurface else ExpenseRed
                        )
                        Spacer(Modifier.height(10.dp))
                        Bar((spent / limit).toFloat().coerceIn(0f, 1f), if (left < 0) ExpenseRed else EmeraldPrimary)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "You spent ${formatRs(spent)} of ${formatRs(limit)}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Limits for each kind
        item {
            Spacer(Modifier.height(14.dp))
            SectionTitle("Limits for each kind", actionLabel = "Add", onAction = { showNewBudget = true })
        }
        if (statuses.isEmpty()) {
            item {
                Text(
                    "No limits yet. A limit for Food or Shopping helps you stop early.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = ScreenPadding, vertical = 6.dp)
                )
            }
        } else {
            items(statuses.size) { i ->
                val s = statuses[i]
                val cat = ExpenseCategory.fromString(s.category)
                val color = when (s.alertStatus) {
                    BudgetAlertStatus.EXCEEDED -> ExpenseRed
                    BudgetAlertStatus.WARNING -> WarningAmber
                    BudgetAlertStatus.SAFE -> EmeraldPrimary
                }
                Box(Modifier.padding(horizontal = ScreenPadding, vertical = 5.dp)) {
                    KharchCard(onClick = { editingBudget = s.budget }, contentPadding = 14.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(icon = cat.icon(), tint = cat.color, size = 38.dp, iconSize = 19.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(s.category, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    text = if (s.alertStatus == BudgetAlertStatus.EXCEEDED) "${formatRs(s.overAmount)} over" else "${formatRs(s.remaining)} left",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (s.alertStatus == BudgetAlertStatus.EXCEEDED) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text("${formatRs(s.currentSpent)} / ${formatRs(s.monthlyLimit)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(10.dp))
                        Bar(s.spentPercentage.coerceIn(0f, 1f), color)
                    }
                }
            }
        }

        // Bills
        item {
            Spacer(Modifier.height(18.dp))
            SectionTitle("Bills", actionLabel = "Add", onAction = { sheet = PlanSheet.BILL })
        }
        if (unpaidBills.isEmpty()) {
            item {
                Text(
                    "No bills to pay. Add rent, electricity or school fees and we will remind you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = ScreenPadding, vertical = 6.dp)
                )
            }
        } else {
            items(unpaidBills.size) { i ->
                BillRow(unpaidBills[i], onPaid = { viewModel.markBillAsPaid(unpaidBills[i]) }, onDelete = { viewModel.deleteBillReminder(unpaidBills[i]) })
            }
        }

        // Goals
        item {
            Spacer(Modifier.height(18.dp))
            SectionTitle("Piggy bank goals", actionLabel = "Add", onAction = { sheet = PlanSheet.GOAL })
        }
        if (uiState.savingsGoals.isEmpty()) {
            item {
                Text(
                    "Saving for something? Add a goal and put money in a little at a time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = ScreenPadding, vertical = 6.dp)
                )
            }
        } else {
            items(uiState.savingsGoals.size) { i ->
                val g = uiState.savingsGoals[i]
                val progress = (g.currentAmount / g.targetAmount).toFloat().coerceIn(0f, 1f)
                Box(Modifier.padding(horizontal = ScreenPadding, vertical = 5.dp)) {
                    KharchCard(contentPadding = 14.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(g.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    "${formatRs(g.currentAmount)} of ${formatRs(g.targetAmount)}" + if (g.targetDate.isNotBlank()) " · by ${g.targetDate}" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (progress < 1f) {
                                Text(
                                    "Add money",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier
                                        .pressable(pressedScale = 0.94f) { depositTo = g }
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(EmeraldPrimary)
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            } else {
                                Text("Done!", style = MaterialTheme.typography.labelLarge, color = EmeraldPrimary)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Bar(progress, EmeraldPrimary)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            SoftButton("Delete", onClick = { viewModel.deleteSavingsGoal(g) })
                        }
                    }
                }
            }
        }

        // Tools
        item {
            Spacer(Modifier.height(18.dp))
            SectionTitle("Helpers")
            Spacer(Modifier.height(6.dp))
            Column(Modifier.padding(horizontal = ScreenPadding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ToolTile("Can I buy it?", "Check before you spend", Icons.Default.Calculate, EmeraldPrimary, onOpenCanBuy, Modifier.weight(1f))
                    ToolTile("Wait list", "Wait, then decide", Icons.Default.HourglassEmpty, WarningAmber, onOpenWishList, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ToolTile("Udhaar", "Money given and taken", Icons.Default.People, TransferBlue, onOpenUdhaar, Modifier.weight(1f))
                    ToolTile("Committee", "Kameti or BC", Icons.Default.Groups, Color(0xFFB59CF2), onOpenCommittee, Modifier.weight(1f))
                }
            }
        }
    }

    when (sheet) {
        PlanSheet.LIMIT -> LimitSheet(
            currentLimit = limit,
            currentThreshold = uiState.overallMonthlyBudget?.alertThresholdPercent ?: 80,
            onSave = { amount, percent -> viewModel.setMonthlySpendingLimit(amount, percent); sheet = PlanSheet.NONE },
            onDismiss = { sheet = PlanSheet.NONE }
        )
        PlanSheet.BILL -> BillSheet(
            onSave = { title, amount, due, repeat ->
                viewModel.addBillReminder(title, amount, due, "Bills", "Cash", repeat)
                sheet = PlanSheet.NONE
            },
            onDismiss = { sheet = PlanSheet.NONE }
        )
        PlanSheet.GOAL -> GoalSheet(
            onSave = { title, target, date -> viewModel.addSavingsGoal(title, target, date); sheet = PlanSheet.NONE },
            onDismiss = { sheet = PlanSheet.NONE }
        )
        PlanSheet.NONE -> Unit
    }

    if (showNewBudget || editingBudget != null) {
        val existing = editingBudget
        CategoryBudgetSettingDialog(
            existingBudget = existing,
            currentSpent = existing?.let { uiState.getCategoryCurrentMonthSpent(it.category) } ?: 0.0,
            onDismiss = { showNewBudget = false; editingBudget = null },
            onSave = { category, amount, percent ->
                viewModel.saveBudget(category, amount, percent)
                showNewBudget = false
                editingBudget = null
            },
            onDelete = { b ->
                viewModel.deleteBudget(b)
                editingBudget = null
            }
        )
    }

    depositTo?.let { goal ->
        DepositSheet(goal = goal, onSave = { amount -> viewModel.depositToSavingsGoal(goal, amount); depositTo = null }, onDismiss = { depositTo = null })
    }
}

@Composable
internal fun SectionTitle(title: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        if (actionLabel != null && onAction != null) {
            Row(
                modifier = Modifier
                    .pressable(pressedScale = 0.94f, onClick = onAction)
                    .clip(RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(actionLabel, style = MaterialTheme.typography.labelLarge, color = EmeraldPrimary)
            }
        }
    }
}

@Composable
internal fun Bar(progress: Float, color: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(8.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
private fun ToolTile(title: String, hint: String, icon: ImageVector, tint: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .pressable(pressedScale = 0.97f, onClick = onClick)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        IconBadge(icon = icon, tint = tint, size = 38.dp, iconSize = 19.dp)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BillRow(bill: BillReminderEntity, onPaid: () -> Unit, onDelete: () -> Unit) {
    val days = ((MoneyMath.startOfDay(bill.dueDate) - MoneyMath.startOfDay(System.currentTimeMillis())) / DAY_MS).toInt()
    val late = days < 0
    Box(Modifier.padding(horizontal = ScreenPadding, vertical = 5.dp)) {
        KharchCard(contentPadding = 14.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(bill.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = when {
                            late -> "Late by ${-days} ${if (-days == 1) "day" else "days"}"
                            days == 0 -> "Due today"
                            days == 1 -> "Due tomorrow"
                            else -> "Due ${SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(bill.dueDate))}"
                        } + " · " + formatRs(bill.amount) + if (bill.repeatMonthly) " · every month" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (late) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "Paid",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .pressable(pressedScale = 0.94f, onClick = onPaid)
                        .clip(RoundedCornerShape(12.dp))
                        .background(EmeraldPrimary)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                SoftButton("Delete", onClick = onDelete)
            }
        }
    }
}

@Composable
private fun LimitSheet(currentLimit: Double?, currentThreshold: Int, onSave: (Double, Int) -> Unit, onDismiss: () -> Unit) {
    var amount by remember { mutableStateOf(currentLimit?.let { if (it % 1 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    var percent by remember { mutableStateOf(currentThreshold) }
    val value = amount.toDoubleOrNull() ?: 0.0
    FormSheet(title = "Monthly limit", subtitle = "How much do you want to spend in one month?", onDismiss = onDismiss) {
        MoneyField(value = amount, onValueChange = { amount = it }, label = "Limit for one month")
        Text("Warn me when I have used", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(70, 80, 90).forEach { p -> PickChip("$p%", percent == p, { percent = p }) }
        }
        PrimaryButton("Save", onClick = { onSave(value, percent) }, enabled = value > 0)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BillSheet(onSave: (String, Double, Long, Boolean) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var dueInDays by remember { mutableStateOf(7) }
    var repeat by remember { mutableStateOf(true) }
    val value = amount.toDoubleOrNull() ?: 0.0
    FormSheet(title = "Add a bill", subtitle = "We will remind you before it is due.", onDismiss = onDismiss) {
        TextBox(value = title, onValueChange = { title = it }, label = "Name", placeholder = "Rent, electricity, fees")
        MoneyField(value = amount, onValueChange = { amount = it }, label = "How much")
        Text("When is it due?", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Today" to 0, "Tomorrow" to 1, "In 3 days" to 3, "In a week" to 7, "In 2 weeks" to 14, "In a month" to 30).forEach { (label, d) ->
                PickChip(label, dueInDays == d, { dueInDays = d })
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Every month", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Switch(checked = repeat, onCheckedChange = { repeat = it }, colors = SwitchDefaults.colors(checkedTrackColor = EmeraldPrimary, checkedThumbColor = MaterialTheme.colorScheme.onPrimary))
        }
        PrimaryButton(
            "Add bill",
            onClick = {
                val due = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, dueInDays)
                    set(Calendar.HOUR_OF_DAY, 9)
                    set(Calendar.MINUTE, 0)
                }.timeInMillis
                onSave(title.ifBlank { "Bill" }, value, due, repeat)
            },
            enabled = value > 0
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoalSheet(onSave: (String, Double, String) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var months by remember { mutableStateOf(6) }
    val value = amount.toDoubleOrNull() ?: 0.0
    FormSheet(title = "New goal", subtitle = "What are you saving for?", onDismiss = onDismiss) {
        TextBox(value = title, onValueChange = { title = it }, label = "Name", placeholder = "Phone, trip, emergency")
        MoneyField(value = amount, onValueChange = { amount = it }, label = "How much do you need")
        Text("In how many months?", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(3, 6, 12, 24).forEach { m -> PickChip("$m months", months == m, { months = m }) }
        }
        if (value > 0) {
            Text(
                "Put aside about ${formatRs(value / months)} each month.",
                style = MaterialTheme.typography.titleSmall,
                color = EmeraldPrimary
            )
        }
        PrimaryButton(
            "Create goal",
            onClick = {
                val by = Calendar.getInstance().apply { add(Calendar.MONTH, months) }
                onSave(title.ifBlank { "My goal" }, value, SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(by.time))
            },
            enabled = value > 0
        )
    }
}

@Composable
private fun DepositSheet(goal: SavingsGoalEntity, onSave: (Double) -> Unit, onDismiss: () -> Unit) {
    var amount by remember { mutableStateOf("") }
    val value = amount.toDoubleOrNull() ?: 0.0
    val room = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
    FormSheet(title = goal.title, subtitle = "${formatRs(room)} more to reach your goal.", onDismiss = onDismiss) {
        MoneyField(value = amount, onValueChange = { amount = it }, label = "How much are you adding")
        PrimaryButton("Add to goal", onClick = { onSave(value) }, enabled = value > 0)
    }
}
