package com.example.pcrs

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class ScheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "SHOW_NOTIFICATION") {
            val notificationText = intent.getStringExtra("notificationText")
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel("default", "Default Channel", NotificationManager.IMPORTANCE_DEFAULT)
                notificationManager.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(context, "default")
                .setContentTitle("Scheduled Task")
                .setContentText(notificationText)
                .setSmallIcon(R.drawable.ic_circle_account)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(0, notification)
        }
    }
}