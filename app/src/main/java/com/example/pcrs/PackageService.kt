package com.example.pcrs

import android.app.AppOpsManager
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class PackageService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val jsonArray = getInstalledApps(applicationContext)
        val sharedPreferences = getSharedPreferences("UserName", Context.MODE_PRIVATE)
        val username = sharedPreferences.getString("username", "")
        // Create JSON object to hold request body
        val jsonObject = JSONObject()
        jsonObject.put("username", username)
        jsonObject.put("apps", jsonArray)

        // Create request object
        val queue = Volley.newRequestQueue(this)
        val url = "https://catfact.ninja/fact"
        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonObject,
            { response ->
                Log.d(ContentValues.TAG, "Server response: $response")

            },
            { error ->
                Log.e(ContentValues.TAG, "Error sending app data: $error")
                // Handle error response
            })

        // Add request to queue
        queue.add(request)





        return START_STICKY
    }




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
        val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, context.packageName)
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
            val usageTime = getAppUsageTime(packageName,System.currentTimeMillis())

            // Create AppInfo object with usage time
            val app = AppInfo(packageName, usageTime)
            appsList.add(app)
            val jsonObject = JSONObject()
            jsonObject.put("packageName", packageName)
          //  jsonObject.put("usageTime", (usageTime/1000).toInt())
            jsonArray.put(jsonObject)

            Log.d(
                ContentValues.TAG,
                "App name: $appName, package name: $packageName, usage time: $usageTime"
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