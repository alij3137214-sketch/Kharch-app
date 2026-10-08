package com.example

import android.Manifest
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.notify.Notifier
import com.example.notify.ReminderScheduler
import com.example.ui.components.DeleteDataSheet
import com.example.ui.components.KharchSplash
import com.example.ui.components.SettingsSheet
import com.example.ui.components.pressable
import com.example.ui.screens.ActivityScreen
import com.example.ui.screens.AddTransactionScreen
import com.example.ui.screens.CanBuyScreen
import com.example.ui.screens.CommitteeScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PlanScreen
import com.example.ui.screens.UdhaarScreen
import com.example.ui.screens.WishListScreen
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.KharchTheme
import com.example.ui.viewmodel.AppEvent
import com.example.ui.viewmodel.KharchViewModel
import com.example.ui.viewmodel.KharchViewModelFactory

enum class AppScreen {
    HOME,
    ACTIVITY,
    UNDERSTAND,
    ADD,
    PLAN,
    CANBUY,
    WISHLIST,
    UDHAAR,
    COMMITTEE;

    /** Pages opened from Plan or Home. Back goes to where the person came from. */
    val isHelper: Boolean get() = this == CANBUY || this == WISHLIST || this == UDHAAR || this == COMMITTEE
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is always dark, so the status bar and navigation bar icons must always be light.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        Notifier.ensureChannels(this)
        setContent {
            KharchTheme {
                val context = LocalContext.current
                val viewModel: KharchViewModel = viewModel(factory = KharchViewModelFactory(context))
                KharchMainApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun KharchMainApp(viewModel: KharchViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val profile = uiState.profile

    var currentScreen by rememberSaveable { mutableStateOf(AppScreen.HOME) }
    var returnTo by rememberSaveable { mutableStateOf(AppScreen.PLAN) }
    var showSplash by rememberSaveable { mutableStateOf(true) }
    var editingAnswers by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showDeleteData by rememberSaveable { mutableStateOf(false) }

    // Add / edit screen details
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var prefilledType by remember { mutableStateOf<TransactionType?>(null) }
    var prefilledTitle by remember { mutableStateOf<String?>(null) }
    var prefilledAmount by remember { mutableStateOf<Double?>(null) }
    var prefilledCategory by remember { mutableStateOf<String?>(null) }

    val snackbar = remember { SnackbarHostState() }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val askNotificationPermission: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Notifier.canNotify(context)) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Keep the daily alarms in step with what the person turned on.
    LaunchedEffect(profile.onboardingDone, profile.billReminders, profile.dailyReminder) {
        ReminderScheduler.schedule(context, profile)
    }

    // Messages from the ViewModel: say it on screen, and also as a phone notification for budget alerts.
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AppEvent.Message -> snackbar.showSnackbar(event.text)
                is AppEvent.BudgetNotice -> {
                    Notifier.showBudget(context, event.alert.title, event.alert.text)
                    snackbar.showSnackbar(event.alert.title + ". " + event.alert.text)
                }
            }
        }
    }

    fun openAddScreen(
        type: TransactionType = TransactionType.EXPENSE,
        title: String? = null,
        amount: Double? = null,
        category: String? = null,
        editTx: TransactionEntity? = null
    ) {
        editingTransaction = editTx
        prefilledType = type
        prefilledTitle = title
        prefilledAmount = amount
        prefilledCategory = category
        currentScreen = AppScreen.ADD
    }

    fun editTransaction(tx: TransactionEntity) = openAddScreen(type = TransactionType.valueOf(tx.type), editTx = tx)

    fun openHelper(screen: AppScreen) {
        returnTo = if (currentScreen.isHelper) returnTo else currentScreen
        currentScreen = screen
    }

    BackHandler(enabled = !showSplash && profile.onboardingDone && !editingAnswers && currentScreen != AppScreen.HOME) {
        if (currentScreen.isHelper) {
            currentScreen = returnTo
        } else {
            currentScreen = AppScreen.HOME
            editingTransaction = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // Phones with a notch or rounded corners: keep content out of the side cut-outs.
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
    ) {
        when {
            !profile.onboardingDone -> OnboardingScreen(
                initial = profile,
                isEditing = false,
                onFinish = { viewModel.completeOnboarding(it) },
                onAskNotificationPermission = askNotificationPermission
            )

            editingAnswers -> OnboardingScreen(
                initial = profile,
                isEditing = true,
                onFinish = {
                    viewModel.completeOnboarding(it)
                    editingAnswers = false
                },
                onAskNotificationPermission = askNotificationPermission,
                onClose = { editingAnswers = false }
            )

            else -> Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    if (currentScreen != AppScreen.ADD) {
                        KharchBottomNavigation(
                            currentScreen = currentScreen,
                            onNavigate = { screen ->
                                if (screen == AppScreen.ADD) openAddScreen(type = TransactionType.EXPENSE) else currentScreen = screen
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
                    ) { screen ->
                        when (screen) {
                            AppScreen.HOME -> HomeScreen(
                                uiState = uiState,
                                onToggleBalanceVisibility = { viewModel.toggleBalanceVisibility() },
                                onQuickExpense = { openAddScreen(type = TransactionType.EXPENSE) },
                                onQuickIncome = { openAddScreen(type = TransactionType.INCOME) },
                                onQuickTransfer = { openAddScreen(type = TransactionType.TRANSFER) },
                                onEditTransaction = { editTransaction(it) },
                                onDeleteTransaction = { viewModel.deleteTransaction(it) },
                                onNavigateToActivity = { currentScreen = AppScreen.ACTIVITY },
                                onNavigateToPlan = { currentScreen = AppScreen.PLAN },
                                onOpenUdhaar = { openHelper(AppScreen.UDHAAR) },
                                onOpenCommittee = { openHelper(AppScreen.COMMITTEE) },
                                onOpenWishList = { openHelper(AppScreen.WISHLIST) },
                                onOpenSettings = { showSettings = true },
                                onOpenDeleteData = { showDeleteData = true },
                                onPayBill = { viewModel.markBillAsPaid(it) },
                                onPayCommittee = { viewModel.payCommitteeMonth(it) },
                                onCommitteePayout = { viewModel.receiveCommitteePayout(it) },
                                onBuyWish = { viewModel.buyWish(it) },
                                onDropWish = { viewModel.dropWish(it) }
                            )

                            AppScreen.ACTIVITY -> ActivityScreen(
                                uiState = uiState,
                                viewModel = viewModel,
                                onQuickExpense = { openAddScreen(type = TransactionType.EXPENSE) },
                                onQuickIncome = { openAddScreen(type = TransactionType.INCOME) },
                                onEditTransaction = { editTransaction(it) },
                                onDeleteTransaction = { viewModel.deleteTransaction(it) },
                                onSeedRandomData = {},
                                onClearAllData = { viewModel.deleteAllTransactions() }
                            )

                            AppScreen.UNDERSTAND -> InsightsScreen(uiState = uiState, viewModel = viewModel)

                            AppScreen.ADD -> AddTransactionScreen(
                                viewModel = viewModel,
                                editingTransaction = editingTransaction,
                                prefilledTitle = prefilledTitle,
                                prefilledAmount = prefilledAmount,
                                prefilledCategory = prefilledCategory,
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

                            AppScreen.PLAN -> PlanScreen(
                                uiState = uiState,
                                viewModel = viewModel,
                                onOpenCanBuy = { openHelper(AppScreen.CANBUY) },
                                onOpenWishList = { openHelper(AppScreen.WISHLIST) },
                                onOpenUdhaar = { openHelper(AppScreen.UDHAAR) },
                                onOpenCommittee = { openHelper(AppScreen.COMMITTEE) }
                            )

                            AppScreen.CANBUY -> CanBuyScreen(uiState, viewModel, onBack = { currentScreen = returnTo })
                            AppScreen.WISHLIST -> WishListScreen(uiState, viewModel, onBack = { currentScreen = returnTo })
                            AppScreen.UDHAAR -> UdhaarScreen(uiState, viewModel, onBack = { currentScreen = returnTo })
                            AppScreen.COMMITTEE -> CommitteeScreen(uiState, viewModel, onBack = { currentScreen = returnTo })
                        }
                    }
                }
            }
        }

        if (showSettings) {
            SettingsSheet(
                profile = profile,
                onChange = { viewModel.updateProfile(it) },
                onEditAnswers = { editingAnswers = true },
                onAskNotificationPermission = askNotificationPermission,
                onDismiss = { showSettings = false }
            )
        }
        if (showDeleteData) {
            DeleteDataSheet(
                onDeleteMonth = { y, m -> viewModel.deleteMonth(y, m) },
                onDeleteAllRecords = { viewModel.deleteAllTransactions() },
                onDeleteEverything = {
                    viewModel.deleteEverything()
                    currentScreen = AppScreen.HOME
                },
                onDismiss = { showDeleteData = false }
            )
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
 * Floating navigation bar. The selected tab grows into a pill that shows its name;
 * the centre button is the thing you do most: write down what you spent.
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
                label = "History",
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
                    contentDescription = "Add",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            NavItem(
                label = "Charts",
                selected = currentScreen == AppScreen.UNDERSTAND,
                selectedIcon = Icons.Filled.PieChart,
                unselectedIcon = Icons.Outlined.PieChart,
                testTag = "nav_understand",
                onClick = { onNavigate(AppScreen.UNDERSTAND) }
            )
            NavItem(
                label = "Plan",
                selected = currentScreen == AppScreen.PLAN || currentScreen.isHelper,
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
