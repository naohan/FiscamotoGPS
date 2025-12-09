package com.example.fiscamotogps.location

import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.util.concurrent.atomic.AtomicInteger

class DefaultLocationClient(
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient
) : LocationClient {

    private val watchIdCounter = AtomicInteger(1)
    private val activeWatchers = mutableMapOf<Int, LocationCallback>()

    override suspend fun getCurrentLocation(): LocationData? =
        suspendCancellableCoroutine { continuation ->
            if (!hasLocationPermission(context)) {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            val cancellationToken = CancellationTokenSource()

            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationToken.token
            ).addOnSuccessListener { location: Location? ->
                if (!continuation.isCompleted) {
                    continuation.resume(location?.toLocationData())
                }
            }.addOnFailureListener { throwable ->
                if (!continuation.isCompleted) {
                    continuation.resumeWithException(throwable)
                }
            }

            continuation.invokeOnCancellation {
                cancellationToken.cancel()
            }
        }

    override fun startLocationUpdates(callback: (LocationData) -> Unit): LocationWatchId? {
        if (!hasLocationPermission(context)) {
            return null
        }

        val watchId = watchIdCounter.getAndIncrement()
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    callback(location.toLocationData())
                }
            }
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            15000L // 15 segundos
        ).apply {
            setMaxUpdateDelayMillis(30000L)
            setMinUpdateIntervalMillis(10000L)
        }.build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            activeWatchers[watchId] = locationCallback
            return LocationWatchId(watchId)
        } catch (e: SecurityException) {
            return null
        }
    }

    override fun stopLocationUpdates(watchId: LocationWatchId) {
        activeWatchers.remove(watchId.id)?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }

    private fun Location.toLocationData(): LocationData =
        LocationData(
            latitude = latitude,
            longitude = longitude,
            accuracy = accuracy,
            timestamp = time
        )
}


