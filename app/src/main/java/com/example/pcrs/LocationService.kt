package com.example.pcrs

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

class LocationService : Service() {
    data class LocationData(
        val latitude: Double,
        val longitude: Double
    )
    interface ApiService {
        @POST("api/location") // Replace "your-endpoint" with the actual endpoint URL
        fun sendLocation(@Body locationData: LocationData): Call<Unit>
    }
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var handler: Handler

    private val locationRequest: LocationRequest = LocationRequest.create().apply {
        interval = THREE_HOURS
        fastestInterval = THREE_HOURS
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    private val getLocationRunnable = object : Runnable {
        override fun run() {
            getLastLocation()
            handler.postDelayed(this, THREE_HOURS)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                p0 ?: return
                for (location in p0.locations) {
                    // Do something with the location
                    Log.d(TAG, "Location: ${location.latitude}, ${location.longitude}")
                    sendLocation(location.latitude,location.longitude)
                }
            }

           /* override fun onLocationResult(locationResult: LocationResult?) {

            }*/
        }

        handler = Handler(Looper.getMainLooper())
        handler.post(getLocationRunnable)

        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        if (checkPermissions()) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    private fun checkPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(getLocationRunnable)
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    companion object {
        private const val TAG = "LocationService"
        private const val THREE_HOURS: Long = 3 * 60 * 60 * 1000 // 3 hours in milliseconds
    }
     val BASE_URL = "https://api.fypsystem.me/" // Replace with your API base URL

    // Inside the LocationService class
    private fun sendLocation(latitude: Double, longitude: Double) {
        val locationData = LocationData(latitude, longitude)
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
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)
        val call = apiService.sendLocation(locationData)

        call.enqueue(object : Callback<Unit> {
            override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                if (response.isSuccessful) {
                    Log.d(TAG, "Location sent successfully")
                } else {
                    Log.e(TAG, "Failed to send location. Error code: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<Unit>, t: Throwable) {
                Log.e(TAG, "Failed to send location: ${t.message}")
            }
        })
    }
}
