package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.CommitteeEntity
import com.example.data.model.DebtDirection
import com.example.data.model.DebtEntity
import com.example.data.model.WishItemEntity
import com.example.data.model.WishStatus
import com.example.domain.MoneyMath
import com.example.ui.components.FormSheet
import com.example.ui.components.IconBadge
import com.example.ui.components.KharchCard
import com.example.ui.components.MoneyField
import com.example.ui.components.PickChip
import com.example.ui.components.PrimaryButton
import com.example.ui.components.ScreenPadding
import com.example.ui.components.SoftButton
import com.example.ui.components.TextBox
import com.example.ui.components.entrance
import com.example.ui.components.formatRs
import com.example.ui.components.pressable
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.TransferBlue
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.KharchUiState
import com.example.ui.viewmodel.KharchViewModel
import com.example.ui.viewmodel.SimVerdict
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** A full page with a back arrow, used by the helpers. */
@Composable
fun SubScreen(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    content: LazyListScope.() -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = ScreenPadding, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("subscreen_back")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (actionLabel != null && onAction != null) {
                Text(
                    actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .pressable(pressedScale = 0.94f, onClick = onAction)
                        .clip(RoundedCornerShape(12.dp))
                        .background(EmeraldPrimary)
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                        .testTag("subscreen_action")
                )
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
private fun EmptyNote(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 24.dp)
    )
}

// ================================================================================================
// Can I buy it?
// ================================================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CanBuyScreen(uiState: KharchUiState, viewModel: KharchViewModel, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var months by remember { mutableStateOf(1) }
    val price = priceText.toDoubleOrNull() ?: 0.0
    var added by remember { mutableStateOf<String?>(null) }

    val sim = remember(uiState, price, months, name) {
        if (price > 0) viewModel.simulatePurchase(name, price, "Shopping", months) else null
    }

    SubScreen(title = "Can I buy it?", subtitle = "Check before you spend", onBack = onBack) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextBox(value = name, onValueChange = { name = it }, label = "What do you want to buy?", placeholder = "Shoes, phone, bike")
                MoneyField(value = priceText, onValueChange = { priceText = it; added = null }, label = "Price")
                Text("Pay it in", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1 to "One go", 3 to "3 months", 6 to "6 months", 12 to "12 months").forEach { (m, label) ->
                        PickChip(label, months == m, { months = m })
                    }
                }
            }
        }

        if (sim != null) {
            item {
                val (color, headline) = when (sim.verdict) {
                    SimVerdict.COMFORTABLE -> EmeraldPrimary to "Yes, you can buy it"
                    SimVerdict.MODERATE -> EmeraldPrimary to "Yes, but be careful"
                    SimVerdict.STRETCHED -> WarningAmber to "It will be tight"
                    SimVerdict.DEFICIT -> ExpenseRed to "Not now"
                }
                Box(Modifier.entrance(0)) {
                    KharchCard(borderColor = color.copy(alpha = 0.5f)) {
                        Text(headline, style = MaterialTheme.typography.headlineSmall, color = color)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (sim.remainingAfter >= 0) "After this you still have ${formatRs(sim.remainingAfter)} free this month."
                            else "You are short by ${formatRs(-sim.remainingAfter)} this month.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (months > 1) {
                            Spacer(Modifier.height(4.dp))
                            Text("Each month you pay ${formatRs(sim.monthlyCost)}.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (sim.remainingBefore > 0) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Your money for each day goes from ${formatRs(sim.dailySpendBefore)} to ${formatRs(sim.dailySpendAfter)}.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        MoneyMath.priceInWork(price, uiState.profile)?.let {
                            Spacer(Modifier.height(4.dp))
                            Text("That is $it.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryButton(
                        text = "Put it on my wait list",
                        onClick = {
                            viewModel.addWish(name, price)
                            added = "Added to your wait list."
                        }
                    )
                    if (sim.verdict == SimVerdict.DEFICIT || sim.verdict == SimVerdict.STRETCHED) {
                        SoftButton(
                            text = "Save for it instead",
                            color = EmeraldPrimary,
                            onClick = {
                                val by = Calendar.getInstance().apply { add(Calendar.MONTH, 6) }
                                viewModel.addSavingsGoal(name.ifBlank { "Something I want" }, price, SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(by.time))
                                added = "Goal made. Open the Plan tab to add money."
                            }
                        )
                    }
                    added?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = EmeraldPrimary) }
                }
            }
        }
    }
}

// ================================================================================================
// Wait list
// ================================================================================================

@Composable
fun WishListScreen(uiState: KharchUiState, viewModel: KharchViewModel, onBack: () -> Unit) {
    var showAdd by remember { mutableStateOf(false) }
    val waiting = uiState.wishItems.filter { it.status == WishStatus.WAITING.name }
    val dropped = uiState.wishItems.filter { it.status == WishStatus.DROPPED.name }
    val savedByWaiting = dropped.sumOf { it.price }

    SubScreen(
        title = "Wait list",
        subtitle = "Wait a few days before you buy",
        onBack = onBack,
        actionLabel = "Add",
        onAction = { showAdd = true }
    ) {
        if (savedByWaiting > 0) {
            item {
                KharchCard(contentPadding = 16.dp, borderColor = EmeraldPrimary.copy(alpha = 0.5f)) {
                    Text("You saved ${formatRs(savedByWaiting)} by waiting", style = MaterialTheme.typography.titleMedium, color = EmeraldPrimary)
                    Text("${dropped.size} ${if (dropped.size == 1) "thing" else "things"} you did not need after all.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (waiting.isEmpty()) {
            item { EmptyNote("Nothing is waiting. When you want something, add it here first. If you still want it after a few days, buy it.") }
        }
        items(waiting.size) { i ->
            val w = waiting[i]
            val left = MoneyMath.daysLeftToWait(w.addedAt, w.waitDays)
            KharchCard(contentPadding = 16.dp) {
                Text(w.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(formatRs(w.price), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                MoneyMath.priceInWork(w.price, uiState.profile)?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(10.dp))
                if (left > 0) {
                    Text("Wait $left more ${if (left == 1) "day" else "days"}", style = MaterialTheme.typography.titleSmall, color = WarningAmber)
                    Bar(1f - left.toFloat() / w.waitDays.coerceAtLeast(1), WarningAmber)
                } else {
                    Text("The waiting is over. Do you still want it?", style = MaterialTheme.typography.titleSmall, color = EmeraldPrimary)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Yes, buy it",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .pressable(pressedScale = 0.94f) { viewModel.buyWish(w) }
                                .clip(RoundedCornerShape(12.dp))
                                .background(EmeraldPrimary)
                                .padding(horizontal = 16.dp, vertical = 9.dp)
                        )
                        SoftButton("No, skip it", onClick = { viewModel.dropWish(w) })
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    SoftButton("Remove", onClick = { viewModel.deleteWish(w) })
                }
            }
        }
    }

    if (showAdd) AddWishSheet(uiState, onSave = { t, p -> viewModel.addWish(t, p); showAdd = false }, onDismiss = { showAdd = false })
}

@Composable
private fun AddWishSheet(uiState: KharchUiState, onSave: (String, Double) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    val value = price.toDoubleOrNull() ?: 0.0
    FormSheet(title = "What do you want?", subtitle = "We will make you wait first.", onDismiss = onDismiss) {
        TextBox(value = title, onValueChange = { title = it }, label = "Name", placeholder = "New headphones")
        MoneyField(value = price, onValueChange = { price = it }, label = "Price")
        if (value > 0) {
            val days = MoneyMath.waitDaysFor(value, uiState.profile.monthlyIncome)
            Text("You will wait $days ${if (days == 1) "day" else "days"} before you decide.", style = MaterialTheme.typography.titleSmall, color = WarningAmber)
        }
        PrimaryButton("Add to wait list", onClick = { onSave(title, value) }, enabled = value > 0)
    }
}

// ================================================================================================
// Udhaar
// ================================================================================================

@Composable
fun UdhaarScreen(uiState: KharchUiState, viewModel: KharchViewModel, onBack: () -> Unit) {
    var showAdd by remember { mutableStateOf(false) }
    var payTarget by remember { mutableStateOf<DebtEntity?>(null) }
    val context = LocalContext.current

    val open = uiState.debts.filter { !it.isSettled }
    val done = uiState.debts.filter { it.isSettled }
    val owedToMe = open.filter { it.direction == DebtDirection.LENT.name }.sumOf { it.remaining }
    val iOwe = open.filter { it.direction == DebtDirection.BORROWED.name }.sumOf { it.remaining }

    SubScreen(title = "Udhaar", subtitle = "Money given and money taken", onBack = onBack, actionLabel = "Add", onAction = { showAdd = true }) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TotalTile("People owe you", formatRs(owedToMe), EmeraldPrimary, Modifier.weight(1f))
                TotalTile("You owe", formatRs(iOwe), ExpenseRed, Modifier.weight(1f))
            }
        }
        if (open.isEmpty()) item { EmptyNote("Nobody owes anything. When you lend or borrow money, write it here so nobody forgets.") }
        items(open.size) { i ->
            val d = open[i]
            val lent = d.direction == DebtDirection.LENT.name
            KharchCard(contentPadding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(d.person, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = if (lent) "Owes you" else "You owe",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (lent) EmeraldPrimary else ExpenseRed
                        )
                    }
                    Text(formatRs(d.remaining), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                }
                if (d.paidAmount > 0) {
                    Spacer(Modifier.height(6.dp))
                    Text("${formatRs(d.paidAmount)} of ${formatRs(d.amount)} is done", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (d.note.isNotBlank()) {
                    Text(d.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (lent) "Got some back" else "I paid some",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .pressable(pressedScale = 0.94f) { payTarget = d }
                            .clip(RoundedCornerShape(12.dp))
                            .background(EmeraldPrimary)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                    if (lent) {
                        SoftButton("Remind", onClick = {
                            val text = "Hi ${d.person}, a small reminder about the ${formatRs(d.remaining)} I gave you. Please send it when you can. Thank you!"
                            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }, "Send reminder"))
                        })
                    }
                    Spacer(Modifier.weight(1f))
                    SoftButton("Delete", onClick = { viewModel.deleteDebt(d) })
                }
            }
        }
        if (done.isNotEmpty()) {
            item { Text("Finished", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp)) }
            items(done.size) { i ->
                val d = done[i]
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${d.person} · ${formatRs(d.amount)}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    SoftButton("Delete", onClick = { viewModel.deleteDebt(d) })
                }
            }
        }
    }

    if (showAdd) AddDebtSheet(onSave = { p, a, dir, n -> viewModel.addDebt(p, a, dir, n); showAdd = false }, onDismiss = { showAdd = false })
    payTarget?.let { d ->
        var amount by remember(d.id) { mutableStateOf("") }
        val value = amount.toDoubleOrNull() ?: 0.0
        FormSheet(
            title = if (d.direction == DebtDirection.LENT.name) "${d.person} gave back" else "You paid ${d.person}",
            subtitle = "${formatRs(d.remaining)} is left.",
            onDismiss = { payTarget = null }
        ) {
            MoneyField(value = amount, onValueChange = { amount = it }, label = "How much")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PickChip("All of it", false, { amount = d.remaining.toLong().toString() })
            }
            PrimaryButton("Save", onClick = { viewModel.payDebt(d, value); payTarget = null }, enabled = value > 0)
        }
    }
}

@Composable
private fun TotalTile(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    KharchCard(modifier = modifier, contentPadding = 14.dp) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, color = color)
    }
}

@Composable
private fun AddDebtSheet(onSave: (String, Double, String, String) -> Unit, onDismiss: () -> Unit) {
    var person by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var lent by remember { mutableStateOf(true) }
    var note by remember { mutableStateOf("") }
    val value = amount.toDoubleOrNull() ?: 0.0
    FormSheet(title = "New udhaar", onDismiss = onDismiss) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PickChip("I gave money", lent, { lent = true })
            PickChip("I took money", !lent, { lent = false }, color = ExpenseRed)
        }
        TextBox(value = person, onValueChange = { person = it }, label = "Person's name")
        MoneyField(value = amount, onValueChange = { amount = it }, label = "How much")
        TextBox(value = note, onValueChange = { note = it }, label = "Note (optional)")
        PrimaryButton(
            "Save",
            onClick = { onSave(person.ifBlank { "Someone" }, value, if (lent) DebtDirection.LENT.name else DebtDirection.BORROWED.name, note) },
            enabled = value > 0
        )
    }
}

// ================================================================================================
// Committee (kameti / BC)
// ================================================================================================

@Composable
fun CommitteeScreen(uiState: KharchUiState, viewModel: KharchViewModel, onBack: () -> Unit) {
    var showAdd by remember { mutableStateOf(false) }

    SubScreen(title = "Committee", subtitle = "Your kameti or BC", onBack = onBack, actionLabel = "Add", onAction = { showAdd = true }) {
        if (uiState.committees.isEmpty()) {
            item { EmptyNote("Are you in a committee? Add it here. We will tell you when to pay and when it is your turn to get the money.") }
        }
        items(uiState.committees.size) { i ->
            val c = uiState.committees[i]
            val s = MoneyMath.committeeStatus(c)
            KharchCard(contentPadding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(c.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    Text("${formatRs(c.monthlyAmount)} / month", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(8.dp))
                Text("You paid ${c.paidMonths} of ${c.totalMembers} months", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Bar(c.paidMonths.toFloat() / c.totalMembers, TransferBlue)
                Spacer(Modifier.height(10.dp))
                Text(
                    if (c.payoutReceived) "You already got ${formatRs(c.pot)}." else "You get ${formatRs(c.pot)} in ${s.payoutMonthLabel} (month ${c.myTurn}).",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (s.myTurnNow) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (s.paymentDue) {
                        Text(
                            "I paid this month",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .pressable(pressedScale = 0.94f) { viewModel.payCommitteeMonth(c) }
                                .clip(RoundedCornerShape(12.dp))
                                .background(EmeraldPrimary)
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                    if (s.myTurnNow) {
                        Text(
                            "I got the money",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .pressable(pressedScale = 0.94f) { viewModel.receiveCommitteePayout(c) }
                                .clip(RoundedCornerShape(12.dp))
                                .background(EmeraldPrimary)
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    SoftButton("Delete", onClick = { viewModel.deleteCommittee(c) })
                }
            }
        }
    }

    if (showAdd) AddCommitteeSheet(onSave = { n, a, m, t, start -> viewModel.addCommittee(n, a, m, t, start); showAdd = false }, onDismiss = { showAdd = false })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddCommitteeSheet(onSave: (String, Double, Int, Int, String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var members by remember { mutableStateOf(10) }
    var myTurn by remember { mutableStateOf(1) }
    var startNext by remember { mutableStateOf(false) }
    val value = amount.toDoubleOrNull() ?: 0.0
    FormSheet(title = "New committee", subtitle = "Everyone pays each month. One person gets all the money.", onDismiss = onDismiss) {
        TextBox(value = name, onValueChange = { name = it }, label = "Name", placeholder = "Office committee")
        MoneyField(value = amount, onValueChange = { amount = it }, label = "You pay each month")
        Text("How many people?", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(5, 6, 8, 10, 12, 15, 20).forEach { n ->
                PickChip("$n", members == n, { members = n; if (myTurn > n) myTurn = n })
            }
        }
        Text("In which month do you get the money?", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..members).forEach { t -> PickChip("$t", myTurn == t, { myTurn = t }) }
        }
        Text("When does it start?", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PickChip("This month", !startNext, { startNext = false })
            PickChip("Next month", startNext, { startNext = true })
        }
        if (value > 0) {
            Text("The money you will get: ${formatRs(value * members)}", style = MaterialTheme.typography.titleSmall, color = EmeraldPrimary)
        }
        PrimaryButton(
            "Save",
            onClick = {
                val start = Calendar.getInstance().apply { if (startNext) add(Calendar.MONTH, 1) }.timeInMillis
                onSave(name, value, members, myTurn, MoneyMath.monthKey(start))
            },
            enabled = value > 0
        )
    }
}
