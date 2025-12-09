package com.example.fiscamotogps.data.remote.model

data class LoginRequest(
    val username: String,
    val password: String,
    val platform: String,
    val deviceInfo: DeviceInfoPayload
)


