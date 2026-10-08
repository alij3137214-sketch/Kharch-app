package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Savings
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.components.KharchSplash
import com.example.ui.components.ReceiptScannerDialog
import com.example.ui.components.pressable
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
    var showSplash by remember { mutableStateOf(true) }

    // Add / Edit Transaction Screen contextual parameters
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var prefilledType by remember { mutableStateOf<TransactionType?>(null) }
    var prefilledTitle by remember { mutableStateOf<String?>(null) }
    var prefilledAmount by remember { mutableStateOf<Double?>(null) }
    var prefilledCategory by remember { mutableStateOf<String?>(null) }
    var prefilledReceiptUri by remember { mutableStateOf<String?>(null) }

    var showReceiptScanner by remember { mutableStateOf(false) }

    // Back from any inner screen goes to Home first.
    BackHandler(enabled = !showSplash && currentScreen != AppScreen.HOME) {
        currentScreen = if (currentScreen == AppScreen.REPORTS) AppScreen.UNDERSTAND else AppScreen.HOME
        if (currentScreen == AppScreen.HOME) editingTransaction = null
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

    fun editTransaction(tx: TransactionEntity) =
        openAddScreen(type = TransactionType.valueOf(tx.type), editTx = tx)

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
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
                    .padding(top = innerPadding.calculateTopPadding())
                    .background(MaterialTheme.colorScheme.background)
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        (fadeIn(tween(260, delayMillis = 60)) + slideInVertically(tween(320)) { it / 28 }) togetherWith
                            fadeOut(tween(120))
                    },
                    label = "screen_transition"
                ) { targetScreen ->
                    when (targetScreen) {
                        AppScreen.HOME -> HomeScreen(
                            uiState = uiState,
                            onToggleBalanceVisibility = { viewModel.toggleBalanceVisibility() },
                            onQuickExpense = { openAddScreen(type = TransactionType.EXPENSE) },
                            onQuickIncome = { openAddScreen(type = TransactionType.INCOME) },
                            onQuickTransfer = { openAddScreen(type = TransactionType.TRANSFER) },
                            onQuickScanReceipt = { showReceiptScanner = true },
                            onBillClick = { currentScreen = AppScreen.PLAN },
                            onEditTransaction = { tx -> editTransaction(tx) },
                            onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) },
                            onNavigateToActivity = { currentScreen = AppScreen.ACTIVITY },
                            onNavigateToPlan = { currentScreen = AppScreen.PLAN },
                            onSeedRandomData = { viewModel.seedRandomTestData() },
                            onClearAllData = { viewModel.clearAllData() }
                        )

                        AppScreen.ACTIVITY -> ActivityScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            onQuickExpense = { openAddScreen(type = TransactionType.EXPENSE) },
                            onQuickIncome = { openAddScreen(type = TransactionType.INCOME) },
                            onEditTransaction = { tx -> editTransaction(tx) },
                            onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) },
                            onSeedRandomData = { viewModel.seedRandomTestData() },
                            onClearAllData = { viewModel.clearAllData() }
                        )

                        AppScreen.UNDERSTAND -> SpendingScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            onEditTransaction = { tx -> editTransaction(tx) },
                            onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) },
                            onNavigateToReports = { currentScreen = AppScreen.REPORTS }
                        )

                        AppScreen.ADD -> AddTransactionScreen(
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

                        AppScreen.REPORTS -> ReportsScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            onTransactionClick = { tx -> editTransaction(tx) }
                        )

                        AppScreen.PLAN -> PlanScreen(
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

        AnimatedVisibility(
            visible = showSplash,
            enter = fadeIn(tween(0)),
            exit = fadeOut(tween(450))
        ) {
            KharchSplash(onFinished = { showSplash = false })
        }
    }
}

/**
 * Floating navigation bar. The selected tab grows into a pill that shows its label;
 * the centre button is the thing you do most: add a record.
 */
@Composable
fun KharchBottomNavigation(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit
) {
    val barShape = RoundedCornerShape(32.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(barShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), barShape)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
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

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .pressable(pressedScale = 0.9f) { onNavigate(AppScreen.ADD) }
                    .clip(CircleShape)
                    .background(EmeraldPrimary)
                    .testTag("nav_add"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add record",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            NavItem(
                label = "Insights",
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
    val tint = if (selected) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .pressable(pressedScale = 0.92f, onClick = onClick)
            .clip(CircleShape)
            .background(if (selected) EmeraldPrimary.copy(alpha = 0.14f) else Color.Transparent)
            .animateContentSize(spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else unselectedIcon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        AnimatedVisibility(
            visible = selected,
            enter = expandHorizontally(tween(260)) + fadeIn(tween(260)),
            exit = shrinkHorizontally(tween(200)) + fadeOut(tween(120))
        ) {
            Row {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = tint,
                    maxLines = 1
                )
            }
        }
    }
}
