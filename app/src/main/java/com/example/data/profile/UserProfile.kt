package com.example.data.profile

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What the person mainly wants from the app. It decides what the Home screen shows first. */
enum class Goal(val title: String, val subtitle: String) {
    TRACK("See where my money goes", "Know what I spend on"),
    SAVE("Save money", "Put some money away every month"),
    CONTROL("Spend less", "Stop running out of money"),
    BILLS("Never miss a bill", "Pay rent, bills and committee on time")
}

enum class IncomeType(val title: String, val subtitle: String) {
    MONTHLY("Every month", "Salary or a fixed amount"),
    DAILY("Every day or week", "Daily wage or weekly pay"),
    IRREGULAR("It changes", "Business, freelance or odd jobs")
}

/** Answers to the first-run questions plus a few settings. Stored on the phone only. */
data class UserProfile(
    val onboardingDone: Boolean = false,
    val name: String = "",
    val goal: Goal = Goal.TRACK,
    val incomeType: IncomeType = IncomeType.MONTHLY,
    /** About how much money comes in each month. */
    val monthlyIncome: Double = 0.0,
    /** Percent of income to keep aside as savings. */
    val savePercent: Int = 10,
    /** Rent and other bills paid every month. */
    val monthlyBills: Double = 0.0,
    /** Day of the month the bills are paid (1 to 28). */
    val billsDay: Int = 1,
    /** Category names the person wants a spending limit for. */
    val focusCategories: Set<String> = emptySet(),
    val hoursPerDay: Int = 8,
    val daysPerMonth: Int = 26,
    val budgetAlerts: Boolean = true,
    val billReminders: Boolean = true,
    val dailyReminder: Boolean = false
) {
    /** What one hour of the person's work is worth, or 0 if income is not known. */
    val hourlyRate: Double
        get() {
            val hours = (hoursPerDay * daysPerMonth).coerceAtLeast(1)
            return if (monthlyIncome > 0) monthlyIncome / hours else 0.0
        }
}

/** Reads and writes [UserProfile]. Pass `null` for an in-memory store (used in tests). */
class ProfileStore(private val prefs: SharedPreferences?) {

    private val _profile = MutableStateFlow(load())
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    fun save(profile: UserProfile) {
        _profile.value = profile
        prefs?.edit()
            ?.putBoolean(K_DONE, profile.onboardingDone)
            ?.putString(K_NAME, profile.name)
            ?.putString(K_GOAL, profile.goal.name)
            ?.putString(K_INCOME_TYPE, profile.incomeType.name)
            ?.putFloat(K_INCOME, profile.monthlyIncome.toFloat())
            ?.putInt(K_SAVE, profile.savePercent)
            ?.putFloat(K_BILLS, profile.monthlyBills.toFloat())
            ?.putInt(K_BILLS_DAY, profile.billsDay)
            ?.putStringSet(K_FOCUS, profile.focusCategories)
            ?.putInt(K_HOURS, profile.hoursPerDay)
            ?.putInt(K_DAYS, profile.daysPerMonth)
            ?.putBoolean(K_ALERTS, profile.budgetAlerts)
            ?.putBoolean(K_BILL_REMINDERS, profile.billReminders)
            ?.putBoolean(K_DAILY, profile.dailyReminder)
            ?.apply()
    }

    fun update(change: (UserProfile) -> UserProfile) = save(change(_profile.value))

    fun reset() {
        prefs?.edit()?.clear()?.apply()
        _profile.value = UserProfile()
    }

    private fun load(): UserProfile {
        val p = prefs ?: return UserProfile()
        return UserProfile(
            onboardingDone = p.getBoolean(K_DONE, false),
            name = p.getString(K_NAME, "") ?: "",
            goal = runCatching { Goal.valueOf(p.getString(K_GOAL, Goal.TRACK.name)!!) }.getOrDefault(Goal.TRACK),
            incomeType = runCatching { IncomeType.valueOf(p.getString(K_INCOME_TYPE, IncomeType.MONTHLY.name)!!) }
                .getOrDefault(IncomeType.MONTHLY),
            monthlyIncome = p.getFloat(K_INCOME, 0f).toDouble(),
            savePercent = p.getInt(K_SAVE, 10),
            monthlyBills = p.getFloat(K_BILLS, 0f).toDouble(),
            billsDay = p.getInt(K_BILLS_DAY, 1),
            focusCategories = p.getStringSet(K_FOCUS, emptySet())?.toSet() ?: emptySet(),
            hoursPerDay = p.getInt(K_HOURS, 8),
            daysPerMonth = p.getInt(K_DAYS, 26),
            budgetAlerts = p.getBoolean(K_ALERTS, true),
            billReminders = p.getBoolean(K_BILL_REMINDERS, true),
            dailyReminder = p.getBoolean(K_DAILY, false)
        )
    }

    companion object {
        private const val K_DONE = "done"
        private const val K_NAME = "name"
        private const val K_GOAL = "goal"
        private const val K_INCOME_TYPE = "income_type"
        private const val K_INCOME = "income"
        private const val K_SAVE = "save_percent"
        private const val K_BILLS = "bills"
        private const val K_BILLS_DAY = "bills_day"
        private const val K_FOCUS = "focus"
        private const val K_HOURS = "hours"
        private const val K_DAYS = "days"
        private const val K_ALERTS = "budget_alerts"
        private const val K_BILL_REMINDERS = "bill_reminders"
        private const val K_DAILY = "daily_reminder"

        fun memory() = ProfileStore(null)

        fun from(context: Context) =
            ProfileStore(context.applicationContext.getSharedPreferences("kharch_profile", Context.MODE_PRIVATE))
    }
}
