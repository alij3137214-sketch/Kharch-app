package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExpenseCategory
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TransferBlue
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.AffordabilityAnalysis
import com.example.ui.viewmodel.FixedCostItem
import com.example.ui.viewmodel.KharchUiState
import com.example.ui.viewmodel.KharchViewModel
import com.example.ui.viewmodel.PurchaseSimulation
import com.example.ui.viewmodel.SimVerdict
import java.util.Locale

/**
 * Full-featured Affordability Simulator component.
 * Calculates how much a user can spend based on their remaining budget for the month
 * after strictly accounting for recurring fixed costs.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AffordabilitySimulator(
    modifier: Modifier = Modifier,
    uiState: KharchUiState,
    viewModel: KharchViewModel,
    onPlanExpense: ((itemName: String, amount: Double, category: String) -> Unit)? = null,
    onCreateGoal: ((title: String, targetAmount: Double) -> Unit)? = null
) {
    val analysis = uiState.affordabilityAnalysis

    // Purchase test states
    var simItemName by remember { mutableStateOf("") }
    var simPriceText by remember { mutableStateOf("") }
    var simSliderPrice by remember { mutableFloatStateOf(0f) }
    var simCategory by remember { mutableStateOf("Shopping") }
    var simTenureMonths by remember { mutableIntStateOf(1) } // 1, 2, 3, 6

    // Dialog & expansion states
    var showFixedCostsSheet by remember { mutableStateOf(false) }
    var showBasisConfigDialog by remember { mutableStateOf(false) }
    var showAddCustomFixedCostDialog by remember { mutableStateOf(false) }
    var isPurchaseTesterExpanded by remember { mutableStateOf(true) }

    val focusManager = LocalFocusManager.current

    val currentPrice = simPriceText.toDoubleOrNull() ?: 0.0
    val activeSimulation: PurchaseSimulation? = remember(currentPrice, simItemName, simCategory, simTenureMonths, analysis) {
        if (currentPrice > 0.0) {
            uiState.simulatePurchaseAffordability(
                analysis = analysis,
                itemName = simItemName,
                price = currentPrice,
                category = simCategory,
                tenureMonths = simTenureMonths
            )
        } else null
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .testTag("affordability_simulator_card"),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .animateContentSize(animationSpec = tween(250)),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with Title & Configuration Gear
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
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "Affordability Simulator",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Affordability",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                IconButton(
                    onClick = { showBasisConfigDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Configure Budget Basis",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Hero Metric Card: Safe Discretionary Spend Available
            val isDeficit = analysis.isDeficit
            val heroColor = if (isDeficit) ExpenseRed else EmeraldPrimary
            val heroBg = if (isDeficit) ExpenseRed.copy(alpha = 0.08f) else EmeraldPrimary.copy(alpha = 0.08f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(heroBg)
                    .border(1.dp, heroColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isDeficit) "Deficit" else "Safe Spend",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(heroColor.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${analysis.daysRemainingInMonth}d left",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = heroColor,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Text(
                        text = if (isDeficit) {
                            "-Rs. ${String.format(Locale.getDefault(), "%,.0f", kotlin.math.abs(analysis.remainingDiscretionary))}"
                        } else {
                            "Rs. ${String.format(Locale.getDefault(), "%,.0f", analysis.remainingDiscretionary)}"
                        },
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = heroColor,
                        letterSpacing = (-0.5).sp
                    )

                    // Daily & Weekly Safe Allowance Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Daily:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Rs. ${String.format(Locale.getDefault(), "%,.0f", analysis.safeDailySpend)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = heroColor
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Weekly:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Rs. ${String.format(Locale.getDefault(), "%,.0f", analysis.safeWeeklySpend)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = heroColor
                                )
                            }
                        }
                    }
                }
            }

            // Visual Multi-Segment Allocation Bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Monthly Budget Allocation",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Basis: Rs. ${String.format(Locale.getDefault(), "%,.0f", analysis.budgetBasis)} (${analysis.budgetBasisType.lowercase().replaceFirstChar { it.uppercase() }})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { showBasisConfigDialog = true }
                    )
                }

                // Stacked segmented bar
                val fixedPct = analysis.fixedCostsShare
                val varPct = analysis.variableSpendShare
                val bufferPct = analysis.remainingBufferShare

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (fixedPct > 0.01f) {
                        Box(
                            modifier = Modifier
                                .weight(fixedPct)
                                .height(14.dp)
                                .background(Color(0xFF5C6BC0)) // Indigo for fixed commitments
                        )
                    }
                    if (varPct > 0.01f) {
                        Box(
                            modifier = Modifier
                                .weight(varPct)
                                .height(14.dp)
                                .background(WarningAmber) // Amber for variable expenses
                        )
                    }
                    if (bufferPct > 0.01f) {
                        Box(
                            modifier = Modifier
                                .weight(bufferPct)
                                .height(14.dp)
                                .background(EmeraldPrimary) // Emerald for remaining safe buffer
                        )
                    }
                }

                // Bar legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LegendItem(
                        color = Color(0xFF5C6BC0),
                        label = "Fixed Costs (${(fixedPct * 100).toInt()}%)"
                    )
                    LegendItem(
                        color = WarningAmber,
                        label = "Variable Spent (${(varPct * 100).toInt()}%)"
                    )
                    LegendItem(
                        color = EmeraldPrimary,
                        label = "Safe Buffer (${(bufferPct * 100).toInt()}%)"
                    )
                }
            }

            // Financial Breakdown Matrix with Action to View/Manage Fixed Costs
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
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
                            text = "Monthly Inflow / Budget Basis:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Rs. ${String.format(Locale.getDefault(), "%,.0f", analysis.budgetBasis)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showFixedCostsSheet = true }
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "(-) Recurring Fixed Costs:",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF5C6BC0),
                                fontWeight = FontWeight.SemiBold
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF5C6BC0).copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "${analysis.fixedCostsList.count { it.isEnabled }} items • Manage",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF5C6BC0)
                                )
                            }
                        }
                        Text(
                            text = "-Rs. ${String.format(Locale.getDefault(), "%,.0f", analysis.totalRecurringFixedCosts)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5C6BC0)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "(-) Variable Expenses to Date:",
                            style = MaterialTheme.typography.bodySmall,
                            color = WarningAmber,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "-Rs. ${String.format(Locale.getDefault(), "%,.0f", analysis.alreadySpentVariable)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = WarningAmber
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "(=) Net Safe Spendable Balance:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Rs. ${String.format(Locale.getDefault(), "%,.0f", analysis.remainingDiscretionary)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = heroColor
                        )
                    }
                }
            }

            // Divider before Purchase Tester
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            )

            // --- INTERACTIVE PURCHASE SIMULATOR ("Can I Afford This?") ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isPurchaseTesterExpanded = !isPurchaseTesterExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Simulate a Planned Purchase",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Icon(
                    imageVector = if (isPurchaseTesterExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = isPurchaseTesterExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Test an item or expense to see its immediate impact on your safe daily allowance:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Quick Presets Row
                    val presets = listOf(
                        "Dining Out" to 2500.0,
                        "New Shoes" to 6500.0,
                        "Weekend Trip" to 15000.0,
                        "New Phone" to 45000.0,
                        "Smartwatch" to 12000.0
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(presets) { (presetName, presetPrice) ->
                            val isSelected = simItemName == presetName
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    simItemName = presetName
                                    simPriceText = String.format(Locale.US, "%.0f", presetPrice)
                                    simSliderPrice = presetPrice.toFloat()
                                    focusManager.clearFocus()
                                },
                                label = {
                                    Text(
                                        text = "$presetName (Rs. ${String.format(Locale.getDefault(), "%,.0f", presetPrice)})",
                                        fontSize = 11.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary.copy(alpha = 0.15f),
                                    selectedLabelColor = EmeraldPrimary
                                )
                            )
                        }
                    }

                    // Item Name & Price Input Fields
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = simItemName,
                            onValueChange = { simItemName = it },
                            placeholder = { Text("e.g. Headphones") },
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            label = { Text("Item / Expense") },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            trailingIcon = {
                                if (simItemName.isNotBlank()) {
                                    IconButton(onClick = { simItemName = "" }) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        )

                        OutlinedTextField(
                            value = simPriceText,
                            onValueChange = {
                                if (it.all { ch -> ch.isDigit() || ch == '.' }) {
                                    simPriceText = it
                                    val num = it.toDoubleOrNull() ?: 0.0
                                    simSliderPrice = num.toFloat().coerceIn(0f, 100000f)
                                }
                            },
                            placeholder = { Text("Price (Rs)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            singleLine = true,
                            label = { Text("Price (Rs.)") }
                        )
                    }

                    // Interactive Slider for Price
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Adjust Price Slider",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Rs. ${String.format(Locale.getDefault(), "%,.0f", simSliderPrice.toDouble())}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }

                        Slider(
                            value = simSliderPrice,
                            onValueChange = {
                                simSliderPrice = it
                                simPriceText = String.format(Locale.US, "%.0f", it.toDouble())
                            },
                            valueRange = 0f..100000f,
                            steps = 99,
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = EmeraldPrimary,
                                activeTrackColor = EmeraldPrimary
                            )
                        )
                    }

                    // Payment Plan Tenure Selector (Upfront vs EMIs)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Payment Structure:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                1 to "100% Upfront",
                                2 to "2 Months",
                                3 to "3 Months",
                                6 to "6 Months"
                            ).forEach { (tenure, label) ->
                                val isSelected = simTenureMonths == tenure
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                        )
                                        .clickable { simTenureMonths = tenure }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    // LIVE VERDICT CARD
                    if (activeSimulation != null) {
                        val sim = activeSimulation
                        val verdictColor = when (sim.verdict) {
                            SimVerdict.COMFORTABLE -> IncomeGreen
                            SimVerdict.MODERATE -> WarningAmber
                            SimVerdict.STRETCHED -> Color(0xFFFF7043) // Coral / Deep Orange
                            SimVerdict.DEFICIT -> ExpenseRed
                        }

                        val verdictIcon = when (sim.verdict) {
                            SimVerdict.COMFORTABLE -> Icons.Default.CheckCircle
                            SimVerdict.MODERATE -> Icons.Default.Warning
                            SimVerdict.STRETCHED -> Icons.Default.Warning
                            SimVerdict.DEFICIT -> Icons.Default.Close
                        }

                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = verdictColor.copy(alpha = 0.10f)),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, verdictColor.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Title Row
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(verdictColor.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = verdictIcon,
                                            contentDescription = null,
                                            tint = verdictColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = sim.verdictTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = verdictColor
                                        )
                                        if (sim.tenureMonths > 1) {
                                            Text(
                                                text = "Split over ${sim.tenureMonths} months: Rs. ${String.format(Locale.getDefault(), "%,.0f", sim.monthlyCost)}/mo",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = sim.verdictDescription,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                // Before vs After Impact Grid
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ImpactMiniCard(
                                        modifier = Modifier.weight(1f),
                                        title = "Safe Spend Left",
                                        before = "Rs. ${String.format(Locale.getDefault(), "%,.0f", sim.remainingBefore)}",
                                        after = "Rs. ${String.format(Locale.getDefault(), "%,.0f", sim.remainingAfter)}",
                                        accentColor = verdictColor
                                    )

                                    ImpactMiniCard(
                                        modifier = Modifier.weight(1f),
                                        title = "Daily Allowance",
                                        before = "Rs. ${String.format(Locale.getDefault(), "%,.0f", sim.dailySpendBefore)}/d",
                                        after = "Rs. ${String.format(Locale.getDefault(), "%,.0f", sim.dailySpendAfter)}/d",
                                        accentColor = verdictColor
                                    )
                                }

                                // Recommendation Banner
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.HelpOutline,
                                            contentDescription = null,
                                            tint = verdictColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = sim.recommendation,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                // Direct Actions (Log as planned transaction or create goal)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (onPlanExpense != null && sim.verdict != SimVerdict.DEFICIT) {
                                        Button(
                                            onClick = {
                                                onPlanExpense(sim.itemName, sim.monthlyCost, simCategory)
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                                        ) {
                                            Text("Log as Expense", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }

                                    if (onCreateGoal != null) {
                                        OutlinedButton(
                                            onClick = {
                                                onCreateGoal(sim.itemName, sim.totalCost)
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Savings, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Save to Goal", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- 1. FIXED COSTS MANAGEMENT MODAL DIALOG ---
    if (showFixedCostsSheet) {
        AlertDialog(
            onDismissRequest = { showFixedCostsSheet = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recurring Fixed Costs",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { showFixedCostsSheet = false }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "These recurring commitments are deducted before calculating your safe spendable balance. Toggle items on/off to simulate alternative financial scenarios:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Summary header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF5C6BC0).copy(alpha = 0.12f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active Fixed Total:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5C6BC0)
                        )
                        Text(
                            text = "Rs. ${String.format(Locale.getDefault(), "%,.0f", analysis.totalRecurringFixedCosts)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF5C6BC0)
                        )
                    }

                    // List of fixed items
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        analysis.fixedCostsList.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Rs. ${String.format(Locale.getDefault(), "%,.0f", item.amount)} • ${item.category}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (item.isCustom) {
                                        IconButton(
                                            onClick = { viewModel.removeAffordabilityCustomFixedCost(item.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remove",
                                                tint = ExpenseRed,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Switch(
                                        checked = item.isEnabled,
                                        onCheckedChange = { viewModel.toggleAffordabilityFixedCost(item.id) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = EmeraldPrimary,
                                            checkedTrackColor = EmeraldPrimary.copy(alpha = 0.4f)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Button to add temporary / custom fixed commitment
                    OutlinedButton(
                        onClick = { showAddCustomFixedCostDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Fixed Cost Scenario")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showFixedCostsSheet = false },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Apply & Close")
                }
            }
        )
    }

    // --- 2. ADD CUSTOM FIXED COST SCENARIO DIALOG ---
    if (showAddCustomFixedCostDialog) {
        var customTitle by remember { mutableStateOf("") }
        var customAmountText by remember { mutableStateOf("") }
        var customCategory by remember { mutableStateOf("Bills") }

        AlertDialog(
            onDismissRequest = { showAddCustomFixedCostDialog = false },
            title = {
                Text("Add Fixed Commitment", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Simulate an additional fixed expense (e.g., loan EMI, gym membership, new rent):",
                        style = MaterialTheme.typography.bodySmall
                    )

                    OutlinedTextField(
                        value = customTitle,
                        onValueChange = { customTitle = it },
                        label = { Text("Commitment Title") },
                        placeholder = { Text("e.g. Car EMI, Gym, Tuition") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = customAmountText,
                        onValueChange = {
                            if (it.all { ch -> ch.isDigit() || ch == '.' }) customAmountText = it
                        },
                        label = { Text("Monthly Amount (Rs.)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = customAmountText.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            viewModel.addAffordabilityCustomFixedCost(customTitle, amt, customCategory)
                            showAddCustomFixedCostDialog = false
                        }
                    },
                    enabled = (customAmountText.toDoubleOrNull() ?: 0.0) > 0.0,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Add to Simulator")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomFixedCostDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- 3. CONFIGURE BUDGET BASIS DIALOG ---
    if (showBasisConfigDialog) {
        var tempBasisType by remember { mutableStateOf(analysis.budgetBasisType) }
        var customBasisInput by remember {
            mutableStateOf(String.format(Locale.US, "%.0f", analysis.budgetBasis))
        }

        AlertDialog(
            onDismissRequest = { showBasisConfigDialog = false },
            title = {
                Text("Monthly Budget Basis", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Select what baseline amount the simulator uses to calculate your discretionary spend:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Option 1: Monthly Income
                    val incomeTotal = uiState.transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
                    BasisOptionCard(
                        title = "Monthly Income",
                        subtitle = "Actual logged income",
                        amount = "Rs. ${String.format(Locale.getDefault(), "%,.0f", if (incomeTotal > 0) incomeTotal else 75000.0)}",
                        isSelected = tempBasisType == "INCOME",
                        onClick = { tempBasisType = "INCOME" }
                    )

                    // Option 2: Category Budgets Sum
                    val totalBudgets = uiState.budgets.sumOf { it.monthlyLimit }
                    BasisOptionCard(
                        title = "Category Budgets Sum",
                        subtitle = "Combined spending limits",
                        amount = "Rs. ${String.format(Locale.getDefault(), "%,.0f", if (totalBudgets > 0) totalBudgets else 60000.0)}",
                        isSelected = tempBasisType == "BUDGETS",
                        onClick = { tempBasisType = "BUDGETS" }
                    )

                    // Option 3: Custom Monthly Cap
                    BasisOptionCard(
                        title = "Custom Spending Cap",
                        subtitle = "Manually specified monthly limit",
                        amount = "Custom",
                        isSelected = tempBasisType == "CUSTOM",
                        onClick = { tempBasisType = "CUSTOM" }
                    )

                    if (tempBasisType == "CUSTOM") {
                        OutlinedTextField(
                            value = customBasisInput,
                            onValueChange = {
                                if (it.all { ch -> ch.isDigit() || ch == '.' }) customBasisInput = it
                            },
                            label = { Text("Custom Monthly Cap (Rs.)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val customAmt = if (tempBasisType == "CUSTOM") customBasisInput.toDoubleOrNull() else null
                        viewModel.setAffordabilityBasisType(tempBasisType, customAmt)
                        showBasisConfigDialog = false
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBasisConfigDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun ImpactMiniCard(
    modifier: Modifier = Modifier,
    title: String,
    before: String,
    after: String,
    accentColor: Color
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = before,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = after,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun BasisOptionCard(
    title: String,
    subtitle: String,
    amount: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                shape = RoundedCornerShape(12.dp)
            )
            .background(
                if (isSelected) EmeraldPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = amount,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
