package com.example

import com.example.data.model.BudgetEntity
import com.example.data.model.ExpenseCategory
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.viewmodel.BudgetAlertStatus
import com.example.ui.viewmodel.KharchUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Locale

class BudgetLogicTest {

    private fun getCurrentMonthYear(): String {
        val cal = Calendar.getInstance()
        return String.format(Locale.US, "%d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    @Test
    fun testBudgetStatus_safe_whenBelowThreshold() {
        val monthYear = getCurrentMonthYear()
        val budget = BudgetEntity(
            id = 1,
            category = ExpenseCategory.FOOD.displayName,
            monthlyLimit = 10000.0,
            monthYear = monthYear,
            alertThresholdPercent = 80
        )
        val transactions = listOf(
            TransactionEntity(
                id = 1,
                title = "Groceries",
                amount = 5000.0,
                type = TransactionType.EXPENSE.name,
                category = ExpenseCategory.FOOD.displayName,
                paymentMethod = "Cash",
                timestamp = System.currentTimeMillis()
            )
        )

        val state = KharchUiState(
            budgets = listOf(budget),
            transactions = transactions
        )

        val status = state.categoryBudgetStatuses.find { it.category == ExpenseCategory.FOOD.displayName }
        assertNotNull(status)
        assertEquals(BudgetAlertStatus.SAFE, status?.alertStatus)
        assertEquals(5000.0, status?.currentSpent ?: 0.0, 0.01)
        assertEquals(5000.0, status?.remaining ?: 0.0, 0.01)
        assertEquals(0.5f, status?.spentPercentage ?: 0f, 0.01f)
        assertEquals(0.0, status?.overAmount ?: 0.0, 0.01)
    }

    @Test
    fun testBudgetStatus_warning_whenReachingThreshold() {
        val monthYear = getCurrentMonthYear()
        val budget = BudgetEntity(
            id = 2,
            category = ExpenseCategory.SHOPPING.displayName,
            monthlyLimit = 10000.0,
            monthYear = monthYear,
            alertThresholdPercent = 80
        )
        val transactions = listOf(
            TransactionEntity(
                id = 2,
                title = "Clothes",
                amount = 8500.0,
                type = TransactionType.EXPENSE.name,
                category = ExpenseCategory.SHOPPING.displayName,
                paymentMethod = "Cash",
                timestamp = System.currentTimeMillis()
            )
        )

        val state = KharchUiState(
            budgets = listOf(budget),
            transactions = transactions
        )

        val status = state.categoryBudgetStatuses.find { it.category == ExpenseCategory.SHOPPING.displayName }
        assertNotNull(status)
        assertEquals(BudgetAlertStatus.WARNING, status?.alertStatus)
        assertEquals(8500.0, status?.currentSpent ?: 0.0, 0.01)
        assertEquals(1500.0, status?.remaining ?: 0.0, 0.01)
        assertEquals(0.85f, status?.spentPercentage ?: 0f, 0.01f)
        assertTrue(state.warningBudgets.any { it.category == ExpenseCategory.SHOPPING.displayName })
        assertTrue(state.hasBudgetAlerts)
    }

    @Test
    fun testBudgetStatus_exceeded_whenOverMonthlyLimit() {
        val monthYear = getCurrentMonthYear()
        val budget = BudgetEntity(
            id = 3,
            category = ExpenseCategory.ENTERTAINMENT.displayName,
            monthlyLimit = 5000.0,
            monthYear = monthYear,
            alertThresholdPercent = 80
        )
        val transactions = listOf(
            TransactionEntity(
                id = 3,
                title = "Concert",
                amount = 6200.0,
                type = TransactionType.EXPENSE.name,
                category = ExpenseCategory.ENTERTAINMENT.displayName,
                paymentMethod = "Cash",
                timestamp = System.currentTimeMillis()
            )
        )

        val state = KharchUiState(
            budgets = listOf(budget),
            transactions = transactions
        )

        val status = state.categoryBudgetStatuses.find { it.category == ExpenseCategory.ENTERTAINMENT.displayName }
        assertNotNull(status)
        assertEquals(BudgetAlertStatus.EXCEEDED, status?.alertStatus)
        assertEquals(6200.0, status?.currentSpent ?: 0.0, 0.01)
        assertEquals(0.0, status?.remaining ?: 0.0, 0.01)
        assertEquals(1200.0, status?.overAmount ?: 0.0, 0.01)
        assertTrue(state.exceededBudgets.any { it.category == ExpenseCategory.ENTERTAINMENT.displayName })
        assertTrue(state.hasBudgetAlerts)
    }

    @Test
    fun testBudgetImpact_realTimeCalculation() {
        val monthYear = getCurrentMonthYear()
        val budget = BudgetEntity(
            id = 4,
            category = ExpenseCategory.FOOD.displayName,
            monthlyLimit = 10000.0,
            monthYear = monthYear,
            alertThresholdPercent = 80
        )
        val transactions = listOf(
            TransactionEntity(
                id = 4,
                title = "Lunch",
                amount = 7500.0,
                type = TransactionType.EXPENSE.name,
                category = ExpenseCategory.FOOD.displayName,
                paymentMethod = "Cash",
                timestamp = System.currentTimeMillis()
            )
        )

        val state = KharchUiState(
            budgets = listOf(budget),
            transactions = transactions
        )

        // Adding 1000: Total 8500 -> Warning reached
        val impactWarning = state.getBudgetImpact(ExpenseCategory.FOOD.displayName, 1000.0)
        assertNotNull(impactWarning)
        assertTrue(impactWarning!!.willReachWarning)
        assertFalse(impactWarning.willExceed)
        assertEquals(8500.0, impactWarning.projectedSpent, 0.01)

        // Adding 3000: Total 10500 -> Exceeded!
        val impactExceed = state.getBudgetImpact(ExpenseCategory.FOOD.displayName, 3000.0)
        assertNotNull(impactExceed)
        assertTrue(impactExceed!!.willExceed)
        assertEquals(10500.0, impactExceed.projectedSpent, 0.01)
        assertEquals(500.0, impactExceed.excessAmount, 0.01)
    }
}
