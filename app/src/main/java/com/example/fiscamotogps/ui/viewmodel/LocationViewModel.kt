package com.example.fiscamotogps.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fiscamotogps.data.local.AuthDataStore
import com.example.fiscamotogps.data.remote.LocationRepository
import com.example.fiscamotogps.location.LocationClient
import com.example.fiscamotogps.location.LocationWatchId
import com.example.fiscamotogps.location.hasLocationPermission
import com.example.fiscamotogps.ui.state.LocationUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class LocationViewModel(
    private val locationClient: LocationClient,
    private val locationRepository: LocationRepository,
    private val authDataStore: AuthDataStore,
    private val context: Context,
    private val userId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationUiState())
    val uiState: StateFlow<LocationUiState> = _uiState.asStateFlow()

    private var locationWatchId: LocationWatchId? = null
    private var continuousSendingJob: Job? = null
    private var isSendingContinuously = false

    init {
        // Verificar permisos iniciales
        _uiState.value = _uiState.value.copy(
            hasLocationPermission = hasLocationPermission(context)
        )
    }

    fun connectSocket() {
        // Socket.IO removido - solo se usa el backend
    }

    fun disconnectSocket() {
        stopContinuousSending()
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
                    
                    // Enviar ubicación al backend
                    sendLocationToBackend(location.latitude, location.longitude, location.accuracy)
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

    fun startContinuousSending() {
        if (isSendingContinuously) return
        
        if (!hasLocationPermission(context)) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Se requiere permiso de ubicación para enviar datos"
            )
            return
        }

        isSendingContinuously = true
        _uiState.value = _uiState.value.copy(isSendingContinuously = true)
        
        // Iniciar tracking de ubicación
        startTracking()
        
        // Iniciar envío periódico
        continuousSendingJob = viewModelScope.launch {
            while (isSendingContinuously) {
                try {
                    val location = locationClient.getCurrentLocation()
                    if (location != null) {
                        _uiState.value = _uiState.value.copy(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracy = location.accuracy
                        )
                        sendLocationToBackend(location.latitude, location.longitude, location.accuracy)
                    }
                } catch (e: Exception) {
                    // Continuar intentando aunque falle una vez
                }
                delay(15000) // Enviar cada 15 segundos
            }
        }
    }

    fun stopContinuousSending() {
        isSendingContinuously = false
        _uiState.value = _uiState.value.copy(isSendingContinuously = false)
        continuousSendingJob?.cancel()
        continuousSendingJob = null
        stopTracking()
    }

    private suspend fun sendLocationToBackend(
        latitude: Double,
        longitude: Double,
        accuracy: Float?
    ) {
        try {
            val session = authDataStore.authSessionFlow.firstOrNull()
            val token = session?.token
            
            if (token == null) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "No hay sesión activa. Por favor, inicia sesión nuevamente."
                )
                return
            }

            val result = locationRepository.sendLocation(
                token = token,
                userId = userId,
                latitude = latitude,
                longitude = longitude,
                accuracy = accuracy
            )

            result.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error al enviar ubicación: ${exception.message}"
                )
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Error al enviar ubicación: ${e.message}"
            )
        }
    }

    fun reportPermissionError() {
        _uiState.value = _uiState.value.copy(
            errorMessage = "Se requiere el permiso de ubicación para continuar",
            hasLocationPermission = false
        )
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
            
            // Si está enviando continuamente, enviar al backend
            if (isSendingContinuously) {
                viewModelScope.launch {
                    sendLocationToBackend(
                        locationData.latitude,
                        locationData.longitude,
                        locationData.accuracy
                    )
                }
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
        stopContinuousSending()
    }

    class Factory(
        private val locationClient: LocationClient,
        private val locationRepository: LocationRepository,
        private val authDataStore: AuthDataStore,
        private val context: Context,
        private val userId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LocationViewModel::class.java)) {
                return LocationViewModel(
                    locationClient,
                    locationRepository,
                    authDataStore,
                    context,
                    userId
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
