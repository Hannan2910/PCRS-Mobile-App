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
import org.json.JSONObject
import java.util.*

class GetBlocked : Service() {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        sharedPreferences = getSharedPreferences("UserName", MODE_PRIVATE)
        lateinit var alarmIntent: PendingIntent
        val username = sharedPreferences.getString("username", "")
        sendRequestToServer(username)
        val interval = 60 * 60 * 1000 // 1 hour in milliseconds
        //val triggerTime = SystemClock.elapsedRealtime() + interval
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
        }
        var alarmManager: AlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, GetBlocked::class.java)
        alarmIntent = PendingIntent.getService(this, 0, intent, 0)
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            interval.toLong(),
            alarmIntent
        )
        return START_STICKY
    }

    private fun sendRequestToServer(username: String?) {
        val url = "https://example.com/blocked_users"
        val jsonObject = JSONObject().apply {
            put("username", username)
        }
        val jsonObjectRequest = JsonObjectRequest(Request.Method.POST, url, jsonObject,
            Response.Listener { response ->
                Log.d(ContentValues.TAG, "Server response: $response")
                val sharedPreferences = getSharedPreferences("BlockedApps", Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putString("blockedApps", response.toString())
                editor.apply()
            },
            Response.ErrorListener { error ->
                // handle error response from server
                Log.e(ContentValues.TAG, "Error sending app data: $error")
            }
        )
        Volley.newRequestQueue(applicationContext).add(jsonObjectRequest)
    }
}
