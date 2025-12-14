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

    fun connect() {
        Log.d(TAG, "🔌 [INICIO] connect() llamado sin userId")
        Log.d(TAG, "📡 [URL] Server URL: $serverUrl")
        Log.d(TAG, "📡 [URL] Server URL length: ${serverUrl.length}")
        Log.d(TAG, "📡 [URL] Server URL starts with https: ${serverUrl.startsWith("https")}")

        if (socket?.connected() == true) {
            Log.d(TAG, "✅ Ya está conectado")
            return
        }

        try {
            Log.d(TAG, "📡 [CREANDO] Creando instancia de Socket.IO...")
            Log.d(TAG, "📡 [URL] URL exacta para Socket.IO: $serverUrl")

            // Configuración robusta de opciones
            val opts = IO.Options().apply {
                // Forzar nueva conexión
                forceNew = true

                // Habilitar reconexión automática
                reconnection = true
                reconnectionDelay = 1000
                reconnectionDelayMax = 5000
                reconnectionAttempts = Int.MAX_VALUE

                // CRÍTICO: Configurar transportes (websocket + polling como fallback)
                // Render.com a veces necesita polling primero
                transports = arrayOf("websocket", "polling")

                // Timeouts
                timeout = 20000
            }

            socket = IO.socket(serverUrl, opts)

            // === EVENTOS DE CONEXIÓN ===
            socket?.on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "✅ CONECTADO al servidor Socket.IO exitosamente")
                _connectionState.value = SocketConnectionState.Connected
                // Solicitar estado del tracking al conectar
                socket?.emit("tracking:getStatus")
            }

            socket?.on(Socket.EVENT_DISCONNECT) { args ->
                val reason = args?.firstOrNull()?.toString() ?: "desconocida"
                Log.w(TAG, "❌ DESCONECTADO del servidor. Razón: $reason")
                _connectionState.value = SocketConnectionState.Disconnected
            }

            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                val error = args?.firstOrNull()
                val errorMsg = when (error) {
                    is Exception -> error.message ?: "Error desconocido"
                    else -> error?.toString() ?: "Error desconocido"
                }
                Log.e(TAG, "❌ ERROR DE CONEXIÓN Socket.IO: $errorMsg")
                _connectionState.value = SocketConnectionState.Error(errorMsg)
            }

            // === EVENTOS DE TRACKING ===
            socket?.on("tracking:statusChanged") { args ->
                val data = args.firstOrNull() as? JSONObject
                val active = data?.optBoolean("active", false) ?: false
                _trackingActive.value = active
                Log.d(TAG, "📊 Tracking status changed: $active")
            }

            socket?.on("tracking:status") { args ->
                val data = args.firstOrNull() as? JSONObject
                val active = data?.optBoolean("active", false) ?: false
                _trackingActive.value = active
                Log.d(TAG, "📊 Tracking status: $active")
            }

            socket?.on("tracking:statusResponse") { args ->
                val data = args.firstOrNull() as? JSONObject
                val active = data?.optBoolean("active", false) ?: false
                _trackingActive.value = active
                Log.d(TAG, "📊 Tracking status response: $active")
            }

            // === EVENTOS DE UBICACIÓN (SIN DUPLICADOS) ===
            socket?.on("location:confirmed") { args ->
                val data = args.firstOrNull() as? JSONObject
                Log.d(TAG, "✅ Ubicación confirmada: ${data?.toString()}")
            }

            socket?.on("location:error") { args ->
                val data = args.firstOrNull() as? JSONObject
                val message = data?.optString("message", "Error desconocido") ?: "Error desconocido"
                Log.e(TAG, "❌ Error de ubicación del servidor: $message")
            }

            socket?.on("location:realtime") { args ->
                Log.d(TAG, "📍 Ubicación en tiempo real recibida")
            }

            socket?.on("location:allLocations") { args ->
                Log.d(TAG, "📊 Lista de ubicaciones activas recibida")
            }

            socket?.on("welcome") { args ->
                val data = args.firstOrNull() as? JSONObject
                Log.d(TAG, "👋 Mensaje de bienvenida: ${data?.toString()}")
            }

            // === EVENTOS GENÉRICOS DE ERROR ===
            socket?.on("error") { args ->
                Log.e(TAG, "❌ Error general: ${args.firstOrNull()}")
            }

            Log.d(TAG, "🚀 Iniciando conexión Socket.IO...")
            _connectionState.value = SocketConnectionState.Connecting
            socket?.connect()

        } catch (e: URISyntaxException) {
            Log.e(TAG, "❌ Error en la URL del servidor", e)
            _connectionState.value = SocketConnectionState.Error("URL inválida: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error inesperado al conectar", e)
            _connectionState.value = SocketConnectionState.Error(e.message ?: "Error desconocido")
        }
    }

    fun disconnect() {
        Log.d(TAG, "🔌 Desconectando Socket.IO...")
        socket?.disconnect()
        socket?.off()
        socket = null
        _connectionState.value = SocketConnectionState.Disconnected
        _trackingActive.value = false
    }

    fun sendLocation(userId: String, latitude: Double, longitude: Double, accuracy: Float?) {
        Log.d(TAG, "📍 [SEND] Enviando ubicación con userId: $userId")

        if (socket?.connected() != true) {
            Log.w(TAG, "⚠️ Socket no conectado, no se puede enviar ubicación")
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
            Log.d(TAG, "📤 Ubicación enviada: lat=$latitude, lon=$longitude")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al enviar ubicación", e)
        }
    }

    fun requestTrackingStatus() {
        if (socket?.connected() == true) {
            socket?.emit("tracking:getStatus")
            Log.d(TAG, "📤 Solicitando estado de tracking...")
        } else {
            Log.w(TAG, "⚠️ No conectado, no se puede solicitar estado")
        }
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