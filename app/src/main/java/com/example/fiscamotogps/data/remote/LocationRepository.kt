package com.example.fiscamotogps.data.remote

import android.util.Log
import com.example.fiscamotogps.data.remote.model.LocationRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocationRepository(
    private val locationApi: LocationApi
) {
    private val TAG = "LocationRepository"

    suspend fun sendLocation(
        token: String,
        userId: String,
        latitude: Double,
        longitude: Double,
        accuracy: Float?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = LocationRequest(
                userId = userId,
                latitude = latitude,
                longitude = longitude,
                accuracy = accuracy
            )

            val response = locationApi.sendLocation(
                token = "Bearer $token",
                request = request
            )

            if (response.isSuccessful) {
                Log.d(TAG, "Ubicación enviada exitosamente")
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = errorBody ?: "Error ${response.code()}: ${response.message()}"
                Log.e(TAG, "Error al enviar ubicación: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción al enviar ubicación", e)
            Result.failure(e)
        }
    }
}

