package com.example.fiscamotogps.data.remote.model

data class LocationRequest(
    val userId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null,
    val timestamp: Long = System.currentTimeMillis()
)

