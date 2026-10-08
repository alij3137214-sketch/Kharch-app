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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BillReminderEntity
import com.example.data.model.TransactionEntity
import com.example.ui.components.AccountsCarousel
import com.example.ui.components.BalanceCard
import com.example.ui.components.HomeBudgetAlertBanner
import com.example.ui.components.QuickActions
import com.example.ui.components.SubscriptionTrackerCard
import com.example.ui.components.TopHeader
import com.example.ui.components.TransactionDetailSheet
import com.example.ui.components.TransactionItem
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.viewmodel.KharchUiState
import java.util.Locale

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    uiState: KharchUiState,
    onToggleBalanceVisibility: () -> Unit,
    onQuickExpense: () -> Unit,
    onQuickIncome: () -> Unit,
    onQuickTransfer: () -> Unit,
    onQuickScanReceipt: () -> Unit,
    onSearchQueryChange: (String) -> Unit = {},
    onFilterTypeChange: (String) -> Unit = {},
    onBillClick: (BillReminderEntity) -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onNavigateToActivity: () -> Unit = {},
    onNavigateToPlan: () -> Unit = {},
    onOpenVaultEntrance: () -> Unit = {},
    onSeedRandomData: () -> Unit = {},
    onClearAllData: () -> Unit = {}
) {
    var selectedTransactionForDetail by remember { mutableStateOf<TransactionEntity?>(null) }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var showConfirmClearDialog by remember { mutableStateOf(false) }

    val filterOptions = listOf(
        "ALL" to "All",
        "EXPENSE" to "Expenses",
        "INCOME" to "Income",
        "TRANSFER" to "Transfers"
    )

    // Calculate smart spending insight
    val insightText = remember(uiState.transactions) {
        val todayExpense = uiState.transactions.filter {
            it.type == "EXPENSE" &&
            (System.currentTimeMillis() - it.timestamp) < 86400000L
        }
        val count = todayExpense.size
        val sum = todayExpense.sumOf { it.amount }
        val topCat = todayExpense.groupBy { it.category }.maxByOrNull { it.value.sumOf { tx -> tx.amount } }?.key

        when {
            count == 0 -> "No expenses today"
            topCat != null -> "Today: Rs. ${String.format(Locale.getDefault(), "%,.0f", sum)} · Top: $topCat"
            else -> "Today: Rs. ${String.format(Locale.getDefault(), "%,.0f", sum)}"
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        // Top Header
        item {
            TopHeader(
                onSearchClick = { isSearchExpanded = !isSearchExpanded },
                onOpenVaultEntrance = onOpenVaultEntrance,
                onSeedRandomData = onSeedRandomData,
                onClearAllData = { showConfirmClearDialog = true }
            )
        }

        // Balance Card
        item {
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

        // Budget Alerts & Warnings Banner (triggers when thresholds exceeded or near limit)
        item {
            HomeBudgetAlertBanner(
                exceededBudgets = uiState.exceededBudgets,
                warningBudgets = uiState.warningBudgets,
                onManageBudgetsClick = onNavigateToPlan
            )
        }

        // Quick Actions
        item {
            Spacer(modifier = Modifier.height(6.dp))
            QuickActions(
                onExpenseClick = onQuickExpense,
                onIncomeClick = onQuickIncome,
                onTransferClick = onQuickTransfer,
                onScanReceiptClick = onQuickScanReceipt
            )
        }

        // Multiple Accounts & Wallets Carousel (BudgetIt signature)
        item {
            Spacer(modifier = Modifier.height(10.dp))
            AccountsCarousel(
                transactions = uiState.transactions,
                totalBalance = uiState.totalBalance,
                isBalanceHidden = uiState.isBalanceHidden,
                onAccountClick = { _ -> onNavigateToActivity() }
            )
        }

        // Subscriptions & Recurring Bills Tracker Card (BudgetIt signature)
        item {
            Spacer(modifier = Modifier.height(10.dp))
            SubscriptionTrackerCard(
                bills = uiState.bills,
                onViewAllClick = onNavigateToPlan
            )
        }

        // Spending Insight Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Insight",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Spending Insight",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = insightText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Safe Discretionary Spend Snapshot (Affordability Simulator Quick Access)
        item {
            val aff = uiState.affordabilityAnalysis
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clickable { onNavigateToPlan() }
                    .testTag("home_affordability_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = "Affordability",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Safe Spend:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (aff.isDeficit) "-Rs. ${String.format(Locale.getDefault(), "%,.0f", kotlin.math.abs(aff.remainingDiscretionary))}"
                                    else "Rs. ${String.format(Locale.getDefault(), "%,.0f", aff.remainingDiscretionary)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (aff.isDeficit) ExpenseRed else EmeraldPrimary
                                )
                            }
                            Text(
                                text = "Rs. ${String.format(Locale.getDefault(), "%,.0f", aff.safeDailySpend)}/day · Fixed: Rs. ${String.format(Locale.getDefault(), "%,.0f", aff.totalRecurringFixedCosts)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Simulate Purchases",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    // Confirmation Dialog to Remove Dummy Data
    if (showConfirmClearDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmClearDialog = false },
            title = {
                Text(
                    text = "Clear All Data?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Reset all records to start fresh?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmClearDialog = false
                        onClearAllData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                    modifier = Modifier.testTag("confirm_remove_dummy_dialog_btn")
                ) {
                    Text("Clear All Data")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmClearDialog = false },
                    modifier = Modifier.testTag("cancel_remove_dummy_dialog_btn")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Detail Bottom Sheet
    if (selectedTransactionForDetail != null) {
        TransactionDetailSheet(
            transaction = selectedTransactionForDetail!!,
            onDismiss = { selectedTransactionForDetail = null },
            onEdit = { tx ->
                selectedTransactionForDetail = null
                onEditTransaction(tx)
            },
            onDelete = { tx ->
                selectedTransactionForDetail = null
                onDeleteTransaction(tx)
            }
        )
    }
}
