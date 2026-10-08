package com.example.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

/** Shows phone notifications. Nothing is sent to any server; all of it is made on the phone. */
object Notifier {

    const val CHANNEL_BUDGET = "budget"
    const val CHANNEL_BILLS = "bills"
    const val CHANNEL_DAILY = "daily"

    private const val ID_BUDGET = 1001
    private const val ID_BILLS = 1002
    private const val ID_DAILY = 1003
    private const val ID_WISH = 1004

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_BUDGET, "Spending limits", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Tells you when you are close to a limit or over it" }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_BILLS, "Bills and committee", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Reminds you before a bill or a committee payment is due" }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_DAILY, "Daily reminder", NotificationManager.IMPORTANCE_LOW)
                .apply { description = "A quiet reminder to write down today's spending" }
        )
    }

    /** True when the phone allows this app to show notifications. */
    fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun showBudget(context: Context, title: String, text: String) =
        show(context, CHANNEL_BUDGET, ID_BUDGET + (title.hashCode() and 0xFF), title, text)

    fun showBills(context: Context, title: String, text: String) = show(context, CHANNEL_BILLS, ID_BILLS, title, text)

    fun showDaily(context: Context, title: String, text: String) = show(context, CHANNEL_DAILY, ID_DAILY, title, text)

    fun showWish(context: Context, title: String, text: String) = show(context, CHANNEL_BILLS, ID_WISH, title, text)

    private fun show(context: Context, channel: String, id: Int, title: String, text: String) {
        if (!canNotify(context)) return
        ensureChannels(context)
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_kharch)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(if (channel == CHANNEL_BUDGET) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }
}
