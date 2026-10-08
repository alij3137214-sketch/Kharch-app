package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.data.model.BillReminderEntity
import com.example.data.model.BudgetEntity
import com.example.data.model.TransactionEntity
import com.example.ui.components.KharchSplash
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.KharchTheme
import com.example.ui.viewmodel.KharchUiState
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Calendar

/** Renders screens to PNG so the design can be reviewed without a device. Output: build/preview/. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi", sdk = [36])
class DesignPreviewTest {

    @get:Rule val rule = createComposeRule()

    private fun ago(days: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -days)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }.timeInMillis

    private fun sampleState(): KharchUiState {
        val month = Calendar.getInstance().let { "${it.get(Calendar.YEAR)}-${"%02d".format(it.get(Calendar.MONTH) + 1)}" }
        return KharchUiState(
            transactions = listOf(
                TransactionEntity(id = 1, title = "Monal Terrace Dinner", amount = 5400.0, type = "EXPENSE", category = "Food", paymentMethod = "Card", timestamp = ago(0, 9, 5)),
                TransactionEntity(id = 2, title = "Total Parco Fuel", amount = 3500.0, type = "EXPENSE", category = "Transport", paymentMethod = "Cash", timestamp = ago(0, 8, 10)),
                TransactionEntity(id = 3, title = "Freelance payout", amount = 40000.0, type = "INCOME", category = "Freelance", paymentMethod = "Easypaisa", timestamp = ago(1, 16, 30)),
                TransactionEntity(id = 4, title = "Al-Fatah Supermarket", amount = 7250.0, type = "EXPENSE", category = "Shopping", paymentMethod = "Bank", timestamp = ago(1, 12, 0)),
                TransactionEntity(id = 5, title = "Primary salary", amount = 120000.0, type = "INCOME", category = "Salary", paymentMethod = "Bank", timestamp = ago(3, 10, 0))
            ),
            budgets = listOf(
                BudgetEntity(category = "OVERALL", monthlyLimit = 60000.0, monthYear = month, alertThresholdPercent = 80),
                BudgetEntity(category = "Food", monthlyLimit = 5000.0, monthYear = month, alertThresholdPercent = 80)
            ),
            billReminders = listOf(
                BillReminderEntity(title = "Electricity", amount = 7500.0, dueDate = System.currentTimeMillis() + 3 * 86_400_000L, category = "Bills", isPaid = false, paymentMethod = "Bank")
            )
        )
    }

    private fun homeContent(state: KharchUiState) {
        rule.setContent {
            KharchTheme {
                Box(Modifier.size(411.dp, 891.dp).background(MaterialTheme.colorScheme.background)) {
                    HomeScreen(
                        uiState = state,
                        onToggleBalanceVisibility = {},
                        onQuickExpense = {}, onQuickIncome = {}, onQuickTransfer = {}, onQuickScanReceipt = {},
                        onBillClick = {}, onEditTransaction = {}, onDeleteTransaction = {}
                    )
                }
            }
        }
    }

    @Test
    fun home_withData() {
        rule.mainClock.autoAdvance = false
        homeContent(sampleState())
        rule.mainClock.advanceTimeBy(2500)
        rule.onRoot().captureRoboImage("build/preview/home.png")
    }

    @Test
    fun home_empty() {
        rule.mainClock.autoAdvance = false
        homeContent(KharchUiState())
        rule.mainClock.advanceTimeBy(2500)
        rule.onRoot().captureRoboImage("build/preview/home_empty.png")
    }

    @Test
    fun navigation_bar() {
        rule.setContent {
            KharchTheme {
                Column(Modifier.size(411.dp, 220.dp).background(MaterialTheme.colorScheme.background)) {
                    KharchBottomNavigation(currentScreen = AppScreen.HOME, onNavigate = {})
                    KharchBottomNavigation(currentScreen = AppScreen.UNDERSTAND, onNavigate = {})
                }
            }
        }
        rule.mainClock.advanceTimeBy(800)
        rule.onRoot().captureRoboImage("build/preview/nav.png")
    }

    @Test
    fun splash() {
        rule.mainClock.autoAdvance = false
        rule.setContent { KharchTheme { Box(Modifier.size(411.dp, 891.dp)) { KharchSplash(onFinished = {}) } } }
        rule.mainClock.advanceTimeBy(900)
        rule.onRoot().captureRoboImage("build/preview/splash.png")
    }
}
