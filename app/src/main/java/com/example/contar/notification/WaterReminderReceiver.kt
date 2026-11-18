package com.example.contar.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.contar.R
import com.example.contar.data.WaterRepository
import com.example.contar.presentation.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WaterReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_ADD_GLASS -> {
                val repository = WaterRepository(context)
                CoroutineScope(Dispatchers.IO).launch {
                    repository.addGlass()
                }
                cancelNotification(context)
            }
            else -> {
                showWaterNotification(context)
            }
        }
    }

    private fun showWaterNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Recordatorio de Agua",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notificaciones periódicas para beber agua"
        }
        notificationManager.createNotificationChannel(channel)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val addGlassIntent = Intent(context, WaterReminderReceiver::class.java).apply {
            action = ACTION_ADD_GLASS
        }
        val addGlassPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            addGlassIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_water_drop)
            .setContentTitle("💧 ¡Hora de tomar agua!")
            .setContentText("Es momento de beber un vaso de agua para tu meta.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_water_drop, "+1 Vaso", addGlassPendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun cancelNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    companion object {
        const val CHANNEL_ID = "water_reminder_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_ADD_GLASS = "com.example.contar.ACTION_ADD_GLASS"
    }
}
