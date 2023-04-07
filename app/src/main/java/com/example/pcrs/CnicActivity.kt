package com.example.pcrs

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import com.example.pcrs.databinding.ActivityCnicBinding

class CnicActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCnicBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding= ActivityCnicBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val appOps = this.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val uid = android.os.Process.myUid()
        //Start the service only once when the activity is started
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, this.packageName)
        } else {
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, this.packageName)
        }

        if (mode != AppOpsManager.MODE_ALLOWED) {
            //  Permission not granted, prompt user to grant permission
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            this.startActivity(intent)
        }
        binding.btnContinue.setOnClickListener {

            //startService(Intent(this, PackageService::class.java))
            startActivity(Intent(this,HomeActivity::class.java))
        }

    }
}
