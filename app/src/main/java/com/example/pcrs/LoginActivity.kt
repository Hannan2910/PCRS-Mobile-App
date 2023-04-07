package com.example.pcrs

import android.app.AppOpsManager
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.pcrs.databinding.ActivityLoginBinding
import org.json.JSONObject


class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
       binding.btnLogin.setOnClickListener {
            val userName=binding.etEmail.text.toString()
            val passWord=binding.etPassword.text.toString()
            if (userName.isEmpty() || passWord.isEmpty()) {
                Toast.makeText(this, "Username or password is empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
           Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
            val sharedPreferences = getSharedPreferences("UserName", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString("username", userName)
            editor.apply()
            Log.d(
                TAG,
                "name: $userName, password: $passWord"
            )
            checkPermissions()
            //startActivity(Intent(this,PermissionActivity::class.java))
        }



      /*  binding.btnLogin.setOnClickListener {
            val userName = binding.etEmail.text.toString()
            val passWord = binding.etPassword.text.toString()

            if (userName.isEmpty() || passWord.isEmpty()) {
                Toast.makeText(this, "Username or password is empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "Please Wait", Toast.LENGTH_SHORT).show()

            val queue = Volley.newRequestQueue(this)
            val url = "http://your.server.com/login.php"

            val params = HashMap<String, String>()
            params["username"] = userName
            params["password"] = passWord

            val jsonRequest = JSONObject(params as Map<*, *>)
            val jsonObjectRequest = JsonObjectRequest(
                Request.Method.POST, url, jsonRequest,
                { response ->
                    val success = response.getBoolean("success")
                    if (success) {
                        // Login successful
                        val userData = response.getJSONObject("data")
                        val authToken = userData.getString("token")

                        // Save the authToken to shared preferences
                        val sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE)
                        val editor = sharedPreferences.edit()
                        editor.putString("authToken", "Bearer $authToken")
                        editor.apply()

                        Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                        checkPermissions()
                      //  startService(Intent(this, PackageService::class.java))
                        //startActivity(Intent(this, PermissionActivity::class.java))
                    } else {
                        // Login failed
                        val error = response.getString("error")
                        Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                    }
                },
                { error ->
                    // Volley error occurred
                    Log.e(TAG, "Volley error: ${error.message}")
                    Toast.makeText(this, "Volley error occurred", Toast.LENGTH_SHORT).show()
                }
            )

            queue.add(jsonObjectRequest)

        }*/

        binding.forgotPw.setOnClickListener {
            val url = "https://pcrs.vercel.app/"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }
        binding.tvHaventAccount.setOnClickListener {
            val url = "https://pcrs.vercel.app/"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

    }
    fun checkPermissions(){
        val appOps = this.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val uid = android.os.Process.myUid()
        val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        //Start the service only once when the activity is started
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, this.packageName)
        } else {
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, this.packageName)
        }

        if ((mode != AppOpsManager.MODE_ALLOWED) && !accessibilityManager.isEnabled) {
            startActivity(Intent(this, PermissionActivity::class.java))
        }
        else if ((mode != AppOpsManager.MODE_ALLOWED) && accessibilityManager.isEnabled) {
            startActivity(Intent(this, PermissionActivity::class.java))
        }
        else if ((mode == AppOpsManager.MODE_ALLOWED) && !accessibilityManager.isEnabled) {
            startActivity(Intent(this, CnicActivity::class.java))
        }
        else if ((mode == AppOpsManager.MODE_ALLOWED) && accessibilityManager.isEnabled) {
            startActivity(Intent(this, HomeActivity::class.java))
        }
    }
}