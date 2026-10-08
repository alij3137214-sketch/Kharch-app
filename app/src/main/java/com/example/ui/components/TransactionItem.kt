package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ExpenseCategory
import com.example.data.model.IncomeSource
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TransferBlue
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun TransactionItem(
    modifier: Modifier = Modifier,
    transaction: TransactionEntity,
    onClick: () -> Unit
) {
    val categoryColor = when (transaction.type) {
        TransactionType.INCOME.name -> IncomeGreen
        TransactionType.TRANSFER.name -> TransferBlue
        else -> ExpenseCategory.fromString(transaction.category).color
    }

    val iconVector = when (transaction.type) {
        TransactionType.INCOME.name -> IncomeSource.fromString(transaction.category).icon()
        TransactionType.TRANSFER.name -> Icons.Default.SwapHoriz
        else -> ExpenseCategory.fromString(transaction.category).icon()
    }

    val formattedDate = formatRelativeDate(transaction.timestamp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("transaction_item_${transaction.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            // Category Icon Badge
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = transaction.category,
                    tint = categoryColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${transaction.category} · $formattedDate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Payment method tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = transaction.paymentMethod,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Amount Display
        val amountPrefix = when (transaction.type) {
            TransactionType.EXPENSE.name -> "−Rs. "
            TransactionType.INCOME.name -> "+Rs. "
            else -> "Rs. "
        }
        val amountColor = when (transaction.type) {
            TransactionType.EXPENSE.name -> ExpenseRed
            TransactionType.INCOME.name -> IncomeGreen
            else -> TransferBlue
        }

        Text(
            text = "$amountPrefix${String.format(Locale.getDefault(), "%,.0f", transaction.amount)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = amountColor
        )
    }
}

fun formatRelativeDate(timestamp: Long): String {
    val now = Calendar.getInstance()
    val txCal = Calendar.getInstance().apply { timeInMillis = timestamp }

    return when {
        now.get(Calendar.YEAR) == txCal.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == txCal.get(Calendar.DAY_OF_YEAR) -> "Today"

        now.get(Calendar.YEAR) == txCal.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) - txCal.get(Calendar.DAY_OF_YEAR) == 1 -> "Yesterday"

        else -> {
            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
