package com.example.pcrs

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import com.example.pcrs.databinding.ActivityBlockingBinding
import com.example.pcrs.databinding.ActivityHomeBinding

class BlockingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBlockingBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityBlockingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
    override fun onBackPressed() {
        // Do nothing to prevent the user from leaving the BlockingActivity
      /*  val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()*/
    }
}