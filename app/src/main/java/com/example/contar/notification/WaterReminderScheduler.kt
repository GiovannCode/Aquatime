package com.example.contar.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock

class WaterReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(context, WaterReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun scheduleReminder(intervalMinutes: Int) {
        cancelReminder()
        val intervalMillis = intervalMinutes * 60 * 1000L
        val triggerAtMillis = SystemClock.elapsedRealtime() + intervalMillis

        alarmManager.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            triggerAtMillis,
            intervalMillis,
            createPendingIntent()
        )
    }

    fun cancelReminder() {
        alarmManager.cancel(createPendingIntent())
    }

    fun triggerImmediateTestNotification() {
        val intent = Intent(context, WaterReminderReceiver::class.java)
        context.sendBroadcast(intent)
    }
}
