package com.example.pcrs

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class Applister : Service() {



    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        // Start the service only once when the activity is started

        val jsonList = getInstalledApps(this)

       Log.d(
            ContentValues.TAG,
            "$jsonList"
        )

       val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getService(this, 0, Intent(this, Applister::class.java), 0)
       // val calendar = Calendar.getInstance()
        //calendar.timeInMillis = System.currentTimeMillis() + 10000 // Set the time to 10 seconds from now
        //alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, 10000, pendingIntent)


        // Set up the alarm manager to run the service once a day
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.set(Calendar.HOUR_OF_DAY, 0) // Set the time to 12:00 AM
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, AlarmManager.INTERVAL_DAY, pendingIntent)

        // Create the notification channel for Android O and above

        return START_STICKY
    }

    /*private fun getInstalledApps(context: Context): List<ApplicationInfo> {
        val packageManager = context.packageManager
        return packageManager.getInstalledApplications(PackageManager.GET_META_DATA or PackageManager.GET_SHARED_LIBRARY_FILES)
    }*/
    data class AppInfo(

        val packageName: String,
        val usageTime: Long
    )
    private fun getInstalledApps(context: Context): JSONArray {
        val packageManager = context.packageManager
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val uid = android.os.Process.myUid()
        val appsList = mutableListOf<AppInfo>()
        // Check if permission is granted
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, context.packageName)
        } else {
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, context.packageName)
        }
        if (mode != AppOpsManager.MODE_ALLOWED) {
            // Permission not granted, prompt user to grant permission
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }

        // Get usage stats for each app
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val usageStats = usageStatsManager.queryAndAggregateUsageStats(
            System.currentTimeMillis() - (1000 * 60 * 60 * 24),
            System.currentTimeMillis()
        )


        val installedApps =
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        val jsonArray = JSONArray()
        for (appInfo in installedApps) {
            val appName = appInfo.loadLabel(packageManager).toString()
            val packageName = appInfo.packageName
            val isSystemApp = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
            val isUserApp = appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0 || appInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0
            if (isSystemApp && !isUserApp) {
                continue // Skip system apps and apps that are not accessible by user
            }
            val usageTime = usageStats[packageName]?.totalTimeInForeground ?: 0
            //val usageTime=getAppUsageTime(packageName,System.currentTimeMillis())
            // Create AppInfo object with usage time
            val app = AppInfo( packageName, usageTime)
            appsList.add(app)
            val jsonObject = JSONObject()
            jsonObject.put("pkg_name", packageName)
            jsonObject.put("usage_time", (usageTime/1000).toInt())
            jsonArray.put(jsonObject)

            Log.d(
                ContentValues.TAG,
                "App name: $appName, package name: $packageName, usage time: ${(usageTime/1000).toInt()}"
            )
        }


        return jsonArray
    }

    private fun getAppUsageTime(packageName: String, currentTime: Long): Long {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val startOfDay = getStartOfDay(currentTime)
        val queryUsageStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startOfDay, currentTime)
        var totalUsageTime = 0L
        for (usageStats in queryUsageStats) {
            if (usageStats.packageName == packageName) {
                totalUsageTime += usageStats.totalTimeInForeground
            }
        }
        return totalUsageTime
    }

    private fun getStartOfDay(currentTime: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentTime
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }






    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}