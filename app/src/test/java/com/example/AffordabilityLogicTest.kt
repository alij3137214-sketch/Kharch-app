package com.example

import com.example.data.model.BillReminderEntity
import com.example.data.model.BudgetEntity
import com.example.data.model.ExpenseCategory
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.viewmodel.FixedCostItem
import com.example.ui.viewmodel.KharchUiState
import com.example.ui.viewmodel.SimVerdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class AffordabilityLogicTest {

    @Test
    fun testAffordabilityAnalysis_deductsRecurringFixedCostsFromBudgetBasis() {
        val income = listOf(
            TransactionEntity(
                id = 1,
                title = "Salary",
                amount = 100000.0,
                type = TransactionType.INCOME.name,
                category = "Salary",
                paymentMethod = "Bank",
                timestamp = System.currentTimeMillis()
            )
        )

        val bills = listOf(
            BillReminderEntity(
                id = 1,
                title = "House Rent",
                amount = 25000.0,
                category = "Rent",
                dueDate = System.currentTimeMillis() + 86400000L,
                isPaid = false
            ),
            BillReminderEntity(
                id = 2,
                title = "Internet Fiber",
                amount = 3000.0,
                category = "Utilities",
                dueDate = System.currentTimeMillis() + 86400000L,
                isPaid = false
            )
        )

        val variableExpense = listOf(
            TransactionEntity(
                id = 2,
                title = "Groceries",
                amount = 12000.0,
                type = TransactionType.EXPENSE.name,
                category = ExpenseCategory.FOOD.displayName,
                paymentMethod = "Card",
                timestamp = System.currentTimeMillis()
            )
        )

        val state = KharchUiState(
            transactions = income + variableExpense,
            billReminders = bills
        )

        val analysis = state.getAffordabilityAnalysis(basisType = "INCOME")

        // Inflow: 100,000
        // Fixed Costs: 25,000 + 3,000 = 28,000
        // Variable Spent: 12,000
        // Remaining Discretionary: 100,000 - 28,000 - 12,000 = 60,000
        assertEquals(100000.0, analysis.budgetBasis, 0.01)
        assertEquals(28000.0, analysis.totalRecurringFixedCosts, 0.01)
        assertEquals(12000.0, analysis.alreadySpentVariable, 0.01)
        assertEquals(60000.0, analysis.remainingDiscretionary, 0.01)
        assertFalse(analysis.isDeficit)
        assertTrue(analysis.safeDailySpend > 0.0)
    }

    @Test
    fun testAffordabilityAnalysis_withCustomBudgetBasis() {
        val bills = listOf(
            BillReminderEntity(
                id = 1,
                title = "Electricity",
                amount = 5000.0,
                category = "Utilities",
                dueDate = System.currentTimeMillis() + 86400000L,
                isPaid = false
            )
        )

        val state = KharchUiState(
            billReminders = bills,
            affordabilityBasisType = "CUSTOM",
            affordabilityCustomBasis = 50000.0
        )

        val analysis = state.affordabilityAnalysis

        assertEquals(50000.0, analysis.budgetBasis, 0.01)
        assertEquals(5000.0, analysis.totalRecurringFixedCosts, 0.01)
        assertEquals(45000.0, analysis.remainingDiscretionary, 0.01)
    }

    @Test
    fun testAffordabilityAnalysis_toggleFixedCostDisabled() {
        val bills = listOf(
            BillReminderEntity(
                id = 10,
                title = "Gym",
                amount = 4000.0,
                category = "Health",
                dueDate = System.currentTimeMillis() + 86400000L,
                isPaid = false
            ),
            BillReminderEntity(
                id = 20,
                title = "Netflix",
                amount = 1500.0,
                category = "Entertainment",
                dueDate = System.currentTimeMillis() + 86400000L,
                isPaid = false
            )
        )

        val state = KharchUiState(
            billReminders = bills,
            disabledFixedCostIds = setOf(20L), // Netflix disabled
            affordabilityBasisType = "CUSTOM",
            affordabilityCustomBasis = 30000.0
        )

        val analysis = state.affordabilityAnalysis
        // Total fixed costs should only include Gym (4000)
        assertEquals(4000.0, analysis.totalRecurringFixedCosts, 0.01)
        assertEquals(26000.0, analysis.remainingDiscretionary, 0.01)
    }

    @Test
    fun testPurchaseSimulation_comfortableVerdict() {
        val state = KharchUiState(
            affordabilityBasisType = "CUSTOM",
            affordabilityCustomBasis = 80000.0,
            billReminders = listOf(
                BillReminderEntity(id = 1, title = "Rent", amount = 20000.0, category = "Rent", dueDate = 0L, isPaid = false)
            )
        )
        val analysis = state.affordabilityAnalysis // 60,000 remaining

        // Testing a small purchase of Rs. 2,000
        val sim = state.simulatePurchaseAffordability(
            analysis = analysis,
            itemName = "Dining Out",
            price = 2000.0
        )

        assertEquals(SimVerdict.COMFORTABLE, sim.verdict)
        assertEquals(58000.0, sim.remainingAfter, 0.01)
        assertTrue(sim.dailyDropPercentage < 10f)
    }

    @Test
    fun testPurchaseSimulation_deficitWhenExceedingRemaining() {
        val state = KharchUiState(
            affordabilityBasisType = "CUSTOM",
            affordabilityCustomBasis = 50000.0,
            billReminders = listOf(
                BillReminderEntity(id = 1, title = "EMI", amount = 35000.0, category = "Loans", dueDate = 0L, isPaid = false)
            )
        )
        val analysis = state.affordabilityAnalysis // 15,000 remaining

        // Testing purchase of Rs. 25,000 (exceeds 15,000)
        val sim = state.simulatePurchaseAffordability(
            analysis = analysis,
            itemName = "Luxury Watch",
            price = 25000.0
        )

        assertEquals(SimVerdict.DEFICIT, sim.verdict)
        assertEquals(-10000.0, sim.remainingAfter, 0.01)
        assertEquals(0.0, sim.dailySpendAfter, 0.01)
        assertTrue(sim.verdictTitle.contains("Exceeds"))
    }

    @Test
    fun testPurchaseSimulation_multiMonthInstallmentReducesImpact() {
        val state = KharchUiState(
            affordabilityBasisType = "CUSTOM",
            affordabilityCustomBasis = 50000.0,
            billReminders = listOf(
                BillReminderEntity(id = 1, title = "EMI", amount = 20000.0, category = "Loans", dueDate = 0L, isPaid = false)
            )
        )
        val analysis = state.affordabilityAnalysis // 30,000 remaining

        // Upfront 36,000 would cause a deficit (30,000 - 36,000 = -6,000)
        val upfrontSim = state.simulatePurchaseAffordability(
            analysis = analysis,
            itemName = "Laptop",
            price = 36000.0,
            tenureMonths = 1
        )
        assertEquals(SimVerdict.DEFICIT, upfrontSim.verdict)

        // Split across 3 months -> 12,000/month impact (30,000 - 12,000 = 18,000 remaining)
        val installmentSim = state.simulatePurchaseAffordability(
            analysis = analysis,
            itemName = "Laptop",
            price = 36000.0,
            tenureMonths = 3
        )
        assertEquals(12000.0, installmentSim.monthlyCost, 0.01)
        assertEquals(18000.0, installmentSim.remainingAfter, 0.01)
        assertTrue(installmentSim.verdict != SimVerdict.DEFICIT)
    }
}
