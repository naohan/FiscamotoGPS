package com.example.fiscamotogps.data.remote

import com.example.fiscamotogps.data.remote.model.LoginRequest
import com.example.fiscamotogps.data.remote.model.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/signin")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
}


