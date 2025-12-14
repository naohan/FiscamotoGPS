package com.example.fiscamotogps.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.LaunchedEffect
import com.example.fiscamotogps.location.hasLocationPermission
import com.example.fiscamotogps.ui.state.LocationUiState

@Composable
fun LocationScreen(
    userName: String,
    locationState: LocationUiState,
    onRequestLocation: () -> Unit,
    onPermissionDenied: () -> Unit,
    onStartContinuousSending: () -> Unit,
    onStopContinuousSending: () -> Unit,
    onLogout: () -> Unit,
    onConnectSocket: () -> Unit = {}
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            // Permiso otorgado, solicitar ubicación
            onRequestLocation()
        } else {
            // Permiso denegado
            onPermissionDenied()
        }
    }

    fun handleLocationRequest() {
        if (hasLocationPermission(context)) {
            onRequestLocation()
        } else {
            // Solicitar permisos si no están otorgados
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
    
    // Conectar Socket.IO automáticamente al cargar la pantalla
    LaunchedEffect(Unit) {
        onConnectSocket()
    }

    // Solicitar permisos automáticamente al cargar la pantalla si no están otorgados
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (!hasLocationPermission(context)) {
            // No solicitar automáticamente, solo mostrar mensaje
            // El usuario debe hacer clic en "Obtener Ubicación Manual"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🚴‍♂️ Conductor: $userName",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Estado de Conexión Socket.IO
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    locationState.isSocketConnected -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (socketColor, socketText) = when {
                        locationState.isSocketConnected -> Color(0xFF4CAF50) to "🟢 Conectado al Servidor (Socket.IO)"
                        locationState.socketConnectionState is com.example.fiscamotogps.socket.SocketConnectionState.Connecting ->
                            Color(0xFF2196F3) to "🔵 Conectando al servidor..."
                        locationState.socketConnectionState is com.example.fiscamotogps.socket.SocketConnectionState.Error ->
                            Color(0xFFF44336) to "🔴 Error de conexión"
                        else -> Color(0xFF9E9E9E) to "⚪ Desconectado del servidor"
                    }

                    Canvas(
                        modifier = Modifier.size(12.dp)
                    ) {
                        drawCircle(color = socketColor, radius = size.minDimension / 2f)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = socketText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (locationState.socketConnectionState is com.example.fiscamotogps.socket.SocketConnectionState.Error) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Error: ${(locationState.socketConnectionState as com.example.fiscamotogps.socket.SocketConnectionState.Error).message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Estado de Tracking GPS
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    locationState.isSendingContinuously -> MaterialTheme.colorScheme.tertiaryContainer
                    locationState.isTracking -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (trackColor, trackText) = when {
                        locationState.isSendingContinuously -> Color(0xFF4CAF50) to "🟢 Enviando GPS al Servidor"
                        locationState.isTracking -> Color(0xFF2196F3) to "🔵 GPS Activo"
                        !locationState.hasLocationPermission ->
                            Color(0xFFFF9800) to "⚠️ Esperando Permiso GPS"
                        else -> Color(0xFF9E9E9E) to "⚪ GPS Inactivo"
                    }

                    Canvas(
                        modifier = Modifier.size(12.dp)
                    ) {
                        drawCircle(color = trackColor, radius = size.minDimension / 2f)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = trackText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (locationState.isSendingContinuously) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Enviando ubicación por Socket.IO cada 15 segundos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                } else if (locationState.isTracking) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "GPS activo y listo para enviar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Última Ubicación
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "📍 Última geolocalización",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (locationState.latitude != null && locationState.longitude != null) {
                    Text(
                        text = "Latitud: ${String.format("%.6f", locationState.latitude)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Longitud: ${String.format("%.6f", locationState.longitude)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    locationState.accuracy?.let { accuracy ->
                        Text(
                            text = "Precisión: ±${String.format("%.0f", accuracy)} metros",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    Text(
                        text = "No hay datos disponibles",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { handleLocationRequest() },
            enabled = !locationState.isFetching && hasLocationPermission(context),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (locationState.isFetching) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Obteniendo ubicación...")
            } else {
                Text("📍 Obtener Ubicación Manual")
            }
        }

        if (!locationState.hasLocationPermission) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "⚠️ Se requiere permiso de ubicación para activar GPS",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Botón de Envío Continuo
        if (locationState.isSendingContinuously) {
            Button(
                onClick = { onStopContinuousSending() },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("⏹️ Detener Envío Continuo")
            }
        } else {
            Button(
                onClick = { 
                    if (hasLocationPermission(context)) {
                        onStartContinuousSending()
                    } else {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                enabled = locationState.hasLocationPermission,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("▶️ Iniciar Envío Continuo al Servidor")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Cerrar sesión")
        }

        if (locationState.errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = locationState.errorMessage,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
