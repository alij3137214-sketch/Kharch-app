package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BillReminderEntity
import com.example.data.model.BudgetEntity
import com.example.data.model.ExpenseCategory
import com.example.data.model.SavingsGoalEntity
import com.example.ui.components.AffordabilitySimulator
import com.example.ui.components.CategoryBudgetSettingDialog
import com.example.ui.components.MonthlySpendingPlanCard
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TransferBlue
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.AffordabilityResult
import com.example.ui.viewmodel.BudgetAlertStatus
import com.example.ui.viewmodel.CategoryBudgetStatus
import com.example.ui.viewmodel.KharchUiState
import com.example.ui.viewmodel.KharchViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun PlanScreen(
    modifier: Modifier = Modifier,
    uiState: KharchUiState,
    viewModel: KharchViewModel,
    onPlanExpense: ((itemName: String, amount: Double, category: String) -> Unit)? = null,
    onCreateGoal: ((title: String, targetAmount: Double) -> Unit)? = null
) {
    // Dialog states
    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<BudgetEntity?>(null) }
    var selectedBudgetFilter by remember { mutableStateOf("ALL") } // "ALL", "EXCEEDED", "WARNING", "SAFE"
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var showAddBillDialog by remember { mutableStateOf(false) }
    var depositGoalTarget by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("plan_screen"),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // Title Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "Financial Planning",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Monthly Spending Limit & Budgets",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // --- 1. MONTHLY SPENDING LIMIT & REMAINING BUDGET TRACKER ---
        item {
            MonthlySpendingPlanCard(
                uiState = uiState,
                onSetMonthlyLimit = { limit, threshold ->
                    viewModel.setMonthlySpendingLimit(limit, threshold)
                }
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // --- 2. AFFORDABILITY SIMULATOR ---
        item {
            AffordabilitySimulator(
                uiState = uiState,
                viewModel = viewModel,
                onPlanExpense = onPlanExpense,
                onCreateGoal = { title, target ->
                    if (onCreateGoal != null) {
                        onCreateGoal(title, target)
                    } else {
                        showAddGoalDialog = true
                    }
                }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- 2. CATEGORY BUDGETS ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category Budgets",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = { showAddBudgetDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_budget_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Budget",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Set Limit", style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Budget Health & Utilization Overview Card
        item {
            val totalBudget = uiState.budgets.sumOf { it.monthlyLimit }
            val totalSpent = uiState.categoryBudgetStatuses.sumOf { it.currentSpent }
            val exceededCount = uiState.exceededBudgets.size
            val warningCount = uiState.warningBudgets.size
            val safeCount = uiState.categoryBudgetStatuses.count { it.alertStatus == BudgetAlertStatus.SAFE }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Monthly Budget Utilization",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Rs. ${String.format(Locale.getDefault(), "%,.0f", totalSpent)} / Rs. ${String.format(Locale.getDefault(), "%,.0f", totalBudget)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val overallPct = if (totalBudget > 0) ((totalSpent / totalBudget) * 100).toInt() else 0
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    when {
                                        exceededCount > 0 -> ExpenseRed.copy(alpha = 0.15f)
                                        warningCount > 0 -> WarningAmber.copy(alpha = 0.15f)
                                        else -> EmeraldPrimary.copy(alpha = 0.15f)
                                    }
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$overallPct% Spent",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    exceededCount > 0 -> ExpenseRed
                                    warningCount > 0 -> WarningAmber
                                    else -> EmeraldPrimary
                                }
                            )
                        }
                    }

                    // Stat summary pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Exceeded Pill
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (exceededCount > 0) ExpenseRed.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface)
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$exceededCount",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (exceededCount > 0) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Exceeded",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = if (exceededCount > 0) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Warning Pill
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (warningCount > 0) WarningAmber.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface)
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$warningCount",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (warningCount > 0) WarningAmber else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Near Limit",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = if (warningCount > 0) WarningAmber else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Safe Pill
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (safeCount > 0) EmeraldPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface)
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$safeCount",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (safeCount > 0) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "On Track",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = if (safeCount > 0) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Filter chips row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "ALL" to "All (${uiState.categoryBudgetStatuses.size})",
                            "EXCEEDED" to "🚨 Over ($exceededCount)",
                            "WARNING" to "⚡ Alert ($warningCount)",
                            "SAFE" to "🟢 Safe ($safeCount)"
                        ).forEach { (filterKey, label) ->
                            val isSelected = selectedBudgetFilter == filterKey
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedBudgetFilter = filterKey },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        val filteredBudgets = uiState.categoryBudgetStatuses.filter {
            when (selectedBudgetFilter) {
                "EXCEEDED" -> it.alertStatus == BudgetAlertStatus.EXCEEDED
                "WARNING" -> it.alertStatus == BudgetAlertStatus.WARNING
                "SAFE" -> it.alertStatus == BudgetAlertStatus.SAFE
                else -> true
            }
        }

        if (filteredBudgets.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (uiState.budgets.isEmpty()) "No budgets set yet. Tap '+ Set Limit' to establish category spending limits."
                        else "No budgets match the selected filter.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(filteredBudgets, key = { it.budget.id }) { item ->
                val cat = ExpenseCategory.fromString(item.category)
                val pctInt = (item.spentPercentage * 100).toInt()
                val barColor = when (item.alertStatus) {
                    BudgetAlertStatus.EXCEEDED -> ExpenseRed
                    BudgetAlertStatus.WARNING -> WarningAmber
                    BudgetAlertStatus.SAFE -> EmeraldPrimary
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 5.dp)
                        .clickable { editingBudget = item.budget }
                        .testTag("budget_item_${item.category.lowercase()}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (item.alertStatus) {
                            BudgetAlertStatus.EXCEEDED -> ExpenseRed.copy(alpha = 0.05f)
                            BudgetAlertStatus.WARNING -> WarningAmber.copy(alpha = 0.05f)
                            BudgetAlertStatus.SAFE -> MaterialTheme.colorScheme.surface
                        }
                    ),
                    border = when (item.alertStatus) {
                        BudgetAlertStatus.EXCEEDED -> androidx.compose.foundation.BorderStroke(1.2.dp, ExpenseRed.copy(alpha = 0.35f))
                        BudgetAlertStatus.WARNING -> androidx.compose.foundation.BorderStroke(1.2.dp, WarningAmber.copy(alpha = 0.35f))
                        BudgetAlertStatus.SAFE -> null
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(cat.color.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = cat.icon(),
                                        contentDescription = null,
                                        tint = cat.color,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = item.category,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Limit: Rs. ${String.format(Locale.getDefault(), "%,.0f", item.monthlyLimit)} · Alert at ${item.alertThresholdPercent}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when (item.alertStatus) {
                                                BudgetAlertStatus.EXCEEDED -> ExpenseRed
                                                BudgetAlertStatus.WARNING -> WarningAmber
                                                BudgetAlertStatus.SAFE -> EmeraldPrimary.copy(alpha = 0.15f)
                                            }
                                        )
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = when (item.alertStatus) {
                                            BudgetAlertStatus.EXCEEDED -> "🚨 $pctInt%"
                                            BudgetAlertStatus.WARNING -> "⚡ $pctInt%"
                                            BudgetAlertStatus.SAFE -> "$pctInt%"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = when (item.alertStatus) {
                                            BudgetAlertStatus.EXCEEDED, BudgetAlertStatus.WARNING -> Color.White
                                            BudgetAlertStatus.SAFE -> EmeraldPrimary
                                        },
                                        fontSize = 11.sp
                                    )
                                }

                                IconButton(
                                    onClick = { editingBudget = item.budget },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Budget",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.deleteBudget(item.budget) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Progress Indicator
                        LinearProgressIndicator(
                            progress = { item.spentPercentage.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(3.5.dp)),
                            color = barColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        // Bottom status description
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Spent: Rs. ${String.format(Locale.getDefault(), "%,.0f", item.currentSpent)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = when (item.alertStatus) {
                                    BudgetAlertStatus.EXCEEDED -> "Exceeded by Rs. ${String.format(Locale.getDefault(), "%,.0f", item.overAmount)}"
                                    BudgetAlertStatus.WARNING -> "Rs. ${String.format(Locale.getDefault(), "%,.0f", item.remaining)} left (Warning zone)"
                                    BudgetAlertStatus.SAFE -> "Rs. ${String.format(Locale.getDefault(), "%,.0f", item.remaining)} left"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (item.alertStatus != BudgetAlertStatus.SAFE) FontWeight.Bold else FontWeight.Normal,
                                color = barColor
                            )
                        }
                    }
                }
            }
        }

        // --- 3. SAVINGS GOALS ---
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Savings Goals",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = { showAddGoalDialog = true },
                    modifier = Modifier.testTag("add_savings_goal_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Goal",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (uiState.savingsGoals.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No savings goals yet. Tap + to set one up.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(uiState.savingsGoals, key = { it.id }) { goal ->
                val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat() else 0f

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 5.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(goal.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                if (goal.targetDate.isNotBlank()) {
                                    Text("Target: ${goal.targetDate}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { depositGoalTarget = goal },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("+ Deposit", style = MaterialTheme.typography.labelSmall)
                                }

                                IconButton(
                                    onClick = { viewModel.deleteSavingsGoal(goal) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = IncomeGreen,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Saved: Rs. ${String.format(Locale.getDefault(), "%,.0f", goal.currentAmount)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = IncomeGreen
                            )
                            Text(
                                "Target: Rs. ${String.format(Locale.getDefault(), "%,.0f", goal.targetAmount)} (${(progress * 100).toInt()}%)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // --- 4. BILL REMINDERS ---
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Upcoming Bills",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = { showAddBillDialog = true },
                    modifier = Modifier.testTag("add_bill_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Bill",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (uiState.billReminders.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No bills to pay! Tap + to add recurring bills.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(uiState.billReminders, key = { it.id }) { bill ->
                val daysLeft = ((bill.dueDate - System.currentTimeMillis()) / 86400000L).coerceAtLeast(0)
                val statusText = when {
                    bill.isPaid -> "Paid"
                    daysLeft == 0L -> "Due today"
                    daysLeft == 1L -> "Due tomorrow"
                    else -> "Due in $daysLeft days"
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 5.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (bill.isPaid) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (bill.isPaid) IncomeGreen.copy(alpha = 0.15f) else ExpenseRed.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (bill.isPaid) Icons.Default.Check else Icons.Default.ElectricBolt,
                                    contentDescription = null,
                                    tint = if (bill.isPaid) IncomeGreen else ExpenseRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column {
                                Text(bill.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "$statusText · Rs. ${String.format(Locale.getDefault(), "%,.0f", bill.amount)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (bill.isPaid) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!bill.isPaid) {
                                Button(
                                    onClick = { viewModel.markBillAsPaid(bill) },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Mark Paid", style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            IconButton(
                                onClick = { viewModel.deleteBillReminder(bill) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // --- 5. DATA SETTINGS / SAMPLE DATA ---
        item {
            Spacer(modifier = Modifier.height(28.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Data Management", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Need to restart or load example transactions?", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    OutlinedButton(
                        onClick = { showResetConfirmDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset & Seed Example Data")
                    }
                }
            }
        }
    }

    // --- DIALOGS ---

    // Add / Edit Budget Dialog
    if (showAddBudgetDialog || editingBudget != null) {
        val targetBudget = editingBudget
        val currentSpent = if (targetBudget != null) {
            uiState.getCategoryCurrentMonthSpent(targetBudget.category)
        } else {
            0.0
        }

        CategoryBudgetSettingDialog(
            existingBudget = targetBudget,
            currentSpent = currentSpent,
            onDismiss = {
                showAddBudgetDialog = false
                editingBudget = null
            },
            onSave = { category, limit, thresholdPercent ->
                viewModel.saveBudget(category, limit, thresholdPercent)
                showAddBudgetDialog = false
                editingBudget = null
            },
            onDelete = { budget ->
                viewModel.deleteBudget(budget)
                showAddBudgetDialog = false
                editingBudget = null
            }
        )
    }

    // Add Goal Dialog
    if (showAddGoalDialog) {
        var goalTitle by remember { mutableStateOf("") }
        var goalTargetText by remember { mutableStateOf("") }
        var goalDate by remember { mutableStateOf("Dec 2026") }

        AlertDialog(
            onDismissRequest = { showAddGoalDialog = false },
            title = { Text("Create Savings Goal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = goalTitle, onValueChange = { goalTitle = it }, label = { Text("Goal Title (e.g. New Laptop)") }, singleLine = true)
                    OutlinedTextField(value = goalTargetText, onValueChange = { if (it.all { ch -> ch.isDigit() || ch == '.' }) goalTargetText = it }, label = { Text("Target Amount (Rs.)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    OutlinedTextField(value = goalDate, onValueChange = { goalDate = it }, label = { Text("Target Date (e.g. Nov 2026)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = goalTargetText.toDoubleOrNull() ?: 0.0
                        if (goalTitle.isNotBlank() && target > 0) {
                            viewModel.addSavingsGoal(goalTitle, target, goalDate)
                            showAddGoalDialog = false
                        }
                    }
                ) {
                    Text("Create Goal")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGoalDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Deposit Goal Dialog
    if (depositGoalTarget != null) {
        val goal = depositGoalTarget!!
        var depositText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { depositGoalTarget = null },
            title = { Text("Deposit to ${goal.title}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Current saved: Rs. ${String.format(Locale.getDefault(), "%,.0f", goal.currentAmount)} of Rs. ${String.format(Locale.getDefault(), "%,.0f", goal.targetAmount)}")
                    OutlinedTextField(
                        value = depositText,
                        onValueChange = { if (it.all { ch -> ch.isDigit() || ch == '.' }) depositText = it },
                        label = { Text("Deposit Amount (Rs.)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = depositText.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            viewModel.depositToSavingsGoal(goal, amt)
                            depositGoalTarget = null
                        }
                    }
                ) {
                    Text("Add Deposit")
                }
            },
            dismissButton = {
                TextButton(onClick = { depositGoalTarget = null }) { Text("Cancel") }
            }
        )
    }

    // Add Bill Dialog
    if (showAddBillDialog) {
        var billTitle by remember { mutableStateOf("") }
        var billAmountText by remember { mutableStateOf("") }
        var billDaysAhead by remember { mutableStateOf("7") }

        AlertDialog(
            onDismissRequest = { showAddBillDialog = false },
            title = { Text("Add Bill Reminder") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = billTitle, onValueChange = { billTitle = it }, label = { Text("Bill Title (e.g. Internet Bill)") }, singleLine = true)
                    OutlinedTextField(value = billAmountText, onValueChange = { if (it.all { ch -> ch.isDigit() || ch == '.' }) billAmountText = it }, label = { Text("Amount (Rs.)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    OutlinedTextField(value = billDaysAhead, onValueChange = { if (it.all { ch -> ch.isDigit() }) billDaysAhead = it }, label = { Text("Due in (Days)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = billAmountText.toDoubleOrNull() ?: 0.0
                        val days = billDaysAhead.toLongOrNull() ?: 7L
                        if (billTitle.isNotBlank() && amt > 0) {
                            val dueDate = System.currentTimeMillis() + (days * 86400000L)
                            viewModel.addBillReminder(billTitle, amt, dueDate, "Bills", "Bank")
                            showAddBillDialog = false
                        }
                    }
                ) {
                    Text("Add Bill")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBillDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Reset Confirm Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Reset to Sample Data") },
            text = { Text("This will reload realistic sample Pakistani Rupee (Rs.) transactions, budgets, goals, and upcoming bills.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetData()
                        showResetConfirmDialog = false
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }
}
