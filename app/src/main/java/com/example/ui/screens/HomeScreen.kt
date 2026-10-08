package com.example.ui.screens

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
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BillReminderEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
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
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.viewmodel.KharchUiState
import java.util.Calendar

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    uiState: KharchUiState,
    onToggleBalanceVisibility: () -> Unit,
    onQuickExpense: () -> Unit,
    onQuickIncome: () -> Unit,
    onQuickTransfer: () -> Unit,
    onQuickScanReceipt: () -> Unit,
    onBillClick: (BillReminderEntity) -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onNavigateToActivity: () -> Unit = {},
    onNavigateToPlan: () -> Unit = {},
    onSeedRandomData: () -> Unit = {},
    onClearAllData: () -> Unit = {}
) {
    var selectedTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var showConfirmClearDialog by remember { mutableStateOf(false) }

    val recent = remember(uiState.transactions) { uiState.transactions.take(6) }
    val grouped = remember(recent) { recent.groupBy { dayLabel(it.timestamp) } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            HomeHeader(
                onSeedRandomData = onSeedRandomData,
                onClearAll = { showConfirmClearDialog = true }
            )
        }

        item {
            Box(modifier = Modifier.entrance(0)) {
                BalanceCard(
                    totalBalance = uiState.totalBalance,
                    totalIncome = uiState.totalIncome,
                    totalExpense = uiState.totalExpense,
                    isBalanceHidden = uiState.isBalanceHidden,
                    availablePercentage = uiState.availableBudgetPercentage,
                    upcomingBill = uiState.upcomingBill,
                    onToggleVisibility = onToggleBalanceVisibility,
                    onUpcomingBillClick = onBillClick
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(14.dp))
            Box(modifier = Modifier.entrance(1)) {
                QuickActions(
                    onExpenseClick = onQuickExpense,
                    onIncomeClick = onQuickIncome,
                    onTransferClick = onQuickTransfer,
                    onScanReceiptClick = onQuickScanReceipt
                )
            }
        }

        item {
            HomeBudgetAlertBanner(
                exceededBudgets = uiState.exceededBudgets,
                warningBudgets = uiState.warningBudgets,
                onManageBudgetsClick = onNavigateToPlan
            )
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            val aff = uiState.affordabilityAnalysis
            Box(modifier = Modifier.padding(horizontal = ScreenPadding).entrance(2)) {
                KharchCard(
                    onClick = onNavigateToPlan,
                    contentPadding = 16.dp,
                    modifier = Modifier.testTag("home_affordability_card")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(icon = Icons.Default.Calculate, tint = EmeraldPrimary, size = 42.dp, iconSize = 20.dp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Safe to spend",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (aff.isDeficit) "−" + formatRs(kotlin.math.abs(aff.remainingDiscretionary))
                                else formatRs(aff.remainingDiscretionary),
                                style = MaterialTheme.typography.titleLarge,
                                color = if (aff.isDeficit) ExpenseRed else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = formatRs(aff.safeDailySpend) + " a day for the rest of the month",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier.padding(horizontal = ScreenPadding).entrance(3)) {
                InsightLine(transactions = uiState.transactions)
            }
            Spacer(modifier = Modifier.height(22.dp))
        }

        item {
            SectionHeader(
                title = "Recent",
                actionLabel = if (uiState.transactions.isNotEmpty()) "See all" else null,
                onAction = onNavigateToActivity,
                modifier = Modifier.entrance(4)
            )
        }

        if (recent.isEmpty()) {
            item {
                EmptyRecent(onAdd = onQuickExpense)
            }
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

    if (showConfirmClearDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmClearDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Clear everything?", fontWeight = FontWeight.Bold) },
            text = { Text("All transactions, budgets, goals and bills will be deleted. This can't be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmClearDialog = false
                        onClearAllData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                    modifier = Modifier.testTag("confirm_remove_dummy_dialog_btn")
                ) { Text("Delete all") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmClearDialog = false },
                    modifier = Modifier.testTag("cancel_remove_dummy_dialog_btn")
                ) { Text("Cancel") }
            }
        )
    }

    selectedTransaction?.let { tx ->
        TransactionDetailSheet(
            transaction = tx,
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

@Composable
private fun HomeHeader(
    onSeedRandomData: () -> Unit,
    onClearAll: () -> Unit
) {
    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Good night"
        }
    }
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = ScreenPadding, end = 8.dp, top = 18.dp, bottom = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = greeting,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Your money",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.testTag("home_more_button")) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Load sample data") },
                    onClick = {
                        menuOpen = false
                        onSeedRandomData()
                    },
                    modifier = Modifier.testTag("dialog_seed_random_data")
                )
                DropdownMenuItem(
                    text = { Text("Clear all data", color = ExpenseRed) },
                    onClick = {
                        menuOpen = false
                        onClearAll()
                    },
                    modifier = Modifier.testTag("dialog_clear_all_data")
                )
            }
        }
    }
}

@Composable
private fun InsightLine(transactions: List<TransactionEntity>) {
    val text = remember(transactions) {
        val now = System.currentTimeMillis()
        val today = transactions.filter { it.type == TransactionType.EXPENSE.name && now - it.timestamp < 86_400_000L }
        val top = today.groupBy { it.category }.maxByOrNull { e -> e.value.sumOf { it.amount } }?.key
        when {
            today.isEmpty() -> "No spending in the last 24 hours"
            top != null -> "${formatRs(today.sumOf { it.amount })} spent today, mostly on $top"
            else -> "${formatRs(today.sumOf { it.amount })} spent today"
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = EmeraldPrimary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
            Icon(
                imageVector = Icons.Default.ReceiptLong,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = "Nothing here yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Add your first expense and it will show up here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Button(
            onClick = onAdd,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = MaterialTheme.colorScheme.onPrimary)
        ) { Text("Add expense") }
    }
}
