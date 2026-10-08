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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionEntity
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.ExportCsvDialog
import com.example.ui.components.RechartsCategoryPieChart
import com.example.ui.components.RechartsDailySpendingBarChart
import com.example.ui.components.ShareVisualAnalyticsDialog
import com.example.ui.components.TransactionDetailSheet
import com.example.ui.components.TransactionItem
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.BudgetAlertStatus
import com.example.ui.viewmodel.KharchUiState
import com.example.ui.viewmodel.KharchViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun SpendingScreen(
    modifier: Modifier = Modifier,
    uiState: KharchUiState,
    viewModel: KharchViewModel,
    onEditTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onNavigateToReports: () -> Unit = {}
) {
    val periods = listOf("Today", "Week", "Month", "Year")
    val currentPeriod = uiState.selectedSpendingPeriod

    val (chartData, categoryShares) = remember(currentPeriod, uiState.transactions) {
        viewModel.getSpendingPeriodData(currentPeriod)
    }

    // Metric Calculations
    val totalSpending = remember(chartData) { chartData.sumOf { it.amount } }
    val avgSpending = remember(chartData, totalSpending) {
        if (chartData.isNotEmpty()) totalSpending / chartData.size else 0.0
    }

    val periodExpenses = remember(currentPeriod, uiState.transactions) {
        val now = System.currentTimeMillis()
        uiState.transactions.filter {
            if (it.type != "EXPENSE") return@filter false
            when (currentPeriod) {
                "Today" -> (now - it.timestamp) < 86400000L
                "Week" -> (now - it.timestamp) < (7 * 86400000L)
                "Month" -> (now - it.timestamp) < (30 * 86400000L)
                else -> (now - it.timestamp) < (365 * 86400000L)
            }
        }
    }

    var selectedTransactionForDetail by remember { mutableStateOf<TransactionEntity?>(null) }
    var showExportCsvDialog by remember { mutableStateOf(false) }
    var showShareVisualDialog by remember { mutableStateOf(false) }

    // Filter transactions list by period AND selectedCategoryFilter
    val filteredList = remember(periodExpenses, uiState.selectedCategoryFilter) {
        if (uiState.selectedCategoryFilter == null) {
            periodExpenses
        } else {
            periodExpenses.filter { it.category.equals(uiState.selectedCategoryFilter, ignoreCase = true) }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("understand_screen"),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // Header: title on the left, three quiet icon actions on the right
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, top = 18.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Insights",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Where your money goes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    IconButton(onClick = { showShareVisualDialog = true }, modifier = Modifier.testTag("analytics_share_visual_btn")) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onNavigateToReports, modifier = Modifier.testTag("understand_to_reports_btn")) {
                        Icon(Icons.Default.BarChart, contentDescription = "Reports", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { showExportCsvDialog = true }, modifier = Modifier.testTag("understand_export_csv_btn")) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export CSV", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Period Selector Tabs
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                periods.forEach { period ->
                    val isSelected = currentPeriod == period
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setSpendingPeriod(period) },
                        label = { Text(period, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Clean Minimalist Period Spending Summary
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CreditCard,
                    label = "$currentPeriod Total",
                    value = "Rs. ${String.format(Locale.getDefault(), "%,.0f", totalSpending)}"
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Analytics,
                    label = "Daily Avg",
                    value = "Rs. ${String.format(Locale.getDefault(), "%,.0f", avgSpending)}"
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 30-Day Daily Spending Bar Chart (Minimalist & Clean)
        item {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                val thirtyDayTrend = remember(uiState.transactions) {
                    viewModel.getThirtyDaySpendingTrend()
                }
                RechartsDailySpendingBarChart(trend = thirtyDayTrend)
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Category Breakdown Pie Chart
        item {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                val effectiveCategoryShares = remember(currentPeriod, categoryShares, uiState.currentMonthCategoryShares) {
                    if (currentPeriod == "Month" && uiState.currentMonthCategoryShares.isNotEmpty()) {
                        uiState.currentMonthCategoryShares
                    } else {
                        categoryShares
                    }
                }
                RechartsCategoryPieChart(
                    categories = effectiveCategoryShares,
                    selectedCategory = uiState.selectedCategoryFilter,
                    onCategorySelected = { cat -> viewModel.selectCategoryFilter(cat) }
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Selected Category Budget Status Card (if budget is set)
        if (uiState.selectedCategoryFilter != null) {
            val selectedCatBudget = uiState.categoryBudgetStatuses.find {
                it.category.equals(uiState.selectedCategoryFilter, ignoreCase = true)
            }
            if (selectedCatBudget != null) {
                item {
                    val barColor = when (selectedCatBudget.alertStatus) {
                        BudgetAlertStatus.EXCEEDED -> ExpenseRed
                        BudgetAlertStatus.WARNING -> WarningAmber
                        BudgetAlertStatus.SAFE -> EmeraldPrimary
                    }
                    val pctInt = (selectedCatBudget.spentPercentage * 100).toInt()

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when (selectedCatBudget.alertStatus) {
                                BudgetAlertStatus.EXCEEDED -> ExpenseRed.copy(alpha = 0.08f)
                                BudgetAlertStatus.WARNING -> WarningAmber.copy(alpha = 0.08f)
                                BudgetAlertStatus.SAFE -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${selectedCatBudget.category} Monthly Budget",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = when (selectedCatBudget.alertStatus) {
                                        BudgetAlertStatus.EXCEEDED -> "🚨 Exceeded ($pctInt%)"
                                        BudgetAlertStatus.WARNING -> "⚡ Near Limit ($pctInt%)"
                                        BudgetAlertStatus.SAFE -> "🟢 On Track ($pctInt%)"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = barColor
                                )
                            }

                            LinearProgressIndicator(
                                progress = { selectedCatBudget.spentPercentage.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = barColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Spent: Rs. ${String.format(Locale.getDefault(), "%,.0f", selectedCatBudget.currentSpent)} / ${String.format(Locale.getDefault(), "%,.0f", selectedCatBudget.monthlyLimit)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = if (selectedCatBudget.alertStatus == BudgetAlertStatus.EXCEEDED) {
                                        "+Rs. ${String.format(Locale.getDefault(), "%,.0f", selectedCatBudget.overAmount)} over"
                                    } else {
                                        "Rs. ${String.format(Locale.getDefault(), "%,.0f", selectedCatBudget.remaining)} left"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = barColor,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        // Filtered Transactions Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (uiState.selectedCategoryFilter != null) {
                        "${uiState.selectedCategoryFilter} Expenses (${filteredList.size})"
                    } else {
                        "Period Transactions (${filteredList.size})"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (uiState.selectedCategoryFilter != null) {
                    Text(
                        text = "Show all",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    )
                }
            }
        }

        if (filteredList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No expenses recorded in this period",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(filteredList, key = { it.id }) { tx ->
                TransactionItem(
                    transaction = tx,
                    onClick = { selectedTransactionForDetail = tx }
                )
            }
        }
    }

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

    if (showExportCsvDialog) {
        val cal = Calendar.getInstance()
        val monthLabel = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
        val monthlyExpenses = remember(uiState.transactions) {
            viewModel.getMonthlyExpenses(0)
        }
        ExportCsvDialog(
            monthLabel = monthLabel,
            calendar = cal,
            expenses = monthlyExpenses,
            onDismiss = { showExportCsvDialog = false }
        )
    }

    if (showShareVisualDialog) {
        ShareVisualAnalyticsDialog(
            periodLabel = currentPeriod,
            totalIncome = uiState.totalIncome,
            totalExpense = totalSpending,
            netBalance = uiState.totalIncome - totalSpending,
            topCategories = categoryShares,
            avgDailyExpense = avgSpending,
            txCount = periodExpenses.size,
            onDismiss = { showShareVisualDialog = false }
        )
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * 'Understand' Screen: Displays category breakdowns, Recharts pie chart, and spending analytics.
 */
@Composable
fun UnderstandScreen(
    modifier: Modifier = Modifier,
    uiState: KharchUiState,
    viewModel: KharchViewModel,
    onEditTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit
) {
    SpendingScreen(
        modifier = modifier,
        uiState = uiState,
        viewModel = viewModel,
        onEditTransaction = onEditTransaction,
        onDeleteTransaction = onDeleteTransaction
    )
}

