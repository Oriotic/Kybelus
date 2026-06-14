package kybelus.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import kybelus.app.SettingsFragment
import java.text.SimpleDateFormat
import java.util.*

object NotificationScheduler {

    fun scheduleTaskReminder(context: Context, taskId: Int, taskTitle: String, dueDate: String) {
        if (dueDate.isEmpty()) return

        val prefs = context.getSharedPreferences(SettingsFragment.PREFS_NAME, Context.MODE_PRIVATE)
        val hour = prefs.getInt(SettingsFragment.KEY_REMINDER_HOUR, 21)
        val minute = prefs.getInt(SettingsFragment.KEY_REMINDER_MINUTE, 0)

        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val date = dateFormat.parse(dueDate) ?: return

        val calendar = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        if (calendar.timeInMillis < System.currentTimeMillis()) return

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("task_title", taskTitle)
            putExtra("task_id", taskId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelTaskReminder(context: Context, taskId: Int) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }

    fun rescheduleAll(
        context: Context,
        tasks: List<kybelus.app.tasks.Task>,
        events: List<kybelus.app.calendar.Event>
    ) {
        tasks.forEach { cancelTaskReminder(context, it.id) }
        events.forEach { cancelTaskReminder(context, it.id + 10000) }

        val prefs = context.getSharedPreferences(SettingsFragment.PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(SettingsFragment.KEY_REMINDERS_ENABLED, false)) return

        tasks.filter { !it.isCompleted && it.dueDate.isNotEmpty() }.forEach {
            scheduleTaskReminder(context, it.id, it.title, it.dueDate)
        }

        events.filter { it.date.isNotEmpty() }.forEach {
            scheduleTaskReminder(context, it.id + 10000, it.title, it.date)
        }
    }
}