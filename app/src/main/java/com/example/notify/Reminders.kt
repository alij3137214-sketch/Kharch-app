package com.example.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.db.KharchDatabase
import com.example.data.model.TransactionType
import com.example.data.model.WishStatus
import com.example.data.profile.ProfileStore
import com.example.data.profile.UserProfile
import com.example.domain.DAY_MS
import com.example.domain.MoneyMath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

/** Sets the two daily alarms (morning check, evening reminder) from what the person turned on. */
object ReminderScheduler {

    private const val KIND = "kind"
    const val MORNING = "MORNING"
    const val EVENING = "EVENING"

    fun schedule(context: Context, profile: UserProfile) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val morningWanted = profile.onboardingDone && profile.billReminders
        val eveningWanted = profile.onboardingDone && profile.dailyReminder
        apply(context, alarms, MORNING, 9, morningWanted)
        apply(context, alarms, EVENING, 20, eveningWanted)
    }

    private fun apply(context: Context, alarms: AlarmManager, kind: String, hour: Int, wanted: Boolean) {
        val pending = pendingIntent(context, kind)
        alarms.cancel(pending)
        if (!wanted) return
        val first = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        // Inexact on purpose: the phone may move it a little to save battery. No special permission is needed.
        alarms.setInexactRepeating(AlarmManager.RTC_WAKEUP, first, AlarmManager.INTERVAL_DAY, pending)
    }

    private fun pendingIntent(context: Context, kind: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            if (kind == MORNING) 101 else 102,
            Intent(context, ReminderReceiver::class.java).putExtra(KIND, kind),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    fun kindOf(intent: Intent): String = intent.getStringExtra(KIND) ?: MORNING
}

/** Runs once a day. It reads the database and shows a notification only if something needs attention. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val profile = ProfileStore.from(app).profile.value
                val dao = KharchDatabase.getDatabase(app).kharchDao()
                when (ReminderScheduler.kindOf(intent)) {
                    ReminderScheduler.EVENING -> if (profile.dailyReminder) {
                        val todayStart = MoneyMath.startOfDay(System.currentTimeMillis())
                        val wroteToday = dao.getAllTransactionsOnce().any { it.timestamp >= todayStart }
                        if (!wroteToday) {
                            Notifier.showDaily(app, "Did you spend today?", "Write it down now. It only takes a few seconds.")
                        }
                    }
                    else -> if (profile.billReminders) {
                        checkBills(app, dao.getAllBillRemindersOnce())
                        checkCommittees(app, dao.getAllCommitteesOnce())
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun checkBills(context: Context, bills: List<com.example.data.model.BillReminderEntity>) {
        val today = MoneyMath.startOfDay(System.currentTimeMillis())
        val soon = bills.filter { !it.isPaid && MoneyMath.startOfDay(it.dueDate) <= today + DAY_MS }
        if (soon.isEmpty()) return
        val first = soon.minBy { it.dueDate }
        val days = ((MoneyMath.startOfDay(first.dueDate) - today) / DAY_MS).toInt()
        val whenText = when {
            days < 0 -> "is late"
            days == 0 -> "is due today"
            else -> "is due tomorrow"
        }
        val amount = "Rs. " + String.format(Locale.getDefault(), "%,.0f", first.amount)
        if (soon.size == 1) {
            Notifier.showBills(context, "${first.title} $whenText", "Pay $amount so you are not late.")
        } else {
            Notifier.showBills(context, "${soon.size} bills need you", "${first.title} $whenText, and ${soon.size - 1} more.")
        }
    }

    private fun checkCommittees(context: Context, committees: List<com.example.data.model.CommitteeEntity>) {
        // Only on the 1st, 10th and 20th, so it does not nag every day.
        val day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        if (day != 1 && day != 10 && day != 20) return
        val due = committees.firstOrNull { MoneyMath.committeeStatus(it).paymentDue }
        val turn = committees.firstOrNull { MoneyMath.committeeStatus(it).myTurnNow }
        when {
            turn != null -> Notifier.showBills(context, "It is your turn!", "You get Rs. ${String.format(Locale.getDefault(), "%,.0f", turn.pot)} from ${turn.name}.")
            due != null -> Notifier.showBills(context, "Committee payment", "Pay Rs. ${String.format(Locale.getDefault(), "%,.0f", due.monthlyAmount)} for ${due.name} this month.")
        }
    }
}

/** After the phone restarts all alarms are gone, so they are set again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ReminderScheduler.schedule(context, ProfileStore.from(context).profile.value)
        }
    }
}
