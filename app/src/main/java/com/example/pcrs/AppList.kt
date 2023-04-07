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
      // val appOps = this.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        //val uid = android.os.Process.myUid()
        // Start the service only once when the activity is started
       // val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, this.packageName)
        //if (mode != AppOpsManager.MODE_ALLOWED) {
            // Permission not granted, prompt user to grant permission
          //  val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            //intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            //this.startActivity(intent)
        //}
       // val appsList = getInstalledApps(this)
      /*  for (appInfo in appsList) {
            val appName = appInfo.loadLabel(packageManager).toString()
            val packageName = appInfo.packageName

            Log.d(ContentValues.TAG, "App name: $appName, package name: $packageName")
        }*/
        //var counter=0
        val serviceIntent = Intent(this, Applister::class.java)
        startService(serviceIntent)
       val serviceIntent2 = Intent(this, ScheduleService::class.java)
        startService(serviceIntent2)
        val serviceIntent3 = Intent(this, MyAccessibilityService::class.java)
        startService(serviceIntent3)

        /*counter++
        Log.d(
            ContentValues.TAG,
            "Number be good: $counter"
        )*/
        // Set up the alarm manager to run the service once a day
//        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
//        val pendingIntent = PendingIntent.getService(this, 0, Intent(this, AppList::class.java), 0)
//        val calendar = Calendar.getInstance()
//        calendar.timeInMillis = System.currentTimeMillis() + 10000 // Set the time to 10 seconds from now
//        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, 10000, pendingIntent)
        /*val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.set(Calendar.HOUR_OF_DAY, 0) // Set the time to 12:00 AM
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, AlarmManager.INTERVAL_DAY, pendingIntent)*/

        // Create the notification channel for Android O and above
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

    /*private fun getInstalledApps(context: Context): List<ApplicationInfo> {
        val packageManager = context.packageManager
        return packageManager.getInstalledApplications(PackageManager.GET_META_DATA or PackageManager.GET_SHARED_LIBRARY_FILES)
    }*/
 /*   data class AppInfo(
        val name: String,
        val packageName: String,
        val usageTime: Long
    )
    private fun getInstalledApps(context: Context): List<AppInfo> {
        val packageManager = context.packageManager
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val uid = android.os.Process.myUid()
        val appsList = mutableListOf<AppInfo>()
        // Check if permission is granted
        val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, context.packageName)
        if (mode != AppOpsManager.MODE_ALLOWED) {
            // Permission not granted, prompt user to grant permission
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
        else {
            // Get usage stats for each app
            val usageStatsManager =
                context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val usageStats = usageStatsManager.queryAndAggregateUsageStats(
                System.currentTimeMillis() - (1000 * 60 * 60 * 24),
                System.currentTimeMillis()
            )


            val installedApps =
                packageManager.getInstalledApplications(PackageManager.GET_META_DATA or PackageManager.GET_SHARED_LIBRARY_FILES)

            for (appInfo in installedApps) {
                val appName = appInfo.loadLabel(packageManager).toString()
                val packageName = appInfo.packageName
                val usageTime = usageStats[packageName]?.totalTimeInForeground ?: 0

                // Create AppInfo object with usage time
                val app = AppInfo(appName, packageName, usageTime)
                appsList.add(app)

                Log.d(
                    ContentValues.TAG,
                    "App name: $appName, package name: $packageName, usage time: $usageTime"
                )
            }

        }
        return appsList
    }*/


    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
