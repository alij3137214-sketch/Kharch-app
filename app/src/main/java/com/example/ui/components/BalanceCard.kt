package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BillReminderEntity
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen

/**
 * Home hero. One big number, two quiet supporting numbers, one slim budget bar.
 */
@Composable
fun BalanceCard(
    modifier: Modifier = Modifier,
    totalBalance: Double,
    totalIncome: Double,
    totalExpense: Double,
    isBalanceHidden: Boolean,
    availablePercentage: Int,
    showBudget: Boolean = true,
    upcomingBill: BillReminderEntity? = null,
    onToggleVisibility: () -> Unit,
    onUpcomingBillClick: (BillReminderEntity) -> Unit = {}
) {
    val shape = RoundedCornerShape(30.dp)
    val budgetProgress by animateFloatAsState(
        targetValue = (availablePercentage / 100f).coerceIn(0f, 1f),
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "budget_progress"
    )
    val budgetColor = when {
        availablePercentage > 40 -> EmeraldPrimary
        availablePercentage > 15 -> com.example.ui.theme.WarningAmber
        else -> ExpenseRed
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), shape)
            .padding(horizontal = 22.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Money you have",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(
                onClick = onToggleVisibility,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("toggle_balance_button")
            ) {
                Icon(
                    imageVector = if (isBalanceHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (isBalanceHidden) "Show balance" else "Hide balance",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (isBalanceHidden) {
            Text(
                text = "Rs. ••••••",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "Rs.",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 7.dp, end = 6.dp)
                )
                AnimatedAmount(
                    value = totalBalance,
                    style = MaterialTheme.typography.displayLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            StatColumn(
                label = "Money in",
                value = if (isBalanceHidden) "••••" else "+" + formatRs(totalIncome),
                dot = IncomeGreen,
                modifier = Modifier.weight(1f)
            )
            StatColumn(
                label = "Money out",
                value = if (isBalanceHidden) "••••" else "−" + formatRs(totalExpense),
                dot = ExpenseRed,
                modifier = Modifier.weight(1f)
            )
        }

        if (showBudget) {
        Spacer(modifier = Modifier.height(22.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Monthly limit",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$availablePercentage% left",
                style = MaterialTheme.typography.labelMedium,
                color = budgetColor
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(budgetProgress)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(budgetColor)
            )
        }
        }

        if (upcomingBill != null) {
            Spacer(modifier = Modifier.height(18.dp))
            val daysRemaining = ((upcomingBill.dueDate - System.currentTimeMillis()) / 86400000L).coerceAtLeast(0)
            val dueText = when (daysRemaining) {
                0L -> "Due today"
                1L -> "Due tomorrow"
                else -> "Due in $daysRemaining days"
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    .pressable(pressedScale = 0.98f) { onUpcomingBillClick(upcomingBill) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ElectricBolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = upcomingBill.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$dueText · ${formatRs(upcomingBill.amount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "View bill",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun StatColumn(
    label: String,
    value: String,
    dot: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dot)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
