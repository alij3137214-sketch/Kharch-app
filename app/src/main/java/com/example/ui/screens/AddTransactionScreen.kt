package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.ExpenseCategory
import com.example.data.model.IncomeSource
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.components.BudgetExceededConfirmDialog
import com.example.ui.components.ExpenseBudgetWarningCard
import com.example.ui.components.ScreenPadding
import com.example.ui.components.pressable
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.TransferBlue
import com.example.ui.viewmodel.KharchViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTransactionScreen(
    modifier: Modifier = Modifier,
    viewModel: KharchViewModel,
    editingTransaction: TransactionEntity? = null,
    prefilledTitle: String? = null,
    prefilledAmount: Double? = null,
    prefilledCategory: String? = null,
    prefilledReceiptUri: String? = null,
    prefilledType: TransactionType? = null,
    onTransactionSaved: () -> Unit = {},
    onCancel: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember {
        mutableStateOf(
            when {
                editingTransaction != null -> TransactionType.valueOf(editingTransaction.type)
                prefilledType != null -> prefilledType
                else -> TransactionType.EXPENSE
            }
        )
    }

    var amountText by remember {
        mutableStateOf(
            when {
                editingTransaction != null -> editingTransaction.amount.toPlainString()
                prefilledAmount != null -> prefilledAmount.toPlainString()
                else -> ""
            }
        )
    }
    var titleText by remember { mutableStateOf(editingTransaction?.title ?: prefilledTitle ?: "") }
    var selectedCategory by remember {
        mutableStateOf(editingTransaction?.category ?: prefilledCategory ?: ExpenseCategory.FOOD.displayName)
    }
    var selectedIncomeSource by remember {
        mutableStateOf(
            if (editingTransaction?.type == TransactionType.INCOME.name) editingTransaction.category
            else IncomeSource.SALARY.displayName
        )
    }
    var selectedPaymentMethod by remember { mutableStateOf(editingTransaction?.paymentMethod ?: PaymentMethod.CASH.displayName) }
    var toPaymentMethod by remember { mutableStateOf(editingTransaction?.toPaymentMethod ?: PaymentMethod.EASYPAISA.displayName) }
    var noteText by remember { mutableStateOf(editingTransaction?.note ?: "") }
    var receiptUri by remember { mutableStateOf<String?>(editingTransaction?.receiptUri ?: prefilledReceiptUri) }
    var showSuccessFeedback by remember { mutableStateOf(false) }
    var showExceedConfirmDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> if (uri != null) receiptUri = uri.toString() }

    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val canSave = parsedAmount > 0.0

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val budgetImpact = remember(uiState, selectedTab, selectedCategory, parsedAmount) {
        if (selectedTab == TransactionType.EXPENSE && parsedAmount > 0) uiState.getBudgetImpact(selectedCategory, parsedAmount) else null
    }

    val typeColor = when (selectedTab) {
        TransactionType.EXPENSE -> ExpenseRed
        TransactionType.INCOME -> EmeraldPrimary
        TransactionType.TRANSFER -> TransferBlue
    }

    val amountFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (editingTransaction == null && prefilledAmount == null) {
            delay(250)
            runCatching { amountFocus.requestFocus() }
        }
    }

    fun save() {
        if (editingTransaction != null) {
            viewModel.updateTransaction(
                editingTransaction.copy(
                    title = titleText.ifBlank { if (selectedTab == TransactionType.EXPENSE) selectedCategory else selectedIncomeSource },
                    amount = parsedAmount,
                    type = selectedTab.name,
                    category = when (selectedTab) {
                        TransactionType.EXPENSE -> selectedCategory
                        TransactionType.INCOME -> selectedIncomeSource
                        TransactionType.TRANSFER -> "Transfer"
                    },
                    paymentMethod = selectedPaymentMethod,
                    toPaymentMethod = if (selectedTab == TransactionType.TRANSFER) toPaymentMethod else null,
                    note = noteText,
                    receiptUri = receiptUri
                )
            )
        } else {
            when (selectedTab) {
                TransactionType.EXPENSE -> viewModel.addExpense(
                    title = titleText.ifBlank { selectedCategory },
                    amount = parsedAmount,
                    category = selectedCategory,
                    paymentMethod = selectedPaymentMethod,
                    note = noteText,
                    receiptUri = receiptUri
                )
                TransactionType.INCOME -> viewModel.addIncome(
                    title = titleText.ifBlank { selectedIncomeSource },
                    amount = parsedAmount,
                    source = selectedIncomeSource,
                    paymentMethod = selectedPaymentMethod,
                    note = noteText
                )
                TransactionType.TRANSFER -> viewModel.addTransfer(
                    amount = parsedAmount,
                    fromMethod = selectedPaymentMethod,
                    toMethod = toPaymentMethod,
                    note = noteText
                )
            }
        }
        showSuccessFeedback = true
        coroutineScope.launch {
            delay(650)
            onTransactionSaved()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .testTag("add_transaction_screen")
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = ScreenPadding, vertical = 12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            editingTransaction != null -> "Edit record"
                            selectedTab == TransactionType.EXPENSE -> "New expense"
                            selectedTab == TransactionType.INCOME -> "New income"
                            else -> "New transfer"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            if (editingTransaction == null) {
                item {
                    TypeSwitch(selected = selectedTab, onSelect = { selectedTab = it })
                    Spacer(modifier = Modifier.height(28.dp))
                }
            }

            // Amount
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "Rs.",
                            style = MaterialTheme.typography.titleLarge,
                            color = typeColor,
                            modifier = Modifier.padding(bottom = 10.dp, end = 8.dp)
                        )
                        BasicTextField(
                            value = amountText,
                            onValueChange = { input ->
                                if (input.count { it == '.' } <= 1 && input.all { it.isDigit() || it == '.' } && input.length <= 12) {
                                    amountText = input
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.displayLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(typeColor),
                            decorationBox = { inner ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (amountText.isEmpty()) {
                                        Text(
                                            text = "0",
                                            style = MaterialTheme.typography.displayLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                        )
                                    }
                                    inner()
                                }
                            },
                            modifier = Modifier
                                .width(IntrinsicSize.Min)
                                .focusRequester(amountFocus)
                                .testTag("amount_input")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            if (selectedTab == TransactionType.EXPENSE && budgetImpact != null) {
                item {
                    ExpenseBudgetWarningCard(impact = budgetImpact)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            if (selectedTab != TransactionType.TRANSFER) {
                item {
                    FieldLabel(if (selectedTab == TransactionType.EXPENSE) "What was it for?" else "Where did it come from?")
                    OutlinedTextField(
                        value = titleText,
                        onValueChange = { titleText = it },
                        placeholder = {
                            Text(if (selectedTab == TransactionType.EXPENSE) "e.g. Lunch, Fuel, Groceries" else "e.g. Salary, Freelance project")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("title_input"),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = fieldColors()
                    )
                    Spacer(modifier = Modifier.height(22.dp))
                }
            }

            if (selectedTab == TransactionType.EXPENSE) {
                item {
                    FieldLabel("Category")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExpenseCategory.entries.forEach { category ->
                            ChoiceChip(
                                label = category.displayName,
                                selected = selectedCategory.equals(category.displayName, ignoreCase = true),
                                color = category.color,
                                icon = category.icon(),
                                onClick = { selectedCategory = category.displayName }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(22.dp))
                }
            } else if (selectedTab == TransactionType.INCOME) {
                item {
                    FieldLabel("Income source")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IncomeSource.entries.forEach { source ->
                            ChoiceChip(
                                label = source.displayName,
                                selected = selectedIncomeSource.equals(source.displayName, ignoreCase = true),
                                color = EmeraldPrimary,
                                icon = source.icon(),
                                onClick = { selectedIncomeSource = source.displayName }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(22.dp))
                }
            }

            item {
                FieldLabel(if (selectedTab == TransactionType.TRANSFER) "From" else "Paid with")
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentMethod.entries.forEach { method ->
                        ChoiceChip(
                            label = method.displayName,
                            selected = selectedPaymentMethod.equals(method.displayName, ignoreCase = true),
                            color = MaterialTheme.colorScheme.onSurface,
                            icon = method.icon(),
                            onClick = { selectedPaymentMethod = method.displayName }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(22.dp))
            }

            if (selectedTab == TransactionType.TRANSFER) {
                item {
                    FieldLabel("To")
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PaymentMethod.entries.forEach { method ->
                            ChoiceChip(
                                label = method.displayName,
                                selected = toPaymentMethod.equals(method.displayName, ignoreCase = true),
                                color = TransferBlue,
                                icon = method.icon(),
                                onClick = { toPaymentMethod = method.displayName }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(22.dp))
                }
            }

            item {
                FieldLabel("Note (optional)")
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    placeholder = { Text("Anything to remember?") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_input"),
                    shape = RoundedCornerShape(16.dp),
                    maxLines = 3,
                    colors = fieldColors()
                )
                Spacer(modifier = Modifier.height(22.dp))
            }

            if (selectedTab == TransactionType.EXPENSE) {
                item {
                    FieldLabel("Receipt (optional)")
                    if (receiptUri == null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pressable(pressedScale = 0.98f) {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                                .padding(vertical = 16.dp)
                                .testTag("add_receipt_button"),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Attach a photo",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model = receiptUri,
                                    contentDescription = "Receipt preview",
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Photo attached", style = MaterialTheme.typography.titleSmall)
                            }
                            IconButton(onClick = { receiptUri = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove receipt", tint = ExpenseRed)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Pinned save bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = ScreenPadding, vertical = 12.dp)
        ) {
            AnimatedVisibility(
                visible = showSuccessFeedback,
                enter = fadeIn(tween(200)) + slideInVertically(tween(260)) { it / 2 },
                exit = fadeOut() + slideOutVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(EmeraldPrimary.copy(alpha = 0.14f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saved", style = MaterialTheme.typography.titleSmall, color = EmeraldPrimary)
                }
            }
            Button(
                onClick = {
                    if (!canSave) return@Button
                    if (selectedTab == TransactionType.EXPENSE && budgetImpact?.willExceed == true) {
                        showExceedConfirmDialog = true
                    } else {
                        save()
                    }
                },
                enabled = canSave && !showSuccessFeedback,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("save_transaction_button"),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    text = if (editingTransaction != null) "Update" else when (selectedTab) {
                        TransactionType.EXPENSE -> "Save expense"
                        TransactionType.INCOME -> "Save income"
                        TransactionType.TRANSFER -> "Save transfer"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }

    if (showExceedConfirmDialog && budgetImpact != null) {
        BudgetExceededConfirmDialog(
            category = selectedCategory,
            addedAmount = parsedAmount,
            excessAmount = budgetImpact.excessAmount,
            projectedTotal = budgetImpact.projectedSpent,
            monthlyLimit = budgetImpact.monthlyLimit,
            onAdjust = { showExceedConfirmDialog = false },
            onConfirm = {
                showExceedConfirmDialog = false
                save()
            }
        )
    }
}

private fun Double.toPlainString(): String =
    if (this % 1 == 0.0) this.toLong().toString() else this.toString()

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = EmeraldPrimary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    cursorColor = EmeraldPrimary
)

/** Expense / Income / Transfer switch. The selected segment lights up in its own color. */
@Composable
private fun TypeSwitch(selected: TransactionType, onSelect: (TransactionType) -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), shape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(
            Triple(TransactionType.EXPENSE, "Expense", ExpenseRed),
            Triple(TransactionType.INCOME, "Income", EmeraldPrimary),
            Triple(TransactionType.TRANSFER, "Transfer", TransferBlue)
        ).forEach { (type, label, color) ->
            val isSelected = selected == type
            val bg by animateColorAsState(
                if (isSelected) color.copy(alpha = 0.16f) else Color.Transparent,
                tween(220),
                label = "type_bg"
            )
            val fg by animateColorAsState(
                if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                tween(220),
                label = "type_fg"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg)
                    .pressable(pressedScale = 0.97f) { onSelect(type) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = label, style = MaterialTheme.typography.titleSmall, color = fg)
            }
        }
    }
}

@Composable
private fun ChoiceChip(
    label: String,
    selected: Boolean,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    val bg by animateColorAsState(
        if (selected) color.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
        tween(200),
        label = "chip_bg"
    )
    val border by animateColorAsState(
        if (selected) color.copy(alpha = 0.9f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
        tween(200),
        label = "chip_border"
    )
    val fg by animateColorAsState(
        if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(200),
        label = "chip_fg"
    )
    Row(
        modifier = Modifier
            .pressable(pressedScale = 0.95f, onClick = onClick)
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}
