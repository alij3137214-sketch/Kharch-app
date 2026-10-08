package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.profile.UserProfile
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private enum class DeleteChoice(val title: String, val hint: String, val warning: String) {
    THIS_MONTH("This month's records", "Only what you wrote this month", "Records from this month will be deleted."),
    LAST_MONTH("Last month's records", "Only what you wrote last month", "Records from last month will be deleted."),
    ALL_RECORDS("All records", "Your limits, bills and goals stay", "Every record will be deleted. Your limits, bills and goals stay."),
    EVERYTHING("Everything", "Start again from the first questions", "Everything will be deleted: records, limits, bills, goals, committees, udhaar and your answers.")
}

/** The small trash icon on Home opens this. Every choice asks "are you sure?" first. */
@Composable
fun DeleteDataSheet(
    onDeleteMonth: (year: Int, month: Int) -> Unit,
    onDeleteAllRecords: () -> Unit,
    onDeleteEverything: () -> Unit,
    onDismiss: () -> Unit
) {
    var confirm by remember { mutableStateOf<DeleteChoice?>(null) }
    val monthFmt = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }

    FormSheet(title = "Delete data", subtitle = "Choose what to delete. You cannot get it back.", onDismiss = onDismiss) {
        DeleteChoice.entries.forEach { choice ->
            val shape = RoundedCornerShape(20.dp)
            val label = when (choice) {
                DeleteChoice.THIS_MONTH -> monthFmt.format(Calendar.getInstance().time)
                DeleteChoice.LAST_MONTH -> monthFmt.format(Calendar.getInstance().apply { add(Calendar.MONTH, -1) }.time)
                else -> choice.hint
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pressable(pressedScale = 0.98f) { confirm = choice }
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.background)
                    .border(1.dp, if (choice == DeleteChoice.EVERYTHING) ExpenseRed.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), shape)
                    .padding(16.dp)
                    .testTag("delete_choice_${choice.name}"),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    choice.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (choice == DeleteChoice.EVERYTHING) ExpenseRed else MaterialTheme.colorScheme.onSurface
                )
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    confirm?.let { choice ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Are you sure?", fontWeight = FontWeight.Bold) },
            text = { Text(choice.warning) },
            confirmButton = {
                Button(
                    onClick = {
                        val cal = Calendar.getInstance()
                        when (choice) {
                            DeleteChoice.THIS_MONTH -> onDeleteMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
                            DeleteChoice.LAST_MONTH -> {
                                cal.add(Calendar.MONTH, -1)
                                onDeleteMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
                            }
                            DeleteChoice.ALL_RECORDS -> onDeleteAllRecords()
                            DeleteChoice.EVERYTHING -> onDeleteEverything()
                        }
                        confirm = null
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed, contentColor = MaterialTheme.colorScheme.onError),
                    modifier = Modifier.testTag("delete_confirm_button")
                ) { Text("Yes, delete") }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("No, keep it") } }
        )
    }
}

/** Name, reminders, and a way to answer the first questions again. */
@Composable
fun SettingsSheet(
    profile: UserProfile,
    onChange: ((UserProfile) -> UserProfile) -> Unit,
    onEditAnswers: () -> Unit,
    onAskNotificationPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    FormSheet(title = "Settings", onDismiss = onDismiss) {
        TextBox(value = profile.name, onValueChange = { v -> onChange { it.copy(name = v.take(24)) } }, label = "Your name")

        Text("Reminders", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 4.dp))
        ToggleRow("Tell me when I spend too much", profile.budgetAlerts) { on ->
            onChange { it.copy(budgetAlerts = on) }
            if (on) onAskNotificationPermission()
        }
        ToggleRow("Remind me before bills and committee are due", profile.billReminders) { on ->
            onChange { it.copy(billReminders = on) }
            if (on) onAskNotificationPermission()
        }
        ToggleRow("Remind me in the evening to write down today's spending", profile.dailyReminder) { on ->
            onChange { it.copy(dailyReminder = on) }
            if (on) onAskNotificationPermission()
        }

        PrimaryButton("Change my answers", onClick = { onDismiss(); onEditAnswers() }, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun ToggleRow(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = EmeraldPrimary, checkedThumbColor = MaterialTheme.colorScheme.onPrimary)
        )
    }
}
