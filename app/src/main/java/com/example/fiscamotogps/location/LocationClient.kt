package com.example.fiscamotogps.location

interface LocationClient {
    suspend fun getCurrentLocation(): LocationData?
    fun startLocationUpdates(callback: (LocationData) -> Unit): LocationWatchId?
    fun stopLocationUpdates(watchId: LocationWatchId)
}

data class LocationWatchId(val id: Int)


