package com.example.pcrs

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.*

class AppList : Service() {

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "ForegroundServiceChannel"
        private const val NOTIFICATION_ID = 123
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val serviceIntent = Intent(this, Applister::class.java)
        startService(serviceIntent)
       val serviceIntent2 = Intent(this, GetBlocked::class.java)
        startService(serviceIntent2)
        val serviceIntent3 = Intent(this, ScheduleService::class.java)
        startService(serviceIntent3)
        val serviceIntent5 = Intent(this, MyAccessibilityService::class.java)
        startService(serviceIntent5)
        val serviceIntent4 = Intent(this, LocationService::class.java)
       startService(serviceIntent4)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, "Foreground Service Channel", NotificationManager.IMPORTANCE_DEFAULT)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }


        // Create the notification that represents the service's ongoing operation
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("PCRS")
            .setContentText("Your phone is being monitored")
            .setSmallIcon(R.drawable.ic_circle_account)
            .build()

        // Start the service as a foreground service
        startForeground(NOTIFICATION_ID, notification)

        return START_STICKY
    }



    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
