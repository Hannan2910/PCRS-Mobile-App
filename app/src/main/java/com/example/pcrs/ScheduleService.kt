package com.example.pcrs

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class ScheduleService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val jsonString = "[{\"taskName\":\"Finish homework\",\"taskHour\":10,\"taskSecond\":35},{\"taskName\":\"Go to the gym\",\"taskHour\":11,\"taskSecond\":46}]"
            if (jsonString != null) {
                scheduleTasks(jsonString)
            }


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
                val pendingIntent = createPendingIntent(task.taskName, index)

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
            jsonObject.getInt("taskSecond")
        )

        // Optional: define a method to convert to a JSONObject

    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
