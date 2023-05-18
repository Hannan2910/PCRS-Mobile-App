package com.example.pcrs

import android.app.AppOpsManager
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import com.google.gson.Gson
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import org.json.JSONArray
import java.util.*
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST


class PackageService : Service() {
    data class AppData(
        val apps: List<String>
    )

    interface ApiService {
        @Headers("Content-Type: application/json")
        @POST("api/apps")
        fun sendAppData(
            @Body requestBody: AppData
        ): Call<Unit>
    }



    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val appsinlist = getInstalledApps(applicationContext)
        val appData = AppData(appsinlist)

        Log.d(ContentValues.TAG, "app list: $appsinlist")
       // val jsonArray = JSONArray(appsinlist)

        ////val jsonObject = JSONObject()
       // jsonObject.put("apps", jsonArray)

        val jsonObject = JSONObject()
        jsonObject.put("apps", appsinlist.toString())
        Log.d(ContentValues.TAG, "app json: $jsonObject")


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
            .baseUrl("https://pcrslive.me/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .build()

        val apiService = retrofit.create(ApiService::class.java)

        val call = apiService.sendAppData(appData)
        call.enqueue(object : Callback<Unit> {
            override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                if (response.isSuccessful) {
                    Log.d(ContentValues.TAG, "Server response: ${response.code()}")
                    stopSelf()
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

        return START_STICKY
    }



    data class AppInfo(

        val packageName: String,
        val usageTime: Int

    )
    private fun getInstalledApps(context: Context): MutableList<String> {
        val packageManager = context.packageManager
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val uid = android.os.Process.myUid()
        val appsList = mutableListOf<AppInfo>()
        val appslist= mutableListOf<String>()
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
            val usageTime = (getAppUsageTime(packageName,System.currentTimeMillis())/(1000*60)).toInt()

            // Create AppInfo object with usage time
            val app = AppInfo(packageName, usageTime)
            appsList.add(app)
           appslist.add(packageName)

            val jsonObject = JSONObject()
            jsonObject.put("pkg_name", packageName)
           // jsonObject.put("usage_time", usageTime)
            //jsonObject.put("Date",1682294400000)
            jsonArray.put(jsonObject)

           // Log.d(
             //   ContentValues.TAG,
               // "App name: $appName, package name: $packageName, usage time: $usageTime"
           // )
        }



        return appslist
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