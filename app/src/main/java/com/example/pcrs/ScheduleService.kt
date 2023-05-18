package com.example.pcrs

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit

import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.*

class ScheduleService : Service() {

    data class ResponseModel(
        val tasks: List<DataItem>
    )

    data class DataItem(
        val taskName: String,
        val taskHour: Int,
        val tasMinute: Int
        // Add other properties as needed
    )
    interface ApiService {
        @GET("api/activities/child")
        fun getData(): Call<ResponseModel>
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

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
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)
        //fetchSchedule()
        val call = apiService.getData()
        call.enqueue(object : Callback<ResponseModel> {
            override fun onResponse(call: Call<ResponseModel>, response: Response<ResponseModel>) {
                if (response.isSuccessful) {

                    val responseModel = response.body()
                  //  Log.d("response",response.toString())
                    var index=0
                    if (responseModel != null) {
                        val tasks = responseModel.tasks
                        for (task in tasks) {
                            val taskName = task.taskName
                            val taskHour = task.taskHour
                            val taskMinute = task.tasMinute
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = System.currentTimeMillis()
                            calendar.set(Calendar.HOUR_OF_DAY, task.taskHour)
                            calendar.set(Calendar.MINUTE, task.tasMinute)
                            calendar.set(Calendar.SECOND, 0)
                            calendar.set(Calendar.MILLISECOND, 0)
                            val timeInMillis = calendar.timeInMillis

                            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
                            val pendingIntent = createPendingIntent(task.taskName, index+1)
                                index++
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                alarmManager.setExactAndAllowWhileIdle(
                                    AlarmManager.RTC_WAKEUP,
                                    timeInMillis,
                                    pendingIntent
                                )
                            } else {
                                alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
                            }
                            // Process each task as needed
                            Log.d("Task", "Name: $taskName, Hour: $taskHour, Minute: $taskMinute")
                        }
                            // val jsonString = responseModel.data
                        //scheduleTasks(jsonString)
                        // Handle the JSON string
                    }
                } else {
                    Log.e(ContentValues.TAG, "Error response: ${response.code()}")
                    val errorBody = response.errorBody()?.string()
                    if (errorBody != null) {
                        Log.d("response",errorBody)
                    }

                }
            }

            override fun onFailure(call: Call<ResponseModel>, t: Throwable) {
                // Request failed due to network error or other reasons
                Log.e(ContentValues.TAG, "Failed get schedule data", t)
            }
        })

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ScheduleService::class.java)
        val pendingIntent = PendingIntent.getService(
            this,
            45,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 4) // Set the time to 4:00 AM
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // Check if the specified time has already passed for today,
            // if yes, add 1 day to schedule it for tomorrow
            if (timeInMillis < System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

// Set up the alarm manager to trigger the service at 4:00 AM every day
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )




        /*  val jsonString = "[{\"taskName\":\"Finish homework\",\"taskHour\":10,\"taskMinute\":35},{\"taskName\":\"Go to the gym\",\"taskHour\":1,\"taskMinute\":4}]"
            if (jsonString != null) {
               scheduleTasks(jsonString)
              }*/
       /* val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ScheduleService::class.java)
        val pendingIntent = PendingIntent.getService(
            this,
            45,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 4) // Set the time to 12:00 AM
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

// Set up the alarm manager to run the service once a day
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )*/


        return START_STICKY
    }

    private fun scheduleTasks(jsonString: String) {
        val jsonArray = JSONArray(jsonString)

        val taskList = mutableListOf<Task>()

        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val task = Task(jsonObject)
            taskList.add(task)
        }

        if (taskList.isNotEmpty()) {
            for ((index, task) in taskList.withIndex()) {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = System.currentTimeMillis()
                calendar.set(Calendar.HOUR_OF_DAY, task.taskHour)
                calendar.set(Calendar.MINUTE, task.taskSecond)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val timeInMillis = calendar.timeInMillis

                val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
                val pendingIntent = createPendingIntent(task.taskName, index+1)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
                }
            }
        }
    }


    private fun createPendingIntent(notificationText: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, ScheduleReceiver::class.java)
        intent.action = "SHOW_NOTIFICATION"
        intent.putExtra("notificationText", notificationText)
        return PendingIntent.getBroadcast(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    data class Task(val taskName: String, val taskHour: Int, val taskSecond: Int) {
        // Optional: define a custom constructor to convert from a JSONObject
        constructor(jsonObject: JSONObject) : this(
            jsonObject.getString("taskName"),
            jsonObject.getInt("taskHour"),
            jsonObject.getInt("taskMinute")
        )

        // Optional: define a method to convert to a JSONObject

    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
