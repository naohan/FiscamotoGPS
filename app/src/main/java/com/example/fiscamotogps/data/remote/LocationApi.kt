package com.example.fiscamotogps.data.remote

import com.example.fiscamotogps.data.remote.model.LocationRequest
import com.example.fiscamotogps.data.remote.model.LocationResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface LocationApi {
    @POST("location/update")
    suspend fun sendLocation(
        @Header("Authorization") token: String,
        @Body request: LocationRequest
    ): Response<LocationResponse>
}

