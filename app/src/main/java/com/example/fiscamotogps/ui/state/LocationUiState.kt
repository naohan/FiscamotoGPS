package com.example.fiscamotogps.ui.state

data class LocationUiState(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracy: Float? = null,
    val isFetching: Boolean = false,
    val errorMessage: String? = null,
    val isTracking: Boolean = false,
    val isSendingContinuously: Boolean = false,
    val hasLocationPermission: Boolean = false
)


