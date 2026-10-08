package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.ExpenseCategory
import com.example.data.profile.BudgetPlanner
import com.example.data.profile.Goal
import com.example.data.profile.IncomeType
import com.example.data.profile.UserProfile
import com.example.ui.components.MoneyField
import com.example.ui.components.PickChip
import com.example.ui.components.PrimaryButton
import com.example.ui.components.ScreenPadding
import com.example.ui.components.SoftButton
import com.example.ui.components.TextBox
import com.example.ui.components.formatRs
import com.example.ui.components.pressable
import com.example.ui.theme.EmeraldPrimary

private enum class Step { NAME, GOAL, INCOME_TYPE, INCOME, SAVE, BILLS, CATEGORIES, WORK, REMINDERS, PLAN }

private val ProfileSaver = listSaver<UserProfile, Any>(
    save = {
        listOf(
            it.name, it.goal.name, it.incomeType.name, it.monthlyIncome, it.savePercent, it.monthlyBills,
            it.billsDay, it.focusCategories.joinToString("|"), it.hoursPerDay, it.daysPerMonth,
            it.budgetAlerts, it.billReminders, it.dailyReminder
        )
    },
    restore = {
        UserProfile(
            name = it[0] as String,
            goal = Goal.valueOf(it[1] as String),
            incomeType = IncomeType.valueOf(it[2] as String),
            monthlyIncome = it[3] as Double,
            savePercent = it[4] as Int,
            monthlyBills = it[5] as Double,
            billsDay = it[6] as Int,
            focusCategories = (it[7] as String).split("|").filter { s -> s.isNotEmpty() }.toSet(),
            hoursPerDay = it[8] as Int,
            daysPerMonth = it[9] as Int,
            budgetAlerts = it[10] as Boolean,
            billReminders = it[11] as Boolean,
            dailyReminder = it[12] as Boolean
        )
    }
)

private fun Double.asText(): String = if (this <= 0) "" else if (this % 1 == 0.0) toLong().toString() else toString()

/**
 * The first-run questions. The answers decide the monthly limit, the category limits
 * and what the Home screen shows first. Also used to change the answers later.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    initial: UserProfile,
    isEditing: Boolean,
    onFinish: (UserProfile) -> Unit,
    onAskNotificationPermission: () -> Unit,
    onClose: (() -> Unit)? = null
) {
    var draft by rememberSaveable(stateSaver = ProfileSaver) { mutableStateOf(initial) }
    var incomeText by rememberSaveable { mutableStateOf(initial.monthlyIncome.asText()) }
    var billsText by rememberSaveable { mutableStateOf(initial.monthlyBills.asText()) }
    var hasBills by rememberSaveable { mutableStateOf(initial.monthlyBills > 0) }
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }

    val income = incomeText.toDoubleOrNull() ?: 0.0
    val steps = buildList {
        add(Step.NAME); add(Step.GOAL); add(Step.INCOME_TYPE); add(Step.INCOME)
        if (income > 0) add(Step.SAVE)
        add(Step.BILLS); add(Step.CATEGORIES)
        if (income > 0) add(Step.WORK)
        add(Step.REMINDERS); add(Step.PLAN)
    }
    val index = stepIndex.coerceIn(0, steps.lastIndex)
    val step = steps[index]
    val working = draft.copy(
        monthlyIncome = income,
        monthlyBills = if (hasBills) (billsText.toDoubleOrNull() ?: 0.0) else 0.0
    )

    fun back() {
        if (index > 0) stepIndex = index - 1 else onClose?.invoke()
    }
    fun next() {
        if (step == Step.REMINDERS && (working.budgetAlerts || working.billReminders || working.dailyReminder)) {
            onAskNotificationPermission()
        }
        if (step == Step.PLAN) onFinish(working) else stepIndex = index + 1
    }
    BackHandler(enabled = index > 0 || (isEditing && onClose != null)) { back() }

    val progress by animateFloatAsState(
        targetValue = (index + 1f) / steps.size,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "onboarding_progress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .testTag("onboarding_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (index > 0 || (isEditing && onClose != null)) {
                IconButton(onClick = { back() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Spacer(Modifier.size(48.dp))
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(EmeraldPrimary)
                )
            }
            Spacer(Modifier.size(width = 24.dp, height = 48.dp))
        }

        AnimatedContent(
            targetState = step,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                (slideInHorizontally(tween(320)) { if (forward) it / 4 else -it / 4 } + fadeIn(tween(280))) togetherWith
                    (slideOutHorizontally(tween(240)) { if (forward) -it / 4 else it / 4 } + fadeOut(tween(160)))
            },
            label = "onboarding_step"
        ) { s ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ScreenPadding, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (s) {
                    Step.NAME -> {
                        Question("Hello! What should we call you?", "This is only to say hi. You can skip it.")
                        Spacer(Modifier.height(8.dp))
                        TextBox(value = draft.name, onValueChange = { draft = draft.copy(name = it.take(24)) }, label = "Your name")
                    }

                    Step.GOAL -> {
                        Question("What do you want help with?", "Pick the one that matters most. Your home screen will be made for it.")
                        Goal.entries.forEach { g ->
                            OptionCard(
                                title = g.title,
                                subtitle = g.subtitle,
                                icon = when (g) {
                                    Goal.TRACK -> Icons.Default.PieChart
                                    Goal.SAVE -> Icons.Default.Savings
                                    Goal.CONTROL -> Icons.AutoMirrored.Filled.TrendingDown
                                    Goal.BILLS -> Icons.Default.NotificationsActive
                                },
                                selected = draft.goal == g,
                                onClick = { draft = draft.copy(goal = g) }
                            )
                        }
                    }

                    Step.INCOME_TYPE -> {
                        Question("When does money come to you?", "This tells us how to show your days and your limit.")
                        IncomeType.entries.forEach { t ->
                            OptionCard(
                                title = t.title,
                                subtitle = t.subtitle,
                                icon = when (t) {
                                    IncomeType.MONTHLY -> Icons.Default.CalendarMonth
                                    IncomeType.DAILY -> Icons.Default.Today
                                    IncomeType.IRREGULAR -> Icons.Default.Shuffle
                                },
                                selected = draft.incomeType == t,
                                onClick = { draft = draft.copy(incomeType = t) }
                            )
                        }
                    }

                    Step.INCOME -> {
                        Question("About how much money do you get in one month?", "A rough number is fine. You can change it any time. Leave it empty if you would rather not say.")
                        Spacer(Modifier.height(8.dp))
                        MoneyField(value = incomeText, onValueChange = { incomeText = it }, label = "Money in each month")
                    }

                    Step.SAVE -> {
                        Question("How much do you want to keep aside?", "We take this out first, so it is safe.")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0, 5, 10, 15, 20, 30).forEach { p ->
                                PickChip(label = "$p%", selected = draft.savePercent == p, onClick = { draft = draft.copy(savePercent = p) })
                            }
                        }
                        val saved = income * draft.savePercent / 100.0
                        Text(
                            text = if (draft.savePercent == 0) "Nothing is kept aside." else "That is ${formatRs(saved)} every month.",
                            style = MaterialTheme.typography.titleMedium,
                            color = EmeraldPrimary
                        )
                    }

                    Step.BILLS -> {
                        Question("Do you pay rent or bills every month?", "Like rent, electricity, internet or school fees.")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PickChip(label = "Yes", selected = hasBills, onClick = { hasBills = true })
                            PickChip(label = "No", selected = !hasBills, onClick = { hasBills = false })
                        }
                        if (hasBills) {
                            Spacer(Modifier.height(4.dp))
                            MoneyField(value = billsText, onValueChange = { billsText = it }, label = "All bills together, each month")
                            Text("Which day of the month?", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(1, 5, 10, 15, 20, 25).forEach { d ->
                                    PickChip(label = "$d", selected = draft.billsDay == d, onClick = { draft = draft.copy(billsDay = d) })
                                }
                            }
                        }
                    }

                    Step.CATEGORIES -> {
                        Question("What do you spend on most?", "Pick a few. We will give each one a limit.")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ExpenseCategory.entries.filter { it != ExpenseCategory.BILLS }.forEach { c ->
                                val on = c.displayName in draft.focusCategories
                                PickChip(
                                    label = c.displayName,
                                    icon = c.icon(),
                                    color = c.color,
                                    selected = on,
                                    onClick = {
                                        val set = if (on) draft.focusCategories - c.displayName else draft.focusCategories + c.displayName
                                        draft = draft.copy(focusCategories = set)
                                    }
                                )
                            }
                        }
                    }

                    Step.WORK -> {
                        Question("How many hours do you work in a day?", "We use this to show prices as hours of work. It makes you think twice. You can skip it.")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(4, 6, 8, 10, 12).forEach { h ->
                                PickChip(label = "$h hours", selected = draft.hoursPerDay == h, onClick = { draft = draft.copy(hoursPerDay = h) })
                            }
                        }
                        Text("Days you work in a month", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(15, 20, 26, 30).forEach { d ->
                                PickChip(label = "$d days", selected = draft.daysPerMonth == d, onClick = { draft = draft.copy(daysPerMonth = d) })
                            }
                        }
                    }

                    Step.REMINDERS -> {
                        Question("Do you want reminders?", "They come on your phone. You can turn them off in Settings.")
                        SwitchRow("Tell me when I spend too much", draft.budgetAlerts) { draft = draft.copy(budgetAlerts = it) }
                        SwitchRow("Remind me before bills and committee are due", draft.billReminders) { draft = draft.copy(billReminders = it) }
                        SwitchRow("Remind me in the evening to write today's spending", draft.dailyReminder) { draft = draft.copy(dailyReminder = it) }
                    }

                    Step.PLAN -> PlanSummary(working)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ScreenPadding, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val canGo = when (step) {
                Step.BILLS -> !hasBills || (billsText.toDoubleOrNull() ?: 0.0) > 0
                else -> true
            }
            PrimaryButton(
                text = when (step) {
                    Step.PLAN -> if (isEditing) "Save" else "Start"
                    Step.NAME -> if (draft.name.isBlank()) "Skip" else "Next"
                    Step.INCOME -> if (income > 0) "Next" else "Skip"
                    Step.WORK -> "Next"
                    else -> "Next"
                },
                onClick = { next() },
                enabled = canGo,
                modifier = Modifier.testTag("onboarding_next")
            )
            if (step == Step.WORK) {
                SoftButton("Skip this", onClick = { stepIndex = index + 1 })
            }
        }
    }
}

@Composable
private fun Question(title: String, hint: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(hint, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun OptionCard(title: String, subtitle: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    val bg by animateColorAsState(
        if (selected) EmeraldPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        tween(200), label = "opt_bg"
    )
    val border by animateColorAsState(
        if (selected) EmeraldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
        tween(200), label = "opt_border"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressable(pressedScale = 0.98f, onClick = onClick)
            .clip(shape)
            .background(bg)
            .border(if (selected) 1.5.dp else 1.dp, border, shape)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(EmeraldPrimary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SwitchRow(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), shape)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = EmeraldPrimary, checkedThumbColor = MaterialTheme.colorScheme.onPrimary)
        )
    }
}

@Composable
private fun PlanSummary(profile: UserProfile) {
    val plan = BudgetPlanner.suggest(profile)
    val hello = if (profile.name.isBlank()) "Your plan is ready" else "${profile.name}, your plan is ready"
    Question(hello, "Made from your answers. You can change everything later.")
    if (profile.monthlyIncome <= 0) {
        Text(
            "You did not tell us your money. That is fine. Just start adding what you spend, and set a limit in the Plan tab when you want.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        return
    }
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), shape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PlanRow("You can spend this month", formatRs(plan.monthlyLimit), strong = true)
        PlanRow("Kept aside for savings", formatRs(plan.savingsPerMonth))
        if (plan.billsPerMonth > 0) PlanRow("For bills", formatRs(plan.billsPerMonth))
        PlanRow("For everyday life", formatRs(plan.everydayMoney))
    }
    if (plan.categoryLimits.isNotEmpty()) {
        Text("Limits for each month", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), shape)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            plan.categoryLimits.forEach { (c, l) -> PlanRow(c, formatRs(l)) }
        }
    }
}

@Composable
private fun PlanRow(label: String, value: String, strong: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = if (strong) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            color = if (strong) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = if (strong) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
            color = if (strong) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
        )
    }
}
