package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionEntity
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.ExportCsvDialog
import com.example.ui.components.ShareVisualAnalyticsDialog
import com.example.ui.components.SpendingChart
import com.example.ui.components.TransactionItem
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TransferBlue
import com.example.ui.viewmodel.CategoryShare
import com.example.ui.viewmodel.KharchUiState
import com.example.ui.viewmodel.KharchViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ReportsScreen(
    modifier: Modifier = Modifier,
    uiState: KharchUiState,
    viewModel: KharchViewModel,
    onTransactionClick: (TransactionEntity) -> Unit = {}
) {
    val modes = listOf("Daily", "Weekly", "Monthly", "Yearly")
    val currentMode = uiState.selectedReportPeriod
    val offset = uiState.reportDateOffset
    var showExportCsvDialog by remember { mutableStateOf(false) }
    var showShareVisualDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("reports_screen"),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // Title Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Reports",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Cash flow analysis & trends",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { showShareVisualDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("reports_share_visual_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Analytics",
                        modifier = Modifier.size(15.dp),
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Share",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }

        // Mode Selector Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                modes.forEach { mode ->
                    val isSelected = currentMode == mode
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setReportPeriod(mode) },
                        label = { Text(mode, style = MaterialTheme.typography.labelMedium) },
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

        // Period Date Navigator (Previous / Next)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.changeReportOffset(-1) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous"
                        )
                    }

                    Text(
                        text = when (currentMode) {
                            "Daily" -> if (offset == 0) "Today" else viewModel.getDailyReport(offset).dateFormatted
                            "Weekly" -> if (offset == 0) "This Week" else viewModel.getWeeklyReport(offset).weekLabel
                            "Monthly" -> viewModel.getMonthlyReport(offset).monthLabel
                            else -> viewModel.getYearlyReport(offset).yearLabel
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(
                        onClick = { viewModel.changeReportOffset(1) },
                        enabled = offset < 0
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next"
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Dynamic Mode Content
        when (currentMode) {
            "Daily" -> {
                val daily = viewModel.getDailyReport(offset)

                item {
                    // Daily Metrics Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Daily Balance Overview",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                ReportMetric("Starting Balance", "Rs. ${String.format(Locale.getDefault(), "%,.0f", daily.startingBalance)}")
                                ReportMetric("Remaining", "Rs. ${String.format(Locale.getDefault(), "%,.0f", daily.remaining)}", EmeraldPrimary)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                ReportMetric("Income", "+Rs. ${String.format(Locale.getDefault(), "%,.0f", daily.income)}", IncomeGreen)
                                ReportMetric("Expenses", "-Rs. ${String.format(Locale.getDefault(), "%,.0f", daily.expenses)}", ExpenseRed)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                ReportMetric("Transactions", "${daily.transactionCount}")
                                ReportMetric("Top Category", daily.topCategory ?: "None")
                            }

                            if (daily.largestExpense != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ExpenseRed.copy(alpha = 0.08f))
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.CreditCard, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(18.dp))
                                        Text(
                                            text = "Largest Expense: ${daily.largestExpense.title} (Rs. ${String.format(Locale.getDefault(), "%,.0f", daily.largestExpense.amount)})",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = ExpenseRed
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Transaction Timeline",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (daily.timeline.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No transactions on this date.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(daily.timeline, key = { it.id }) { tx ->
                        val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = timeFmt.format(Date(tx.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(68.dp)
                            )
                            Box(modifier = Modifier.weight(1f)) {
                                TransactionItem(transaction = tx, onClick = { onTransactionClick(tx) })
                            }
                        }
                    }
                }
            }

            "Weekly" -> {
                val weekly = viewModel.getWeeklyReport(offset)

                item {
                    // Weekly Metrics
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Weekly Spending Performance",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                ReportMetric("Total Spent", "Rs. ${String.format(Locale.getDefault(), "%,.0f", weekly.totalSpent)}", ExpenseRed)
                                ReportMetric("Average Daily", "Rs. ${String.format(Locale.getDefault(), "%,.0f", weekly.averageDaily)}")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Daily breakdown chart
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        SpendingChart(data = weekly.dailyBreakdown, periodName = "Week")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Category distribution
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        CategoryDonutChart(
                            categories = weekly.topCategories,
                            selectedCategory = null,
                            onSelectCategory = {}
                        )
                    }
                }
            }

            "Monthly" -> {
                val monthly = viewModel.getMonthlyReport(offset)

                // Monthly CSV Export Action Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Export ${monthly.monthLabel} CSV",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Export expense data for spreadsheets & accounting",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = { showExportCsvDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("export_monthly_csv_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "Export CSV",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Export",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Monthly Savings & Budget Adherence",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                ReportMetric("Total Income", "+Rs. ${String.format(Locale.getDefault(), "%,.0f", monthly.totalIncome)}", IncomeGreen)
                                ReportMetric("Total Expense", "-Rs. ${String.format(Locale.getDefault(), "%,.0f", monthly.totalExpense)}", ExpenseRed)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                ReportMetric("Net Savings", "Rs. ${String.format(Locale.getDefault(), "%,.0f", monthly.netSavings)}", EmeraldPrimary)
                                ReportMetric("Savings Rate", "${monthly.savingsRate.toInt()}%")
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Budget Compliance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${monthly.budgetAdherencePercent.toInt()}% within plan", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                }
                                LinearProgressIndicator(
                                    progress = { (monthly.budgetAdherencePercent / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = EmeraldPrimary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        CategoryDonutChart(
                            categories = monthly.topCategories,
                            selectedCategory = null,
                            onSelectCategory = {}
                        )
                    }
                }
            }

            "Yearly" -> {
                val yearly = viewModel.getYearlyReport(offset)

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Annual Financial Summary (${yearly.yearLabel})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                ReportMetric("Annual Income", "Rs. ${String.format(Locale.getDefault(), "%,.0f", yearly.totalIncome)}", IncomeGreen)
                                ReportMetric("Annual Spending", "Rs. ${String.format(Locale.getDefault(), "%,.0f", yearly.totalExpense)}", ExpenseRed)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                ReportMetric("Net Saved", "Rs. ${String.format(Locale.getDefault(), "%,.0f", yearly.netSavings)}", EmeraldPrimary)
                                ReportMetric("Highest Spend Month", "${yearly.highestSpendingMonth} (Rs. ${String.format(Locale.getDefault(), "%,.0f", yearly.highestMonthExpense)})")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        SpendingChart(data = yearly.monthlyBreakdown, periodName = "Year")
                    }
                }
            }
        }
    }

    if (showExportCsvDialog) {
        val cal = Calendar.getInstance().apply { add(Calendar.MONTH, offset) }
        val monthLabel = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
        val monthlyExpenses = remember(offset, uiState.transactions) {
            viewModel.getMonthlyExpenses(offset)
        }
        ExportCsvDialog(
            monthLabel = monthLabel,
            calendar = cal,
            expenses = monthlyExpenses,
            onDismiss = { showExportCsvDialog = false }
        )
    }

    if (showShareVisualDialog) {
        val totalExp = uiState.totalExpense
        val totalInc = uiState.totalIncome
        val (data, shares) = viewModel.getSpendingPeriodData("Month")
        ShareVisualAnalyticsDialog(
            periodLabel = currentMode,
            totalIncome = totalInc,
            totalExpense = totalExp,
            netBalance = totalInc - totalExp,
            topCategories = shares,
            avgDailyExpense = if (data.isNotEmpty()) totalExp / data.size else 0.0,
            txCount = uiState.transactions.size,
            onDismiss = { showShareVisualDialog = false }
        )
    }
}

@Composable
private fun ReportMetric(
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (valueColor != Color.Unspecified) valueColor else MaterialTheme.colorScheme.onSurface
        )
    }
}
