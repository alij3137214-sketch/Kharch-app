package com.example.data.profile

import kotlin.math.roundToLong

/** The plan the app suggests from the first-run answers. */
data class PlanSuggestion(
    /** How much can be spent in a month (income minus savings). */
    val monthlyLimit: Double,
    val savingsPerMonth: Double,
    val billsPerMonth: Double,
    /** What is left for daily life after bills and savings. */
    val everydayMoney: Double,
    val categoryLimits: Map<String, Double>
)

/** Simple rules, no guessing. Same answers always give the same plan. */
object BudgetPlanner {

    /** How big a share of everyday money each category usually needs. */
    private val weights = mapOf(
        "Food" to 0.35,
        "Transport" to 0.15,
        "Shopping" to 0.10,
        "Education" to 0.10,
        "Health" to 0.07,
        "Entertainment" to 0.08,
        "Clothing" to 0.05,
        "Travel" to 0.05,
        "Technology" to 0.05,
        "Charity" to 0.03,
        "Other" to 0.05
    )

    val defaultFocus = setOf("Food", "Transport", "Shopping")

    fun suggest(profile: UserProfile): PlanSuggestion {
        val income = profile.monthlyIncome.coerceAtLeast(0.0)
        val savings = income * profile.savePercent.coerceIn(0, 80) / 100.0
        val bills = profile.monthlyBills.coerceAtLeast(0.0)
        val monthlyLimit = (income - savings).coerceAtLeast(0.0)
        val everyday = (monthlyLimit - bills).coerceAtLeast(0.0)

        val focus = profile.focusCategories.filter { it in weights }.ifEmpty { defaultFocus.toList() }
        val totalWeight = focus.sumOf { weights.getValue(it) }
        // Keep 10% of everyday money free so one small surprise does not break the plan.
        val limits = focus.associateWith { category ->
            roundToHundred(everyday * 0.9 * weights.getValue(category) / totalWeight)
        }.filterValues { it > 0 }

        return PlanSuggestion(
            monthlyLimit = roundToHundred(monthlyLimit),
            savingsPerMonth = roundToHundred(savings),
            billsPerMonth = bills,
            everydayMoney = roundToHundred(everyday),
            categoryLimits = limits
        )
    }

    private fun roundToHundred(value: Double): Double = (value / 100.0).roundToLong() * 100.0
}
