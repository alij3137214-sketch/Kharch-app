package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ExpenseCategory
import com.example.data.model.IncomeSource
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.components.BudgetExceededConfirmDialog
import com.example.ui.components.ExpenseBudgetWarningCard
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TransferBlue
import com.example.ui.viewmodel.KharchViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

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
            if (editingTransaction != null) {
                if (editingTransaction.amount % 1 == 0.0) editingTransaction.amount.toInt().toString()
                else editingTransaction.amount.toString()
            } else if (prefilledAmount != null) {
                if (prefilledAmount % 1 == 0.0) prefilledAmount.toInt().toString()
                else prefilledAmount.toString()
            } else ""
        )
    }

    var titleText by remember {
        mutableStateOf(editingTransaction?.title ?: prefilledTitle ?: "")
    }

    var selectedCategory by remember {
        mutableStateOf(
            editingTransaction?.category ?: prefilledCategory ?: ExpenseCategory.FOOD.displayName
        )
    }

    var selectedIncomeSource by remember {
        mutableStateOf(
            if (editingTransaction?.type == TransactionType.INCOME.name) editingTransaction.category
            else IncomeSource.SALARY.displayName
        )
    }

    var selectedPaymentMethod by remember {
        mutableStateOf(editingTransaction?.paymentMethod ?: PaymentMethod.CASH.displayName)
    }

    var toPaymentMethod by remember {
        mutableStateOf(editingTransaction?.toPaymentMethod ?: PaymentMethod.EASYPAISA.displayName)
    }

    var noteText by remember {
        mutableStateOf(editingTransaction?.note ?: "")
    }

    var receiptUri by remember {
        mutableStateOf<String?>(editingTransaction?.receiptUri ?: prefilledReceiptUri)
    }

    var showSuccessFeedback by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            receiptUri = uri.toString()
        }
    }

    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val canSave = parsedAmount > 0.0

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showExceedConfirmDialog by remember { mutableStateOf(false) }

    val budgetImpact = remember(uiState, selectedTab, selectedCategory, parsedAmount) {
        if (selectedTab == TransactionType.EXPENSE && parsedAmount > 0) {
            uiState.getBudgetImpact(selectedCategory, parsedAmount)
        } else {
            null
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("add_transaction_screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Top Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingTransaction != null) "Edit Transaction" else "Add Record",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
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

        // Tab Selector (Expense / Income / Transfer) - Only show if not editing
        if (editingTransaction == null) {
            item {
                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clip(RoundedCornerShape(14.dp))
                ) {
                    Tab(
                        selected = selectedTab == TransactionType.EXPENSE,
                        onClick = { selectedTab = TransactionType.EXPENSE },
                        text = {
                            Text(
                                "Expense",
                                fontWeight = if (selectedTab == TransactionType.EXPENSE) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == TransactionType.EXPENSE) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == TransactionType.INCOME,
                        onClick = { selectedTab = TransactionType.INCOME },
                        text = {
                            Text(
                                "Income",
                                fontWeight = if (selectedTab == TransactionType.INCOME) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == TransactionType.INCOME) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == TransactionType.TRANSFER,
                        onClick = { selectedTab = TransactionType.TRANSFER },
                        text = {
                            Text(
                                "Transfer",
                                fontWeight = if (selectedTab == TransactionType.TRANSFER) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == TransactionType.TRANSFER) TransferBlue else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Large Amount Input Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when (selectedTab) {
                            TransactionType.EXPENSE -> "EXPENSE AMOUNT"
                            TransactionType.INCOME -> "INCOME AMOUNT"
                            TransactionType.TRANSFER -> "TRANSFER AMOUNT"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Rs. ",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = when (selectedTab) {
                                TransactionType.EXPENSE -> ExpenseRed
                                TransactionType.INCOME -> IncomeGreen
                                TransactionType.TRANSFER -> TransferBlue
                            }
                        )

                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() || it == '.' }) {
                                    amountText = input
                                }
                            },
                            placeholder = {
                                Text(
                                    "0",
                                    style = MaterialTheme.typography.displayMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .width(200.dp)
                                .testTag("amount_input")
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        // Real-time Budget Warning & Threshold Alert Card
        if (selectedTab == TransactionType.EXPENSE && budgetImpact != null) {
            item {
                ExpenseBudgetWarningCard(impact = budgetImpact)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Title / Description
        if (selectedTab != TransactionType.TRANSFER) {
            item {
                Text(
                    text = if (selectedTab == TransactionType.EXPENSE) "What did you spend on?" else "Income Title / Description",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    placeholder = {
                        Text(if (selectedTab == TransactionType.EXPENSE) "e.g. McDonald's, Fuel, Groceries" else "e.g. Monthly Salary, Freelance project")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("title_input"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )
                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        // Category Selection
        if (selectedTab == TransactionType.EXPENSE) {
            item {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExpenseCategory.entries.forEach { category ->
                        val isSelected = selectedCategory.equals(category.displayName, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) category.color.copy(alpha = 0.18f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) category.color else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedCategory = category.displayName }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = category.icon(),
                                contentDescription = category.displayName,
                                tint = if (isSelected) category.color else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = category.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) category.color else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
        } else if (selectedTab == TransactionType.INCOME) {
            item {
                Text(
                    text = "Income Source",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IncomeSource.entries.forEach { source ->
                        val isSelected = selectedIncomeSource.equals(source.displayName, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) IncomeGreen.copy(alpha = 0.18f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) IncomeGreen else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedIncomeSource = source.displayName }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = source.icon(),
                                contentDescription = source.displayName,
                                tint = if (isSelected) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = source.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) IncomeGreen else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        // Payment Method Selection
        item {
            Text(
                text = if (selectedTab == TransactionType.TRANSFER) "From Account / Wallet" else "Payment Method",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaymentMethod.entries.forEach { method ->
                    val isSelected = selectedPaymentMethod.equals(method.displayName, ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 0.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedPaymentMethod = method.displayName }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = method.icon(),
                            contentDescription = method.displayName,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = method.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        // Transfer Target Account (if transfer)
        if (selectedTab == TransactionType.TRANSFER) {
            item {
                Text(
                    text = "To Account / Wallet",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentMethod.entries.forEach { method ->
                        val isSelected = toPaymentMethod.equals(method.displayName, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) TransferBlue.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) TransferBlue else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { toPaymentMethod = method.displayName }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = method.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) TransferBlue else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        // Optional Note
        item {
            Text(
                text = "Note (Optional)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                placeholder = { Text("Add any extra notes or details...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_input"),
                shape = RoundedCornerShape(14.dp),
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )
            Spacer(modifier = Modifier.height(18.dp))
        }

        // Receipt Photo Selection (Only for Expense)
        if (selectedTab == TransactionType.EXPENSE) {
            item {
                Text(
                    text = "Receipt Attachment (Optional)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (receiptUri == null) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("add_receipt_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Attach Receipt",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Attach Receipt Photo")
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AsyncImage(
                                model = receiptUri,
                                contentDescription = "Receipt Preview",
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Text(
                                text = "Receipt photo attached",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        IconButton(onClick = { receiptUri = null }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove receipt",
                                tint = ExpenseRed
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(26.dp))
            }
        }

        // Save Button & Subtle Success Animation
        item {
            AnimatedVisibility(visible = showSuccessFeedback) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(IncomeGreen.copy(alpha = 0.15f))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Success",
                            tint = IncomeGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Record saved successfully!",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen
                        )
                    }
                }
            }

            val executeSaveTransaction: () -> Unit = {
                val finalAmount = parsedAmount

                if (editingTransaction != null) {
                    viewModel.updateTransaction(
                        editingTransaction.copy(
                            title = titleText.ifBlank {
                                if (selectedTab == TransactionType.EXPENSE) selectedCategory else selectedIncomeSource
                            },
                            amount = finalAmount,
                            type = selectedTab.name,
                            category = if (selectedTab == TransactionType.EXPENSE) selectedCategory
                            else if (selectedTab == TransactionType.INCOME) selectedIncomeSource
                            else "Transfer",
                            paymentMethod = selectedPaymentMethod,
                            toPaymentMethod = if (selectedTab == TransactionType.TRANSFER) toPaymentMethod else null,
                            note = noteText,
                            receiptUri = receiptUri
                        )
                    )
                } else {
                    when (selectedTab) {
                        TransactionType.EXPENSE -> {
                            viewModel.addExpense(
                                title = titleText.ifBlank { selectedCategory },
                                amount = finalAmount,
                                category = selectedCategory,
                                paymentMethod = selectedPaymentMethod,
                                note = noteText,
                                receiptUri = receiptUri
                            )
                        }
                        TransactionType.INCOME -> {
                            viewModel.addIncome(
                                title = titleText.ifBlank { selectedIncomeSource },
                                amount = finalAmount,
                                source = selectedIncomeSource,
                                paymentMethod = selectedPaymentMethod,
                                note = noteText
                            )
                        }
                        TransactionType.TRANSFER -> {
                            viewModel.addTransfer(
                                amount = finalAmount,
                                fromMethod = selectedPaymentMethod,
                                toMethod = toPaymentMethod,
                                note = noteText
                            )
                        }
                    }
                }

                showSuccessFeedback = true
                coroutineScope.launch {
                    delay(650)
                    onTransactionSaved()
                }
            }

            Button(
                onClick = {
                    if (!canSave) return@Button
                    if (selectedTab == TransactionType.EXPENSE && budgetImpact?.willExceed == true) {
                        showExceedConfirmDialog = true
                    } else {
                        executeSaveTransaction()
                    }
                },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_transaction_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (selectedTab) {
                        TransactionType.EXPENSE -> EmeraldPrimary
                        TransactionType.INCOME -> IncomeGreen
                        TransactionType.TRANSFER -> TransferBlue
                    }
                )
            ) {
                Text(
                    text = if (editingTransaction != null) "Update Record"
                    else when (selectedTab) {
                        TransactionType.EXPENSE -> "Save Expense"
                        TransactionType.INCOME -> "Save Income"
                        TransactionType.TRANSFER -> "Save Transfer"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    // Budget Exceeded Confirmation Dialog
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
                val finalAmount = parsedAmount

                if (editingTransaction != null) {
                    viewModel.updateTransaction(
                        editingTransaction.copy(
                            title = titleText.ifBlank { selectedCategory },
                            amount = finalAmount,
                            type = selectedTab.name,
                            category = selectedCategory,
                            paymentMethod = selectedPaymentMethod,
                            note = noteText,
                            receiptUri = receiptUri
                        )
                    )
                } else {
                    viewModel.addExpense(
                        title = titleText.ifBlank { selectedCategory },
                        amount = finalAmount,
                        category = selectedCategory,
                        paymentMethod = selectedPaymentMethod,
                        note = noteText,
                        receiptUri = receiptUri
                    )
                }

                showSuccessFeedback = true
                coroutineScope.launch {
                    delay(650)
                    onTransactionSaved()
                }
            }
        )
    }
}
