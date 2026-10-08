package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

    val time = remember(transaction.timestamp) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(transaction.timestamp))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressable(pressedScale = 0.98f, onClick = onClick)
            .padding(horizontal = ScreenPadding, vertical = 10.dp)
            .testTag("transaction_item_${transaction.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon = iconVector, tint = categoryColor, size = 46.dp, contentDescription = transaction.category)

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = transaction.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${transaction.category} · ${transaction.paymentMethod} · $time",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        val sign = when (transaction.type) {
            TransactionType.EXPENSE.name -> "−"
            TransactionType.INCOME.name -> "+"
            else -> ""
        }
        val amountColor = when (transaction.type) {
            TransactionType.INCOME.name -> IncomeGreen
            else -> MaterialTheme.colorScheme.onSurface
        }
        Text(
            text = "$sign${formatRs(transaction.amount)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = amountColor
        )
    }
}

/** "Today", "Yesterday", or "Mon, 12 Oct" style label for grouping lists. */
fun dayLabel(timestamp: Long): String {
    val now = Calendar.getInstance()
    val tx = Calendar.getInstance().apply { timeInMillis = timestamp }
    val sameYear = now.get(Calendar.YEAR) == tx.get(Calendar.YEAR)
    val diff = now.get(Calendar.DAY_OF_YEAR) - tx.get(Calendar.DAY_OF_YEAR)
    return when {
        sameYear && diff == 0 -> "Today"
        sameYear && diff == 1 -> "Yesterday"
        else -> SimpleDateFormat(if (sameYear) "EEE, d MMM" else "d MMM yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

/** Small uppercase day header shown above a group of transactions. */
@Composable
fun DayHeader(label: String, total: Double? = null, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = ScreenPadding, end = ScreenPadding, top = 18.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (total != null && total > 0) {
            Text(
                text = "−" + formatRs(total),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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

