package com.example.fiscamotogps.socket

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.net.URISyntaxException

class SocketService(private val serverUrl: String) {
    
    private var socket: Socket? = null
    private val _connectionState = MutableStateFlow<SocketConnectionState>(SocketConnectionState.Disconnected)
    val connectionState: StateFlow<SocketConnectionState> = _connectionState.asStateFlow()
    
    private val _trackingActive = MutableStateFlow(false)
    val trackingActive: StateFlow<Boolean> = _trackingActive.asStateFlow()
    
    private val TAG = "SocketService"
    
    fun connect(userId: String) {
        if (socket?.connected() == true) {
            Log.d(TAG, "Ya está conectado")
            return
        }
        
        try {
            val options = IO.Options().apply {
                reconnection = true
                timeout = 20000
            }
            
            socket = IO.socket(serverUrl, options)
            
            socket?.on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "Conectado al servidor")
                _connectionState.value = SocketConnectionState.Connected
                // Solicitar estado del tracking al conectar
                socket?.emit("tracking:getStatus")
            }
            
            socket?.on(Socket.EVENT_DISCONNECT) { args ->
                Log.d(TAG, "Desconectado: ${args?.firstOrNull()}")
                _connectionState.value = SocketConnectionState.Disconnected
            }
            
            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e(TAG, "Error de conexión: ${args?.firstOrNull()}")
                _connectionState.value = SocketConnectionState.Error(
                    args?.firstOrNull()?.toString() ?: "Error desconocido"
                )
            }
            
            socket?.on("tracking:statusChanged") { args ->
                val data = args.firstOrNull() as? JSONObject
                val active = data?.optBoolean("active", false) ?: false
                _trackingActive.value = active
                Log.d(TAG, "Tracking status changed: $active")
            }
            
            socket?.on("tracking:status") { args ->
                val data = args.firstOrNull() as? JSONObject
                val active = data?.optBoolean("active", false) ?: false
                _trackingActive.value = active
                Log.d(TAG, "Tracking status: $active")
            }
            
            socket?.on("tracking:statusResponse") { args ->
                val data = args.firstOrNull() as? JSONObject
                val active = data?.optBoolean("active", false) ?: false
                _trackingActive.value = active
                Log.d(TAG, "Tracking status response: $active")
            }
            
            socket?.on("location:confirmed") { args ->
                val data = args.firstOrNull() as? JSONObject
                Log.d(TAG, "Ubicación confirmada: ${data?.toString()}")
            }
            
            socket?.on("location:error") { args ->
                val data = args.firstOrNull() as? JSONObject
                val message = data?.optString("message", "Error desconocido") ?: "Error desconocido"
                Log.e(TAG, "Error de ubicación: $message")
            }
            
            socket?.on("welcome") { args ->
                val data = args.firstOrNull() as? JSONObject
                Log.d(TAG, "Mensaje de bienvenida: ${data?.toString()}")
            }
            
            socket?.connect()
            _connectionState.value = SocketConnectionState.Connecting
            
        } catch (e: URISyntaxException) {
            Log.e(TAG, "Error en la URL del servidor", e)
            _connectionState.value = SocketConnectionState.Error("URL inválida: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error al conectar", e)
            _connectionState.value = SocketConnectionState.Error(e.message ?: "Error desconocido")
        }
    }
    
    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
        _connectionState.value = SocketConnectionState.Disconnected
        _trackingActive.value = false
    }
    
    fun sendLocation(userId: String, latitude: Double, longitude: Double, accuracy: Float?) {
        if (socket?.connected() != true) {
            Log.w(TAG, "No hay conexión, no se puede enviar ubicación")
            return
        }
        
        try {
            val locationData = JSONObject().apply {
                put("userId", userId)
                put("latitude", latitude)
                put("longitude", longitude)
                put("timestamp", System.currentTimeMillis())
                accuracy?.let { put("accuracy", it) }
            }
            
            socket?.emit("location:update", locationData)
            Log.d(TAG, "Ubicación enviada: $locationData")
        } catch (e: Exception) {
            Log.e(TAG, "Error al enviar ubicación", e)
        }
    }
    
    fun requestTrackingStatus() {
        socket?.emit("tracking:getStatus")
    }
    
    fun isConnected(): Boolean {
        return socket?.connected() == true
    }
}

sealed class SocketConnectionState {
    object Disconnected : SocketConnectionState()
    object Connecting : SocketConnectionState()
    object Connected : SocketConnectionState()
    data class Error(val message: String) : SocketConnectionState()
}

