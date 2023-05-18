/*package com.example.pcrs

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.app.ActivityManager
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*

class MyAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Check if the event type is a window state change event

        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            // Get the package name of the app that is currently in the foreground
            val packageName = event.packageName?.toString()

            // Log the package name to the console for testing purposes
            Log.d("MyAccessibilityService", "Package name: $packageName")
            if(packageName=="com.kiloo.subwaysurf") {
                val intent = Intent(this, BlockingActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                // Check if the package name is in your list of blocked apps
            }


        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Set the alarm to trigger every 7 seconds
        Log.d(ContentValues.TAG, "Service Restarted")
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getService(this, 0, Intent(this, MyAccessibilityService::class.java), 0)
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis() + 10000 // Set the time to 10 seconds from now
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, 10000, pendingIntent)
        return START_STICKY
    }

    override fun onInterrupt() {
        // Handle service interruption here
    }
}*/
package com.example.pcrs

import android.accessibilityservice.AccessibilityService
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class MyAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Check if the event type is a window state change event

        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event?.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            // Get the package name of the app that is currently in the foreground
            val packageName = event.packageName?.toString()

            // Log the package name to the console for testing purposes


            val usageTime =    getAppUsageTime(packageName,System.currentTimeMillis())
            val appList = mutableListOf<AppData>()
             val sharedPrefs = getSharedPreferences("blocked", Context.MODE_PRIVATE)
            //val editor = sharedPrefs.edit()
            //editor.remove("jsonString")
            //editor.putString("jsonString","[{\"packageName\":\"com.kiloo.subwaysurf\",\"allowedTime\":0}]")
            //editor.apply() // Add this line to save the changes

            val jsonString = sharedPrefs.getString("jsonString", "") ?: ""
           /* Log.d(
                "MyAccessibilityService",
                jsonString
            )*/
            //val jsonString = "[{\"packageName\":\"com.backflipstudios.transformersearthwars\",\"allowedTime\":0}]"
            if(!jsonString.isEmpty()) {
                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val task = AppData(jsonObject)
                    appList.add(task)
                }

                Log.d(
                    "MyAccessibilityService",
                    "Package name: $packageName, Usage time: ${(usageTime / 1000).toInt()}"
                )

                for (app in appList) {
                    if (packageName == app.packageName) {

                        if ((usageTime / 60000).toInt() <= app.allowedTime) {
                            val intent = Intent(this, BlockingActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                            startActivity(intent)
                            break // Stop iterating through the list once you find a match
                        }
                    }
                }
            }
        }
    }
    private fun getAppUsageTime(packageName: String?, currentTime: Long): Long {
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
    data class AppData(val packageName: String, val allowedTime: Int) {
        // Optional: define a custom constructor to convert from a JSONObject
        constructor(jsonObject: JSONObject) : this(
            jsonObject.getString("packageName"),
            jsonObject.getInt("allowedTime")
        )
    }
    override fun onInterrupt() {
        // Handle service interruption here
    }
}
