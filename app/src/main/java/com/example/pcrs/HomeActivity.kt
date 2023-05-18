package com.example.pcrs

import android.Manifest
import android.app.AppOpsManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.pcrs.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val PERMISSION_REQUEST_LOCATION = 123
        binding=ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
//       val appOps = this.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
//        val uid = android.os.Process.myUid()
//        // Start the service only once when the activity is started
//        val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, this.packageName)
//        if (mode != AppOpsManager.MODE_ALLOWED) {
//            // Permission not granted, prompt user to grant permission
//            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
//            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//            this.startActivity(intent)
//        }

        val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)

       /* if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request location permissions
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                PERMISSION_REQUEST_LOCATION
            )
            return
        }*/

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val name = "AppList"
//            val descriptionText = "AppList service notification channel"
//            val importance = NotificationManager.IMPORTANCE_DEFAULT
//            val channel = NotificationChannel("channelId", name, importance).apply {
//                description = descriptionText
//            }
//            val notificationManager: NotificationManager =
//                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//            notificationManager.createNotificationChannel(channel)
//        }
        binding.btnLogout.setOnClickListener {
            startService(Intent(this, AppList::class.java))
            //startService(Intent(this, BlockApp::class.java))

        }
    }


}