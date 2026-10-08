package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.components.CyberpunkBankEntrance
import com.example.ui.components.ReceiptScannerDialog
import com.example.ui.screens.ActivityScreen
import com.example.ui.screens.AddTransactionScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PlanScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SpendingScreen
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.KharchTheme
import com.example.ui.viewmodel.KharchViewModel
import com.example.ui.viewmodel.KharchViewModelFactory

enum class AppScreen {
    HOME,
    ACTIVITY,
    UNDERSTAND,
    ADD,
    REPORTS,
    PLAN;

    companion object {
        val SPENDING = UNDERSTAND
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KharchTheme {
                val context = LocalContext.current
                val viewModel: KharchViewModel = viewModel(
                    factory = KharchViewModelFactory(context)
                )

                KharchMainApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun KharchMainApp(viewModel: KharchViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentScreen by remember { mutableStateOf(AppScreen.HOME) }
    var showCyberpunkEntrance by remember { mutableStateOf(true) }

    // Add / Edit Transaction Screen contextual parameters
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var prefilledType by remember { mutableStateOf<TransactionType?>(null) }
    var prefilledTitle by remember { mutableStateOf<String?>(null) }
    var prefilledAmount by remember { mutableStateOf<Double?>(null) }
    var prefilledCategory by remember { mutableStateOf<String?>(null) }
    var prefilledReceiptUri by remember { mutableStateOf<String?>(null) }

    // Scanner Dialog state
    var showReceiptScanner by remember { mutableStateOf(false) }

    // Back handling for cyberpunk entrance
    BackHandler(enabled = showCyberpunkEntrance) {
        showCyberpunkEntrance = false
    }

    if (showCyberpunkEntrance) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF040810))
                .safeDrawingPadding()
        ) {
            CyberpunkBankEntrance(
                onEnterVault = { showCyberpunkEntrance = false }
            )
        }
        return
    }

    fun openAddScreen(
        type: TransactionType = TransactionType.EXPENSE,
        title: String? = null,
        amount: Double? = null,
        category: String? = null,
        receiptUri: String? = null,
        editTx: TransactionEntity? = null
    ) {
        editingTransaction = editTx
        prefilledType = type
        prefilledTitle = title
        prefilledAmount = amount
        prefilledCategory = category
        prefilledReceiptUri = receiptUri
        currentScreen = AppScreen.ADD
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
        bottomBar = {
            if (currentScreen != AppScreen.ADD) {
                KharchBottomNavigation(
                    currentScreen = currentScreen,
                    onNavigate = { screen ->
                        if (screen == AppScreen.ADD) {
                            openAddScreen(type = TransactionType.EXPENSE)
                        } else {
                            currentScreen = screen
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen_transition"
            ) { targetScreen ->
                when (targetScreen) {
                    AppScreen.HOME -> {
                        HomeScreen(
                            uiState = uiState,
                            onToggleBalanceVisibility = { viewModel.toggleBalanceVisibility() },
                            onQuickExpense = { openAddScreen(type = TransactionType.EXPENSE) },
                            onQuickIncome = { openAddScreen(type = TransactionType.INCOME) },
                            onQuickTransfer = { openAddScreen(type = TransactionType.TRANSFER) },
                            onQuickScanReceipt = { showReceiptScanner = true },
                            onSearchQueryChange = { query -> viewModel.setSearchQuery(query) },
                            onFilterTypeChange = { filter -> viewModel.setFilterType(filter) },
                            onBillClick = { currentScreen = AppScreen.PLAN },
                            onEditTransaction = { tx -> openAddScreen(editTx = tx) },
                            onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) },
                            onNavigateToActivity = { currentScreen = AppScreen.ACTIVITY },
                            onNavigateToPlan = { currentScreen = AppScreen.PLAN },
                            onOpenVaultEntrance = { showCyberpunkEntrance = true },
                            onSeedRandomData = { viewModel.seedRandomTestData() },
                            onClearAllData = { viewModel.clearAllData() }
                        )
                    }

                    AppScreen.ACTIVITY -> {
                        ActivityScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            onQuickExpense = { openAddScreen(type = TransactionType.EXPENSE) },
                            onQuickIncome = { openAddScreen(type = TransactionType.INCOME) },
                            onEditTransaction = { tx -> openAddScreen(editTx = tx) },
                            onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) },
                            onSeedRandomData = { viewModel.seedRandomTestData() },
                            onClearAllData = { viewModel.clearAllData() }
                        )
                    }

                    AppScreen.UNDERSTAND -> {
                        SpendingScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            onEditTransaction = { tx -> openAddScreen(editTx = tx) },
                            onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) },
                            onNavigateToReports = { currentScreen = AppScreen.REPORTS }
                        )
                    }

                    AppScreen.ADD -> {
                        AddTransactionScreen(
                            viewModel = viewModel,
                            editingTransaction = editingTransaction,
                            prefilledTitle = prefilledTitle,
                            prefilledAmount = prefilledAmount,
                            prefilledCategory = prefilledCategory,
                            prefilledReceiptUri = prefilledReceiptUri,
                            prefilledType = prefilledType,
                            onTransactionSaved = {
                                editingTransaction = null
                                currentScreen = AppScreen.HOME
                            },
                            onCancel = {
                                editingTransaction = null
                                currentScreen = AppScreen.HOME
                            }
                        )
                    }

                    AppScreen.REPORTS -> {
                        ReportsScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            onTransactionClick = { tx -> openAddScreen(editTx = tx) }
                        )
                    }

                    AppScreen.PLAN -> {
                        PlanScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            onPlanExpense = { itemName, amount, category ->
                                openAddScreen(
                                    type = TransactionType.EXPENSE,
                                    title = itemName,
                                    amount = amount,
                                    category = category
                                )
                            }
                        )
                    }
                }
            }

            // Receipt Scanner Dialog
            if (showReceiptScanner) {
                ReceiptScannerDialog(
                    onDismiss = { showReceiptScanner = false },
                    onReceiptExtracted = { title, amount, category, receiptUri ->
                        showReceiptScanner = false
                        openAddScreen(
                            type = TransactionType.EXPENSE,
                            title = title,
                            amount = amount,
                            category = category,
                            receiptUri = receiptUri
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun KharchBottomNavigation(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(26.dp), spotColor = Color.Black.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(
                label = "Home",
                selected = currentScreen == AppScreen.HOME,
                selectedIcon = Icons.Filled.Home,
                unselectedIcon = Icons.Outlined.Home,
                testTag = "nav_home",
                onClick = { onNavigate(AppScreen.HOME) }
            )

            NavItem(
                label = "Activity",
                selected = currentScreen == AppScreen.ACTIVITY,
                selectedIcon = Icons.Filled.ReceiptLong,
                unselectedIcon = Icons.Outlined.ReceiptLong,
                testTag = "nav_activity",
                onClick = { onNavigate(AppScreen.ACTIVITY) }
            )

            // Center Prominent Add Button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(EmeraldPrimary)
                    .clickable { onNavigate(AppScreen.ADD) }
                    .testTag("nav_add"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Record",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            NavItem(
                label = "Analytics",
                selected = currentScreen == AppScreen.UNDERSTAND || currentScreen == AppScreen.REPORTS,
                selectedIcon = Icons.Filled.PieChart,
                unselectedIcon = Icons.Outlined.PieChart,
                testTag = "nav_understand",
                onClick = { onNavigate(AppScreen.UNDERSTAND) }
            )

            NavItem(
                label = "Plan",
                selected = currentScreen == AppScreen.PLAN,
                selectedIcon = Icons.Filled.Savings,
                unselectedIcon = Icons.Outlined.Savings,
                testTag = "nav_plan",
                onClick = { onNavigate(AppScreen.PLAN) }
            )
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else unselectedIcon,
            contentDescription = label,
            tint = if (selected) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}
