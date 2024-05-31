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
import com.google.gson.Gson
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import java.util.*

class Applister : Service() {
    data class AppData(
        val usageTimes: MutableList<AppInfo>
    )
    interface ApiService {
        @Headers("Content-Type: application/json")
        @POST("api/usage-time")
        fun sendAppUsageData(
            @Body requestBody: AppData
        ): Call<Unit>
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        // Start the service only once when the activity is started

        val jsonList = getInstalledApps(this)

       Log.d(
            ContentValues.TAG,
            "$jsonList"
        )
        val appData = AppData(jsonList)

        Log.d(ContentValues.TAG, "app list: $jsonList")
        // val jsonArray = JSONArray(appsinlist)

        ////val jsonObject = JSONObject()
        // jsonObject.put("apps", jsonArray)

       /* val jsonObject = JSONObject()
        jsonObject.put("apps", .toString())
        Log.d(ContentValues.TAG, "app json: $jsonObject")*/


        val sharedPreferences = getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
        val authToken = sharedPreferences.getString("AuthToken", "") ?: ""
        Log.d(ContentValues.TAG, "Auth token: $authToken")
        // Replace with your actual authorization token

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $authToken")
                    .build()
                chain.proceed(request)
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.fypsystem.me/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .build()

        val apiService = retrofit.create(ApiService::class.java)

        val call = apiService.sendAppUsageData(appData)
        call.enqueue(object : Callback<Unit> {
            override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                if (response.isSuccessful) {
                    Log.d(ContentValues.TAG, "Server response33: ${response.code()}")
                } else {
                    Log.e(ContentValues.TAG, "Error response: ${response.code()}")
                    val errorBody = response.errorBody()?.string()
                    if (errorBody != null) {
                        Log.d("response",errorBody)
                    }

                }
            }

            override fun onFailure(call: Call<Unit>, t: Throwable) {
                Log.e(ContentValues.TAG, "Failed to send app data", t)
            }
        })


       // val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        //val pendingIntent = PendingIntent.getService(this, 0, Intent(this, Applister::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        //val calendar = Calendar.getInstance()
        //calendar.timeInMillis = System.currentTimeMillis() + 86400000 // Set the time to 10 seconds from now
        //alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, 10000, pendingIntent)
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, Applister::class.java)
        val pendingIntent = PendingIntent.getService(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 0) // Set the time to 12:00 AM
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

// Check if the current time is after the set alarm time, if so, push the alarm to the next day
        if(Calendar.getInstance().after(calendar)){
            calendar.add(Calendar.DATE, 1)
        }

// Set up the alarm manager to run the service once a day
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )


        // Set up the alarm manager to run the service once a day
      /*  val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 0) // Set the time to 12:00 AM
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, AlarmManager.INTERVAL_DAY, pendingIntent)
*/
        // Create the notification channel for Android O and above

        return START_STICKY
    }

    /*private fun getInstalledApps(context: Context): List<ApplicationInfo> {
        val packageManager = context.packageManager
        return packageManager.getInstalledApplications(PackageManager.GET_META_DATA or PackageManager.GET_SHARED_LIBRARY_FILES)
    }*/
    data class AppInfo(

        val packageName: String,
        val usageTime: Int
    )
    private fun getInstalledApps(context: Context): MutableList<AppInfo> {
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
            val app = AppInfo( packageName, (usageTime/60000).toInt())
            appsList.add(app)
            val jsonObject = JSONObject()
            jsonObject.put("pkg_name", packageName)
            jsonObject.put("usage_time", (usageTime/60000).toInt())
            jsonArray.put(jsonObject)

           /* Log.d(
                ContentValues.TAG,
                "App name: $appName, package name: $packageName, usage time: ${(usageTime/1000).toInt()}"
            )*/
        }


        return appsList
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