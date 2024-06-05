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
        val jsonList = getInstalledApps(this)
        Log.d(ContentValues.TAG, "$jsonList")
        val appData = AppData(jsonList)
        Log.d(ContentValues.TAG, "app list: $jsonList")

        val sharedPreferences = getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
        val authToken = sharedPreferences.getString("AuthToken", "") ?: ""
        Log.d(ContentValues.TAG, "Auth token: $authToken")
        
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
                        Log.d("response", errorBody)
                    }
                }
            }

            override fun onFailure(call: Call<Unit>, t: Throwable) {
                Log.e(ContentValues.TAG, "Failed to send app data", t)
            }
        })

        // Set up the alarm manager to run the service once a minute
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, Applister::class.java)
        val pendingIntent = PendingIntent.getService(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val triggerTime = System.currentTimeMillis() + (60 * 1000) // 1 minute from now
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            60 * 1000, // 1 minute interval
            pendingIntent
        )

        return START_STICKY
    }

    data class AppInfo(
        val packageName: String,
        val usageTime: Int
    )

    private fun getInstalledApps(context: Context): MutableList<AppInfo> {
        val packageManager = context.packageManager
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val uid = android.os.Process.myUid()
        val appsList = mutableListOf<AppInfo>()

        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, context.packageName)
        } else {
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, context.packageName)
        }
        if (mode != AppOpsManager.MODE_ALLOWED) {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val usageStats = usageStatsManager.queryAndAggregateUsageStats(
            System.currentTimeMillis() - (1000 * 60 * 60 * 24),
            System.currentTimeMillis()
        )

        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        for (appInfo in installedApps) {
            val packageName = appInfo.packageName
            val isSystemApp = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
            val isUserApp = appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0 || appInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0
            if (isSystemApp && !isUserApp) {
                continue
            }
            val usageTime = usageStats[packageName]?.totalTimeInForeground ?: 0
            val app = AppInfo(packageName, (usageTime / 60000).toInt())
            appsList.add(app)
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
