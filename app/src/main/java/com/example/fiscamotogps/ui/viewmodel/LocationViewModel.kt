package com.example.fiscamotogps.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fiscamotogps.location.LocationClient
import com.example.fiscamotogps.location.LocationWatchId
import com.example.fiscamotogps.location.hasLocationPermission
import com.example.fiscamotogps.socket.SocketConnectionState
import com.example.fiscamotogps.socket.SocketService
import com.example.fiscamotogps.ui.state.LocationUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class LocationViewModel(
    private val locationClient: LocationClient,
    private val socketService: SocketService,
    private val context: Context,
    private val userId: String,
    private val serverUrl: String = "http://localhost:4000"
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationUiState())
    val uiState: StateFlow<LocationUiState> = _uiState.asStateFlow()

    private var locationWatchId: LocationWatchId? = null

    init {
        // Conectar automáticamente al iniciar
        connectSocket()
        
        // Observar cambios en el estado del socket
        viewModelScope.launch {
            combine(
                socketService.connectionState,
                socketService.trackingActive
            ) { connectionState, trackingActive ->
                connectionState to trackingActive
            }.collect { (connectionState, trackingActive) ->
                _uiState.value = _uiState.value.copy(
                    socketConnectionState = connectionState,
                    trackingActive = trackingActive
                )
                
                // Iniciar o detener tracking automático basado en el estado
                updateTrackingState(connectionState, trackingActive)
            }
        }
        
        // Verificar permisos iniciales
        _uiState.value = _uiState.value.copy(
            hasLocationPermission = hasLocationPermission(context)
        )
    }

    fun connectSocket() {
        viewModelScope.launch {
            socketService.connect(userId)
        }
    }

    fun disconnectSocket() {
        stopTracking()
        socketService.disconnect()
    }

    fun fetchLocation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isFetching = true, errorMessage = null)
            try {
                val location = locationClient.getCurrentLocation()
                if (location != null) {
                    _uiState.value = _uiState.value.copy(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = location.accuracy,
                        isFetching = false,
                        errorMessage = null,
                        hasLocationPermission = true
                    )
                    
                    // Enviar ubicación al servidor si está conectado
                    if (socketService.isConnected()) {
                        socketService.sendLocation(
                            userId = userId,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracy = location.accuracy
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isFetching = false,
                        errorMessage = "No se pudo obtener la ubicación actual",
                        hasLocationPermission = hasLocationPermission(context)
                    )
                }
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isFetching = false,
                    errorMessage = exception.message ?: "Error al obtener la ubicación",
                    hasLocationPermission = hasLocationPermission(context)
                )
            }
        }
    }

    fun reportPermissionError() {
        _uiState.value = _uiState.value.copy(
            errorMessage = "Se requiere el permiso de ubicación para continuar",
            hasLocationPermission = false
        )
    }

    private fun updateTrackingState(
        connectionState: SocketConnectionState,
        trackingActive: Boolean
    ) {
        val shouldTrack = connectionState is SocketConnectionState.Connected && 
                         trackingActive && 
                         hasLocationPermission(context)
        
        if (shouldTrack && locationWatchId == null) {
            startTracking()
        } else if (!shouldTrack && locationWatchId != null) {
            stopTracking()
        }
    }

    private fun startTracking() {
        if (!hasLocationPermission(context)) {
            return
        }
        
        locationWatchId = locationClient.startLocationUpdates { locationData ->
            _uiState.value = _uiState.value.copy(
                latitude = locationData.latitude,
                longitude = locationData.longitude,
                accuracy = locationData.accuracy,
                isTracking = true
            )
            
            // Enviar ubicación al servidor
            if (socketService.isConnected()) {
                socketService.sendLocation(
                    userId = userId,
                    latitude = locationData.latitude,
                    longitude = locationData.longitude,
                    accuracy = locationData.accuracy
                )
            }
        }
        
        if (locationWatchId != null) {
            _uiState.value = _uiState.value.copy(isTracking = true)
        }
    }

    private fun stopTracking() {
        locationWatchId?.let { id ->
            locationClient.stopLocationUpdates(id)
            locationWatchId = null
        }
        _uiState.value = _uiState.value.copy(isTracking = false)
    }

    override fun onCleared() {
        super.onCleared()
        stopTracking()
        disconnectSocket()
    }

    class Factory(
        private val locationClient: LocationClient,
        private val socketService: SocketService,
        private val context: Context,
        private val userId: String,
        private val serverUrl: String = "http://localhost:4000"
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LocationViewModel::class.java)) {
                return LocationViewModel(locationClient, socketService, context, userId, serverUrl) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
