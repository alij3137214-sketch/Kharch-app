package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WarningAmber
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.data.model.BudgetEntity
import com.example.data.model.ExpenseCategory
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.BudgetAlertStatus
import com.example.ui.viewmodel.BudgetImpactInfo
import com.example.ui.viewmodel.CategoryBudgetStatus
import java.util.Locale

/**
 * One calm line on Home when a budget is over or close to its limit.
 */
@Composable
fun HomeBudgetAlertBanner(
    modifier: Modifier = Modifier,
    exceededBudgets: List<CategoryBudgetStatus>,
    warningBudgets: List<CategoryBudgetStatus>,
    onManageBudgetsClick: () -> Unit
) {
    if (exceededBudgets.isEmpty() && warningBudgets.isEmpty()) return

    val isCritical = exceededBudgets.isNotEmpty()
    val accent = if (isCritical) ExpenseRed else WarningAmber
    val first = if (isCritical) exceededBudgets.first() else warningBudgets.first()
    val count = if (isCritical) exceededBudgets.size else warningBudgets.size

    val title = when {
        isCritical && count == 1 -> "${first.category} is over its limit"
        isCritical -> "$count kinds of spending are over their limit"
        count == 1 -> "${first.category} is close to its limit"
        else -> "$count kinds of spending are close to their limit"
    }
    val detail = if (isCritical) {
        "Rs. ${String.format(Locale.getDefault(), "%,.0f", first.overAmount)} over your limit"
    } else {
        "Rs. ${String.format(Locale.getDefault(), "%,.0f", first.remaining)} left in ${first.category}"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(accent.copy(alpha = 0.10f))
            .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(20.dp))
            .pressable(pressedScale = 0.985f, onClick = onManageBudgetsClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("home_budget_alert_banner"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = "Manage budgets",
            tint = accent,
            modifier = Modifier.size(18.dp)
        )
    }
}
/**
 * Real-time Budget Warning Card inside Add/Edit Transaction Screen.
 * Triggers interactive visual alerts as soon as an expense amount is typed.
 */
@Composable
fun ExpenseBudgetWarningCard(
    modifier: Modifier = Modifier,
    impact: BudgetImpactInfo?
) {
    if (impact == null || (!impact.willExceed && !impact.willReachWarning && !impact.isAlreadyExceeded)) {
        return
    }

    val isExceeded = impact.willExceed || impact.isAlreadyExceeded
    val accentColor = if (isExceeded) ExpenseRed else WarningAmber
    val bgColor = if (isExceeded) ExpenseRed.copy(alpha = 0.1f) else WarningAmber.copy(alpha = 0.1f)
    val borderColor = if (isExceeded) ExpenseRed.copy(alpha = 0.4f) else WarningAmber.copy(alpha = 0.4f)
    val projectedPctInt = (impact.projectedPercentage * 100).toInt()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("expense_budget_warning_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isExceeded) Icons.Default.Warning else Icons.Default.ElectricBolt,
                    contentDescription = "Alert",
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (isExceeded) "Over your limit" else "Almost at your limit",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )

                Text(
                    text = if (impact.isAlreadyExceeded) {
                        "You already used all of your ${impact.category} limit. This makes it $projectedPctInt%."
                    } else if (impact.willExceed) {
                        "This is Rs. ${String.format(Locale.getDefault(), "%,.0f", impact.excessAmount)} more than your ${impact.category} limit."
                    } else {
                        "After this, $projectedPctInt% of your ${impact.category} limit is used. Rs. ${String.format(Locale.getDefault(), "%,.0f", impact.remainingAfter)} is left."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Mini progress indicator
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { impact.projectedPercentage.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = accentColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

/**
 * Confirmation dialog when user saves an expense that exceeds the category limit.
 */
@Composable
fun BudgetExceededConfirmDialog(
    category: String,
    addedAmount: Double,
    excessAmount: Double,
    projectedTotal: Double,
    monthlyLimit: Double,
    onConfirm: () -> Unit,
    onAdjust: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onAdjust,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Alert",
                tint = ExpenseRed,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "Over your $category limit",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "This will be Rs. ${String.format(Locale.getDefault(), "%,.0f", excessAmount)} more than your limit.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Your limit", style = MaterialTheme.typography.bodySmall)
                            Text("Rs. ${String.format(Locale.getDefault(), "%,.0f", monthlyLimit)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("You will have spent", style = MaterialTheme.typography.bodySmall)
                            Text("Rs. ${String.format(Locale.getDefault(), "%,.0f", projectedTotal)}", fontWeight = FontWeight.Bold, color = ExpenseRed, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Text(
                    text = "Do you still want to save it?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save anyway")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onAdjust,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Change amount")
            }
        }
    )
}

/**
 * Dialog for setting and updating monthly category budget limits and alert warning thresholds.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryBudgetSettingDialog(
    existingBudget: BudgetEntity? = null,
    initialCategory: String? = null,
    currentSpent: Double = 0.0,
    allCategories: List<ExpenseCategory> = ExpenseCategory.entries,
    onDismiss: () -> Unit,
    onSave: (category: String, limit: Double, thresholdPercent: Int) -> Unit,
    onDelete: ((BudgetEntity) -> Unit)? = null
) {
    var selectedCategory by remember {
        mutableStateOf(existingBudget?.category ?: initialCategory ?: ExpenseCategory.FOOD.displayName)
    }
    var limitAmountText by remember {
        mutableStateOf(if (existingBudget != null) String.format(Locale.getDefault(), "%.0f", existingBudget.monthlyLimit) else "")
    }
    var alertThresholdPercent by remember {
        mutableIntStateOf(existingBudget?.alertThresholdPercent ?: 80)
    }

    val parsedLimit = limitAmountText.toDoubleOrNull() ?: 0.0
    val thresholdAmount = parsedLimit * (alertThresholdPercent / 100.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = EmeraldPrimary
                )
                Text(
                    text = if (existingBudget != null) "Change limit" else "New limit",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Category Picker
                Text(
                    text = "What kind of spending?",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    allCategories.forEach { cat ->
                        val isSelected = selectedCategory.equals(cat.displayName, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) cat.color.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) cat.color else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedCategory = cat.displayName }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = cat.icon(),
                                contentDescription = null,
                                tint = if (isSelected) cat.color else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = cat.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) cat.color else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Monthly Limit Input
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Limit for one month",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = limitAmountText,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() || it == '.' }) {
                                limitAmountText = input
                            }
                        },
                        prefix = {
                            Text(
                                text = "Rs. ",
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        },
                        placeholder = { Text("e.g. 15,000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("budget_limit_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    )

                    // Quick Preset Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(3000, 5000, 10000, 15000, 25000).forEach { preset ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                    .clickable { limitAmountText = preset.toString() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Rs. ${String.format(Locale.getDefault(), "%,d", preset)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Current Spending Context Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Spent this month:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Rs. ${String.format(Locale.getDefault(), "%,.0f", currentSpent)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (parsedLimit > 0) {
                            val currentPct = ((currentSpent / parsedLimit) * 100).toInt()
                            val willBeExceeded = currentSpent >= parsedLimit
                            val willBeWarning = currentPct >= alertThresholdPercent && !willBeExceeded

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Used so far:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (willBeExceeded) "⚠️ Currently Exceeded ($currentPct%)"
                                    else if (willBeWarning) "⚡ Near Warning Limit ($currentPct%)"
                                    else "🟢 On Track ($currentPct%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (willBeExceeded) ExpenseRed else if (willBeWarning) WarningAmber else EmeraldPrimary
                                )
                            }
                        }
                    }
                }

                // Visual Alert Threshold Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Warn me at",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(WarningAmber.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$alertThresholdPercent%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = WarningAmber
                            )
                        }
                    }

                    Text(
                        text = if (parsedLimit > 0) {
                            "Triggers visual alerts at Rs. ${String.format(Locale.getDefault(), "%,.0f", thresholdAmount)} (before reaching the Rs. ${String.format(Locale.getDefault(), "%,.0f", parsedLimit)} cap)."
                        } else {
                            "Choose spending percentage that triggers visual warnings."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )

                    // Preset threshold chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(70, 80, 90, 100).forEach { pct ->
                            val isSelected = alertThresholdPercent == pct
                            FilterChip(
                                selected = isSelected,
                                onClick = { alertThresholdPercent = pct },
                                label = {
                                    Text(
                                        text = if (pct == 100) "100% (Strict)" else "$pct%",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = WarningAmber.copy(alpha = 0.2f),
                                    selectedLabelColor = WarningAmber
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (parsedLimit > 0) {
                        onSave(selectedCategory, parsedLimit, alertThresholdPercent)
                    }
                },
                enabled = parsedLimit > 0,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (existingBudget != null && onDelete != null) {
                    TextButton(
                        onClick = { onDelete(existingBudget) },
                        colors = ButtonDefaults.textButtonColors(contentColor = ExpenseRed)
                    ) {
                        Text("Delete")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
