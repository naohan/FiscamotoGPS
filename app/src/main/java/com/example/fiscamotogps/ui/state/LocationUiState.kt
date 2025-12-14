package com.example.fiscamotogps.ui.state

import com.example.fiscamotogps.socket.SocketConnectionState

data class LocationUiState(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracy: Float? = null,
    val isFetching: Boolean = false,
    val errorMessage: String? = null,
    val isTracking: Boolean = false,
    val isSendingContinuously: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val socketConnectionState: SocketConnectionState = SocketConnectionState.Disconnected,
    val isSocketConnected: Boolean = false,
    val isTrackingActive: Boolean = false
)


