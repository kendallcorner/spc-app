package com.kendall.spctest

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging
import com.kendall.spctest.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.UUID

// Data classes for API
data class AppData(
    val phone_id: String,
    val fcm_token: String,
    val lat: Double?,
    val lon: Double?
)

// API interface
interface ApiService {
    @POST("/test/app-data")
    fun sendAppData(@Body data: AppData): Call<Unit>
}

object SpcDbApi {
    private const val TAG = "SpcDbApi"

    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val requestWithHeader = originalRequest.newBuilder()
           .header("Authorization", BuildConfig.SPC_DB_API_KEY)
            .build()
        chain.proceed(requestWithHeader)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://bmorlmhe80.execute-api.us-east-2.amazonaws.com")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService = retrofit.create(ApiService::class.java)

    // Get or generate a unique device ID
    private fun getDeviceUniqueId(context: Context): String {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val existingId = prefs.getString("device_id", null)

        return if (existingId != null) {
            existingId
        } else {
            val newId = UUID.randomUUID().toString()
            prefs.edit().putString("device_id", newId).apply()
            newId
        }
    }

    fun getTokenAndSendData(context: Context, lat: Double?, lon: Double?, onSuccess: () -> Unit, onError: (String) -> Unit) {
        // Get FCM token and send data
        Firebase.messaging.token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                val deviceId = getDeviceUniqueId(context)

                val appData = AppData(
                    phone_id = deviceId,
                    fcm_token = token,
                    lat = lat,
                    lon = lon
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
    }

    private fun sendDataToApi(
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
