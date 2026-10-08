package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TransferBlue

@Composable
fun QuickActions(
    modifier: Modifier = Modifier,
    onExpenseClick: () -> Unit,
    onIncomeClick: () -> Unit,
    onTransferClick: () -> Unit,
    onScanReceiptClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        QuickActionButton(
            label = "Expense",
            icon = Icons.Default.Remove,
            iconTint = ExpenseRed,
            backgroundTint = ExpenseRed.copy(alpha = 0.12f),
            testTag = "quick_action_expense",
            onClick = onExpenseClick
        )

        QuickActionButton(
            label = "Income",
            icon = Icons.Default.Add,
            iconTint = IncomeGreen,
            backgroundTint = IncomeGreen.copy(alpha = 0.12f),
            testTag = "quick_action_income",
            onClick = onIncomeClick
        )

        QuickActionButton(
            label = "Transfer",
            icon = Icons.Default.SwapHoriz,
            iconTint = TransferBlue,
            backgroundTint = TransferBlue.copy(alpha = 0.12f),
            testTag = "quick_action_transfer",
            onClick = onTransferClick
        )

        QuickActionButton(
            label = "Scan",
            icon = Icons.Default.CameraAlt,
            iconTint = MaterialTheme.colorScheme.primary,
            backgroundTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            testTag = "quick_action_scan_receipt",
            onClick = onScanReceiptClick
        )
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    iconTint: Color,
    backgroundTint: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(backgroundTint),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
