package com.example.domain

import com.example.ui.viewmodel.KharchUiState
import java.util.Locale

/** A short message to show the person (also used for the phone notification). */
data class BudgetAlert(val title: String, val text: String)

/**
 * Works out which alerts a new expense should cause. An alert only fires when the expense
 * crosses a line (reaches the warning level, or goes over the limit), so the same message
 * is not repeated for every later expense.
 */
object BudgetAlertLogic {

    fun alertsFor(state: KharchUiState, category: String, amount: Double): List<BudgetAlert> {
        if (amount <= 0) return emptyList()
        val alerts = mutableListOf<BudgetAlert>()

        // 1. The limit for this kind of spending
        state.categoryBudgetStatuses.firstOrNull { it.category.equals(category, ignoreCase = true) }?.let { s ->
            val limit = s.monthlyLimit
            if (limit > 0) {
                val before = s.currentSpent
                val after = before + amount
                val warnAt = limit * s.alertThresholdPercent.coerceIn(50, 100) / 100.0
                when {
                    before <= limit && after > limit ->
                        alerts += BudgetAlert(
                            "$category limit is over",
                            "You spent ${rs(after - limit)} more than your ${rs(limit)} limit for $category."
                        )
                    before < warnAt && after >= warnAt && after <= limit ->
                        alerts += BudgetAlert(
                            "$category is almost full",
                            "You used ${(after / limit * 100).toInt()}% of your $category limit. ${rs(limit - after)} is left."
                        )
                }
            }
        }

        // 2. The limit for the whole month (only when the person set one on purpose)
        state.overallMonthlyBudget?.let { overall ->
            val limit = overall.monthlyLimit
            val before = state.currentMonthTotalExpense
            val after = before + amount
            val threshold = overall.alertThresholdPercent.coerceIn(50, 100)
            val warnAt = limit * threshold / 100.0
            when {
                before <= limit && after > limit ->
                    alerts += BudgetAlert(
                        "Monthly limit is over",
                        "You spent ${rs(after - limit)} more than your ${rs(limit)} for this month."
                    )
                before < warnAt && after >= warnAt && after <= limit ->
                    alerts += BudgetAlert(
                        "This month is almost used up",
                        "You used ${(after / limit * 100).toInt()}% of this month's money. ${rs(limit - after)} is left."
                    )
            }
        }
        return alerts
    }

    private fun rs(amount: Double) = "Rs. " + String.format(Locale.getDefault(), "%,.0f", amount)
}
