package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.components.DayHeader
import com.example.ui.components.ExportCsvDialog
import com.example.ui.components.ScreenPadding
import com.example.ui.components.TransactionDetailSheet
import com.example.ui.components.TransactionItem
import com.example.ui.components.dayLabel
import com.example.ui.components.entrance
import com.example.ui.components.formatRs
import com.example.ui.components.pressable
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.KharchUiState
import com.example.ui.viewmodel.KharchViewModel

/**
 * Everything you've logged, grouped by day, searchable and filterable.
 */
@Composable
fun ActivityScreen(
    modifier: Modifier = Modifier,
    uiState: KharchUiState,
    viewModel: KharchViewModel,
    onQuickExpense: () -> Unit,
    onQuickIncome: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onSeedRandomData: () -> Unit,
    onClearAllData: () -> Unit
) {
    var selectedTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var showConfirmClearDialog by remember { mutableStateOf(false) }
    var showExportCsvDialog by remember { mutableStateOf(false) }

    val filterOptions = listOf(
        "ALL" to "All",
        "EXPENSE" to "Expenses",
        "INCOME" to "Income",
        "TRANSFER" to "Transfers"
    )

    val filtered = uiState.filteredTransactions
    val grouped = remember(filtered) { filtered.groupBy { dayLabel(it.timestamp) } }
    val totalInflow = remember(filtered) { filtered.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount } }
    val totalOutflow = remember(filtered) { filtered.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("activity_screen"),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = ScreenPadding, end = 8.dp, top = 18.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Activity",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${filtered.size} ${if (filtered.size == 1) "record" else "records"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    IconButton(onClick = { showExportCsvDialog = true }, modifier = Modifier.testTag("activity_export_csv_btn")) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export CSV", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (uiState.transactions.isNotEmpty()) {
                        IconButton(onClick = { showConfirmClearDialog = true }, modifier = Modifier.testTag("activity_clear_all_btn")) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Clear all records", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ScreenPadding)
                    .entrance(0),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryTile("In", "+" + formatRs(totalInflow), IncomeGreen, Modifier.weight(1f))
                SummaryTile("Out", "−" + formatRs(totalOutflow), ExpenseRed, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        item {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ScreenPadding)
                    .testTag("activity_search_text_field"),
                placeholder = { Text("Search records") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (uiState.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    cursorColor = EmeraldPrimary
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = ScreenPadding),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterOptions) { (key, label) ->
                    FilterPill(label = label, selected = uiState.selectedFilterType == key) {
                        viewModel.setFilterType(key)
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ScreenPadding, vertical = 56.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = if (uiState.searchQuery.isNotBlank()) "No matches" else "No records yet",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (uiState.searchQuery.isNotBlank()) "Try a different word or clear the filter." else "Log an expense or income to get started.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onQuickExpense,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = MaterialTheme.colorScheme.onPrimary),
                            modifier = Modifier.testTag("activity_empty_add_expense_btn")
                        ) { Text("Add expense") }
                        TextButton(
                            onClick = onSeedRandomData,
                            modifier = Modifier.testTag("activity_empty_seed_data_btn")
                        ) { Text("Try sample data") }
                    }
                }
            }
        } else {
            grouped.forEach { (label, dayItems) ->
                item(key = "hdr_$label") {
                    DayHeader(
                        label = label,
                        total = dayItems.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
                    )
                }
                items(dayItems, key = { it.id }) { tx ->
                    TransactionItem(transaction = tx, onClick = { selectedTransaction = tx })
                }
            }
        }
    }

    if (showConfirmClearDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmClearDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Clear all records?", fontWeight = FontWeight.Bold) },
            text = { Text("Every transaction will be deleted. This can't be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmClearDialog = false
                        onClearAllData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                    modifier = Modifier.testTag("confirm_clear_activity_dialog_btn")
                ) { Text("Delete all") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmClearDialog = false },
                    modifier = Modifier.testTag("cancel_clear_activity_dialog_btn")
                ) { Text("Cancel") }
            }
        )
    }

    selectedTransaction?.let { tx ->
        TransactionDetailSheet(
            transaction = tx,
            onDismiss = { selectedTransaction = null },
            onEdit = {
                selectedTransaction = null
                onEditTransaction(it)
            },
            onDelete = {
                selectedTransaction = null
                onDeleteTransaction(it)
            }
        )
    }

    if (showExportCsvDialog) {
        val currentCalendar = remember { java.util.Calendar.getInstance() }
        val currentMonthLabel = remember(currentCalendar) {
            java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()).format(currentCalendar.time)
        }
        ExportCsvDialog(
            monthLabel = currentMonthLabel,
            calendar = currentCalendar,
            expenses = uiState.transactions,
            onDismiss = { showExportCsvDialog = false }
        )
    }
}

@Composable
private fun SummaryTile(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        if (selected) EmeraldPrimary.copy(alpha = 0.16f) else Color.Transparent,
        tween(200),
        label = "pill_bg"
    )
    val border by animateColorAsState(
        if (selected) EmeraldPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outline,
        tween(200),
        label = "pill_border"
    )
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .pressable(pressedScale = 0.95f, onClick = onClick)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, border, CircleShape)
            .padding(horizontal = 16.dp, vertical = 9.dp)
    )
}
