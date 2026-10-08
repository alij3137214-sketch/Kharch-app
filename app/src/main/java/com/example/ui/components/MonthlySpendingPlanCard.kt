package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.BudgetAlertStatus
import com.example.ui.viewmodel.KharchUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

/**
 * Minimalist & Modern Monthly Spending Limit Card.
 * Allows users to set a monthly spending limit and track in real-time how much
 * of their budget remains throughout the current month.
 */
@Composable
fun MonthlySpendingPlanCard(
    modifier: Modifier = Modifier,
    uiState: KharchUiState,
    onSetMonthlyLimit: (limit: Double, alertThreshold: Int) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }

    val monthName = remember {
        val cal = Calendar.getInstance()
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }

    val limit = uiState.effectiveMonthlySpendingLimit
    val spent = uiState.currentMonthTotalExpense
    val remaining = uiState.currentMonthBudgetRemaining
    val isExceeded = uiState.isCurrentMonthBudgetExceeded
    val spentRatio = uiState.currentMonthBudgetSpentRatio.coerceIn(0f, 1f)
    val spentPercentage = uiState.currentMonthBudgetSpentPercentage
    val daysLeft = uiState.daysLeftInCurrentMonth
    val dailyAllowance = uiState.dailyBudgetRemainingAllowance
    val alertStatus = uiState.currentMonthBudgetAlertStatus

    val animatedProgress by animateFloatAsState(
        targetValue = spentRatio,
        animationSpec = tween(durationMillis = 600),
        label = "budget_progress"
    )

    val statusColor by animateColorAsState(
        targetValue = when (alertStatus) {
            BudgetAlertStatus.EXCEEDED -> ExpenseRed
            BudgetAlertStatus.WARNING -> WarningAmber
            BudgetAlertStatus.SAFE -> EmeraldPrimary
        },
        label = "status_color"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .testTag("monthly_spending_limit_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            when (alertStatus) {
                BudgetAlertStatus.EXCEEDED -> ExpenseRed.copy(alpha = 0.45f)
                BudgetAlertStatus.WARNING -> WarningAmber.copy(alpha = 0.4f)
                BudgetAlertStatus.SAFE -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Header: Month & Edit Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (alertStatus) {
                                BudgetAlertStatus.EXCEEDED -> Icons.Default.ErrorOutline
                                BudgetAlertStatus.WARNING -> Icons.Default.WarningAmber
                                BudgetAlertStatus.SAFE -> Icons.Default.CalendarMonth
                            },
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Monthly Spending Plan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = monthName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Action to Set / Edit Monthly Limit
                OutlinedButton(
                    onClick = { showEditDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("set_monthly_limit_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Set Limit",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (uiState.isMonthlyLimitCustomSet) "Edit Limit" else "Set Limit",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            // Hero Metric: Budget Remaining Throughout Current Month
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (isExceeded) "OVER MONTHLY LIMIT" else "BUDGET REMAINING THIS MONTH",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    ),
                    color = if (isExceeded) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Big Hero Amount
                    Text(
                        text = if (isExceeded) {
                            "-Rs. ${String.format(Locale.getDefault(), "%,.0f", abs(remaining))}"
                        } else {
                            "Rs. ${String.format(Locale.getDefault(), "%,.0f", remaining)}"
                        },
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isExceeded) ExpenseRed else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("monthly_limit_remaining_value")
                    )

                    // Status Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusColor.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Text(
                                text = when (alertStatus) {
                                    BudgetAlertStatus.EXCEEDED -> "Over Limit"
                                    BudgetAlertStatus.WARNING -> "Near Limit ($spentPercentage%)"
                                    BudgetAlertStatus.SAFE -> "On Track"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = statusColor
                            )
                        }
                    }
                }
            }

            // Clean Animated Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = statusColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${spentPercentage}% spent",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isExceeded) "Exceeded by ${spentPercentage - 100}%" else "${100 - spentPercentage}% remaining",
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Key Metrics Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Metric 1: Monthly Limit
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Monthly Limit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Rs. ${String.format(Locale.getDefault(), "%,.0f", limit)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("monthly_limit_total_value")
                    )
                }

                // Metric 2: Spent So Far
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Spent So Far",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Rs. ${String.format(Locale.getDefault(), "%,.0f", spent)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("monthly_limit_spent_value")
                    )
                }

                // Metric 3: Daily Safe Allowance
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Daily Safe",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isExceeded) "Rs. 0/day" else "Rs. ${String.format(Locale.getDefault(), "%,.0f", dailyAllowance)}/d",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isExceeded) ExpenseRed else EmeraldPrimary
                    )
                }

                // Metric 4: Days Remaining
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Days Left",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${daysLeft}d",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    // Modal Dialog to Set / Edit Monthly Spending Limit
    if (showEditDialog) {
        MonthlySpendingLimitDialog(
            currentLimit = limit,
            currentSpent = spent,
            daysLeft = daysLeft,
            currentThreshold = uiState.overallMonthlyBudget?.alertThresholdPercent ?: 80,
            onDismiss = { showEditDialog = false },
            onSave = { newLimit, threshold ->
                onSetMonthlyLimit(newLimit, threshold)
                showEditDialog = false
            }
        )
    }
}

/**
 * Clean & modern dialog to set or update the monthly spending limit.
 */
@Composable
fun MonthlySpendingLimitDialog(
    currentLimit: Double,
    currentSpent: Double,
    daysLeft: Int,
    currentThreshold: Int,
    onDismiss: () -> Unit,
    onSave: (limit: Double, threshold: Int) -> Unit
) {
    var limitInput by remember {
        mutableStateOf(if (currentLimit > 0) String.format(Locale.US, "%.0f", currentLimit) else "50000")
    }
    var thresholdPercent by remember { mutableIntStateOf(currentThreshold) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val presetAmounts = listOf(30000.0, 50000.0, 75000.0, 100000.0, 150000.0)

    val targetLimit = limitInput.toDoubleOrNull() ?: 0.0
    val projectedRemaining = targetLimit - currentSpent
    val projectedDaily = if (projectedRemaining > 0 && daysLeft > 0) projectedRemaining / daysLeft else 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("monthly_limit_dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Monthly Spending Limit",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Set your total expenditure cap for the current month. We'll track your daily pace and remaining balance.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Quick Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetAmounts.take(4).forEach { preset ->
                        val isSelected = targetLimit == preset
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                limitInput = String.format(Locale.US, "%.0f", preset)
                                errorMessage = null
                            },
                            label = {
                                Text(
                                    text = "Rs. ${(preset / 1000).toInt()}k",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                // Amount Text Field
                OutlinedTextField(
                    value = limitInput,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() || it == '.' }) {
                            limitInput = input
                            errorMessage = null
                        }
                    },
                    label = { Text("Spending Limit (Rs.)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it, color = ExpenseRed) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("monthly_limit_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Alert Threshold Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Warning Alert Threshold",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$thresholdPercent%",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Slider(
                        value = thresholdPercent.toFloat(),
                        onValueChange = { thresholdPercent = it.toInt() },
                        valueRange = 50f..95f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // Live Projection Card
                if (targetLimit > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Projected Remaining:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (projectedRemaining >= 0) {
                                        "Rs. ${String.format(Locale.getDefault(), "%,.0f", projectedRemaining)}"
                                    } else {
                                        "-Rs. ${String.format(Locale.getDefault(), "%,.0f", abs(projectedRemaining))} (Deficit)"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (projectedRemaining >= 0) EmeraldPrimary else ExpenseRed
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Safe Daily Allowance:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Rs. ${String.format(Locale.getDefault(), "%,.0f", projectedDaily)}/day",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (projectedRemaining >= 0) EmeraldPrimary else ExpenseRed
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = limitInput.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        errorMessage = "Please enter a valid amount greater than 0"
                        return@Button
                    }
                    onSave(amount, thresholdPercent)
                },
                modifier = Modifier.testTag("monthly_limit_save_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Save Limit")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("monthly_limit_cancel_btn")
            ) {
                Text("Cancel")
            }
        }
    )
}
