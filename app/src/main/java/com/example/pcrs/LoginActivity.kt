package com.example.pcrs

import android.app.AppOpsManager
import android.content.ContentValues
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
import com.example.pcrs.databinding.ActivityLoginBinding
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

data class LoginResponse(
    val token: String
)
interface ApiService {
    @Headers("Content-Type: application/json")
    @POST("api/children/login")
    fun login(@Body body: RequestBody): Call<LoginResponse>
    //fun login(@Body body: RequestBody): Call<String>
}

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var retrofit: Retrofit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val httpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl("https://api.fypsystem.me/")
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        binding.btnLogin.setOnClickListener {
            val userName = binding.etEmail.text.toString()
            val passWord = binding.etPassword.text.toString()

            if (userName.isEmpty() || passWord.isEmpty()) {
                Toast.makeText(this, "Username or password is empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Toast.makeText(this, "Please Wait", Toast.LENGTH_SHORT).show()
            val apiService = retrofit.create(ApiService::class.java)
            val loginBody = """
            {
                "email": "$userName",
                "password": "$passWord"
            }
            """.trimIndent().toRequestBody("application/json".toMediaType())
            val call = apiService.login(loginBody)
            call.enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful) {
                        val loginResponse = response.body()
                        val token = loginResponse?.token
                        saveAuthToken(token)
                        checkPermissions()
                       // Log.d(ContentValues.TAG, "Token: $token")
                    } else {
                        val errorBody = response.errorBody()?.string()
                        if (errorBody != null) {
                            geter()
                            Log.d("response",errorBody)
                        }
                        if (errorBody != null) {
                          /*  val jsonObject = JSONObject(errorBody)
                            val error = jsonObject.getString("error")
                            val status = jsonObject.getInt("status")
                            Log.d("Response", "Error: $error, Status: $status")*/
                        }

                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    neter()
                    Log.e(ContentValues.TAG, "Error sending app data: $t")


                }
            })
        }

        binding.forgotPw.setOnClickListener {
            val url = "http://api.pcrsyp.info/"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }
        binding.tvHaventAccount.setOnClickListener {
            val url = "http://api.pcrsyp.info/"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }
    }
    fun geter(){
        Toast.makeText(this, "Incorrect Email or Password", Toast.LENGTH_SHORT).show()
    }
    fun neter(){
        Toast.makeText(this, "Incorrect Email or Password", Toast.LENGTH_SHORT).show()

    }
    private fun saveAuthToken(token: String?) {
        val sharedPreferences = getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Clear the previous token
        editor.remove("AuthToken")

        // Save the new token if it is not null or empty
        if (!token.isNullOrEmpty()) {
            editor.putString("AuthToken", token)
            Log.d(ContentValues.TAG, "Token saved: $token")
        } else {
            Log.d(ContentValues.TAG, "Token is null or empty")
        }

        editor.apply()
    }
    fun checkPermissions(){
        Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
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
    // ... rest of your code
}
