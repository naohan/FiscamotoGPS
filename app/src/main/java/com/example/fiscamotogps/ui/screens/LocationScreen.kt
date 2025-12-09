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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import com.example.fiscamotogps.location.hasLocationPermission
import com.example.fiscamotogps.socket.SocketConnectionState
import com.example.fiscamotogps.ui.state.LocationUiState

@Composable
fun LocationScreen(
    userName: String,
    locationState: LocationUiState,
    onRequestLocation: () -> Unit,
    onPermissionDenied: () -> Unit,
    onConnectSocket: () -> Unit,
    onDisconnectSocket: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            onRequestLocation()
        } else {
            onPermissionDenied()
        }
    }

    fun handleLocationRequest() {
        if (hasLocationPermission(context)) {
            onRequestLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
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
                containerColor = when (locationState.socketConnectionState) {
                    is SocketConnectionState.Connected -> MaterialTheme.colorScheme.primaryContainer
                    is SocketConnectionState.Connecting -> MaterialTheme.colorScheme.secondaryContainer
                    is SocketConnectionState.Error -> MaterialTheme.colorScheme.errorContainer
                    is SocketConnectionState.Disconnected -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Indicador de estado
                val (statusColor, statusText) = when (locationState.socketConnectionState) {
                    is SocketConnectionState.Connected -> Color(0xFF4CAF50) to "Conectado"
                    is SocketConnectionState.Connecting -> Color(0xFFFF9800) to "Conectando..."
                    is SocketConnectionState.Error -> Color(0xFFF44336) to "Error"
                    is SocketConnectionState.Disconnected -> Color(0xFF9E9E9E) to "Desconectado"
                }
                
                Canvas(
                    modifier = Modifier.size(12.dp)
                ) {
                    drawCircle(color = statusColor, radius = size.minDimension / 2f)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Servidor: $statusText",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                
                if (locationState.socketConnectionState is SocketConnectionState.Error) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = locationState.socketConnectionState.message,
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
                    locationState.isTracking -> MaterialTheme.colorScheme.tertiaryContainer
                    locationState.trackingActive -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (trackColor, trackText) = when {
                        locationState.isTracking -> Color(0xFF4CAF50) to "🟢 Enviando GPS Activamente"
                        locationState.trackingActive && !locationState.hasLocationPermission -> 
                            Color(0xFFFF9800) to "⚠️ Esperando Permiso GPS"
                        locationState.trackingActive -> 
                            Color(0xFF2196F3) to "🔵 Tracking Activado"
                        else -> Color(0xFF9E9E9E) to "⚪ Tracking Desactivado"
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
                
                if (locationState.isTracking) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Enviando ubicación cada 15 segundos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
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

        // Botones de Control
        when (locationState.socketConnectionState) {
            is SocketConnectionState.Connected -> {
                Button(
                    onClick = { onDisconnectSocket() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Desconectar del Servidor")
                }
            }
            is SocketConnectionState.Connecting -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Conectando...")
                }
            }
            else -> {
                Button(
                    onClick = { onConnectSocket() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Conectar al Servidor")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

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
