package com.example.fiscamotogps.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fiscamotogps.data.local.AuthDataStore
import com.example.fiscamotogps.data.remote.LocationRepository
import com.example.fiscamotogps.location.LocationClient
import com.example.fiscamotogps.location.LocationWatchId
import com.example.fiscamotogps.location.hasLocationPermission
import com.example.fiscamotogps.socket.SocketService
import com.example.fiscamotogps.socket.SocketConnectionState
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

    // Socket.IO service
    private val socketService = SocketService("https://backfiscamotov2.onrender.com").apply {
        Log.d("LocationViewModel", "🏗️ [CREACIÓN] SocketService creado con URL: https://backfiscamotov2.onrender.com")
    }

    // Variable para almacenar el userId dinámico
    private var currentUserId: String? = null

    init {
        // Verificar permisos iniciales
        _uiState.value = _uiState.value.copy(
            hasLocationPermission = hasLocationPermission(context)
        )

        // Observar la sesión para obtener el userId dinámicamente
        viewModelScope.launch {
            authDataStore.authSessionFlow.collect { session ->
                currentUserId = session?.userId
                Log.d("LocationViewModel", "👤 [USER ID] Actualizado userId: $currentUserId")
            }
        }

        // Observar estado de conexión Socket.IO
        viewModelScope.launch {
            socketService.connectionState.collect { connectionState ->
                _uiState.value = _uiState.value.copy(
                    socketConnectionState = connectionState,
                    isSocketConnected = connectionState is SocketConnectionState.Connected
                )
            }
        }

        // Observar estado de tracking
        viewModelScope.launch {
            socketService.trackingActive.collect { trackingActive ->
                _uiState.value = _uiState.value.copy(
                    isTrackingActive = trackingActive
                )
            }
        }
    }

    fun connectSocket() {
        Log.d("LocationViewModel", "🚀 [LLAMADA] connectSocket() llamado")
        Log.d("LocationViewModel", "✅ [LLAMADA] Llamando a socketService.connect() - sin userId necesario")
        socketService.connect()
    }

    fun disconnectSocket() {
        stopContinuousSending()
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
                        hasLocationPermission = true,
                        isTracking = true
                    )

                    // Enviar ubicación por Socket.IO solo si tenemos userId
                    currentUserId?.let { uid ->
                        socketService.sendLocation(
                            userId = uid,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracy = location.accuracy
                        )
                    } ?: run {
                        Log.w("LocationViewModel", "⚠️ No hay userId disponible, no se envía ubicación")
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "No se puede enviar ubicación: userId no disponible"
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

    fun startContinuousSending() {
        if (isSendingContinuously) return

        if (!hasLocationPermission(context)) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Se requiere permiso de ubicación para enviar datos"
            )
            return
        }

        // Verificar que tengamos userId
        if (currentUserId == null) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "No se puede iniciar envío: userId no disponible. Por favor, reinicia la sesión."
            )
            Log.e("LocationViewModel", "❌ No hay userId disponible para iniciar envío continuo")
            return
        }

        isSendingContinuously = true
        _uiState.value = _uiState.value.copy(isSendingContinuously = true)

        // Conectar Socket.IO si no está conectado
        if (!socketService.isConnected()) {
            connectSocket()
        }

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

                        // Enviar por Socket.IO solo si tenemos userId
                        currentUserId?.let { uid ->
                            socketService.sendLocation(
                                userId = uid,
                                latitude = location.latitude,
                                longitude = location.longitude,
                                accuracy = location.accuracy
                            )
                        } ?: Log.w("LocationViewModel", "⚠️ No hay userId disponible, omitiendo envío de ubicación")
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

            // Si está enviando continuamente, enviar por Socket.IO solo si tenemos userId
            if (isSendingContinuously) {
                currentUserId?.let { uid ->
                    socketService.sendLocation(
                        userId = uid,
                        latitude = locationData.latitude,
                        longitude = locationData.longitude,
                        accuracy = locationData.accuracy
                    )
                } ?: Log.w("LocationViewModel", "⚠️ No hay userId disponible en tracking continuo")
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
