package com.example.fiscamotogps.ui.state

import com.example.fiscamotogps.socket.SocketConnectionState

data class LocationUiState(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracy: Float? = null,
    val isFetching: Boolean = false,
    val errorMessage: String? = null,
    val socketConnectionState: SocketConnectionState = SocketConnectionState.Disconnected,
    val trackingActive: Boolean = false,
    val isTracking: Boolean = false,
    val hasLocationPermission: Boolean = false
)


