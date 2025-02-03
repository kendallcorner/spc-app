package com.google.firebase.quickstart.fcm.kotlin

import android.util.Log
import android.widget.Toast
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Data classes for API
data class AppData(
    val phone_id: String,
    val fcm_token: String,
    val lat: Double,
    val lon: Double
)

data class InputAppData(
    val fcm_token: String,
    val lat: Optional[Double],
    val lon: Optional[Double]
)

// API interface
interface ApiService {
    @POST("/test/app-data")
    fun sendAppData(@Body data: AppData): Call<Unit>
}

object SpcDbApi {
    private const val TAG = "SpcDbApi"
    
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://bmorlmhe80.execute-api.us-east-2.amazonaws.com")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService = retrofit.create(ApiService::class.java)

    // Get or generate a unique device ID
    private fun getDeviceUniqueId(): String {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val existingId = prefs.getString("device_id", null)

        return if (existingId != null) {
            existingId
        } else {
            val newId = UUID.randomUUID().toString()
            prefs.edit().putString("device_id", newId).apply()
            newId
        }
    }

    fun getTokenAndSendData(lat: Double, lon: Double, onSuccess: () -> Unit, onError: (String) -> Unit) {
        // Get FCM token and send data
        Firebase.messaging.token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                val deviceId = getDeviceUniqueId()

                val appData = AppData(
                    phone_id = deviceId,
                    fcm_token = token,
                    lat = location.latitude,
                    lon = location.longitude
                )

                this.sendDataToApi(
                    appData = appData,
                    onSuccess = onSuccess,
                    onError = onError
                )
            } else {
                onError("Error getting FCM token")
            }
    }

    fun sendDataToApi(
        appData: AppData,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        apiService.sendAppData(appData).enqueue(object : Callback<Unit> {
            override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    onError("Failed to send data: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<Unit>, t: Throwable) {
                Log.e(TAG, "API call failed", t)
                onError("Error sending data: ${t.message}")
            }
        })
    }
}