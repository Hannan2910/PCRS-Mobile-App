package com.example.pcrs

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.*

class GetBlocked : Service() {

    data class ResponseModel(
        val predictions: List<DataItem>
    )
    data class DataItem(
        val packageName: String,
        val allowedTime: Int
        // Add other properties as needed
    )

    interface ApiService {
        @GET("api/usage-time/predictions")
        fun getData(): Call<ResponseModel>
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
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
            .baseUrl("https://api.fypsystem.me/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val apiService = retrofit.create(ApiService::class.java)
        //fetchSchedule()
        val call = apiService.getData()
        call.enqueue(object : Callback<ResponseModel> {
            override fun onResponse(call: Call<ResponseModel>, response: retrofit2.Response<ResponseModel>) {
                if (response.isSuccessful) {
                    val responseModel = response.body()
                    if (responseModel != null) {
                        val predictions = responseModel.predictions

                        val gson = Gson()
                        val jsonString = gson.toJson(predictions)

                        val sharedPrefs = getSharedPreferences("blocked", Context.MODE_PRIVATE)
                        val editor = sharedPrefs.edit()
                        editor.remove("jsonString")
                        editor.putString("jsonString", jsonString)
                        editor.apply()

                        Log.d("response", jsonString)



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
                Log.e(ContentValues.TAG, "Failed get block app data", t)
            }
        })




        lateinit var alarmIntent: PendingIntent
        val interval = 3*60 * 60 * 1000 // 3 hours in milliseconds
        val triggerTime = System.currentTimeMillis() + interval // setting first alarm 3 hours from now
        val alarmManager: AlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, GetBlocked::class.java)
        alarmIntent = PendingIntent.getService(this, 65, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            interval.toLong(),
            alarmIntent
        )

        return START_STICKY
    }


}
