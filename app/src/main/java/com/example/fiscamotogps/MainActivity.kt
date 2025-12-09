package com.example.fiscamotogps

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fiscamotogps.data.DeviceInfoProvider
import com.example.fiscamotogps.data.local.AuthDataStore
import com.example.fiscamotogps.data.remote.AuthApi
import com.example.fiscamotogps.data.remote.AuthRepository
import com.example.fiscamotogps.location.DefaultLocationClient
import com.example.fiscamotogps.location.LocationClient
import com.example.fiscamotogps.ui.screens.LocationScreen
import com.example.fiscamotogps.ui.screens.LoginScreen
import com.example.fiscamotogps.ui.theme.FiscamotoGPSTheme
import com.example.fiscamotogps.socket.SocketService
import com.example.fiscamotogps.ui.viewmodel.AuthViewModel
import com.example.fiscamotogps.ui.viewmodel.LocationViewModel
import com.google.android.gms.location.LocationServices
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : ComponentActivity() {

    private val authDataStore by lazy { AuthDataStore(applicationContext) }
    private val authRepository by lazy {
        AuthRepository(
            authApi = provideAuthApi(),
            deviceInfoProvider = DeviceInfoProvider(applicationContext)
        )
    }
    private val locationClient: LocationClient by lazy {
        DefaultLocationClient(
            applicationContext,
            LocationServices.getFusedLocationProviderClient(this)
        )
    }
    
    private val socketService: SocketService by lazy {
        // Cambiar esta URL por la URL real de tu servidor Socket.IO
        SocketService("http://localhost:4000")
    }

    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModel.Factory(authRepository, authDataStore)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FiscamotoGPSTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FiscamotoGpsApp(
                        authViewModel = authViewModel,
                        locationClient = locationClient,
                        socketService = socketService,
                        context = this@MainActivity
                    )
                }
            }
        }
    }

    private fun provideAuthApi(): AuthApi {
        val isDebug = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val logging = HttpLoggingInterceptor().apply {
            level = if (isDebug) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.BASIC
            }
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val gson = GsonBuilder()
            .setLenient()
            .create()

        return Retrofit.Builder()
            .baseUrl("https://backfiscamotov2.onrender.com/api/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(AuthApi::class.java)
    }
}

@Composable
fun FiscamotoGpsApp(
    authViewModel: AuthViewModel,
    locationClient: LocationClient,
    socketService: SocketService,
    context: android.content.Context
) {
    val authState = authViewModel.uiState.collectAsStateWithLifecycle().value
    val locationViewModel = viewModel<LocationViewModel>(
        factory = LocationViewModel.Factory(
            locationClient = locationClient,
            socketService = socketService,
            context = context,
            userId = authState.username.ifBlank { authState.userName },
            serverUrl = "http://localhost:4000" // Cambiar por la URL real
        )
    )
    val locationState = locationViewModel.uiState.collectAsStateWithLifecycle().value

    if (authState.isLoggedIn) {
        LocationScreen(
            userName = authState.userName.ifBlank { authState.username },
            locationState = locationState,
            onRequestLocation = { locationViewModel.fetchLocation() },
            onPermissionDenied = { locationViewModel.reportPermissionError() },
            onConnectSocket = { locationViewModel.connectSocket() },
            onDisconnectSocket = { locationViewModel.disconnectSocket() },
            onLogout = { 
                locationViewModel.disconnectSocket()
                authViewModel.logout()
            }
        )
    } else {
        LoginScreen(
            state = authState,
            onUsernameChange = authViewModel::onUsernameChanged,
            onPasswordChange = authViewModel::onPasswordChanged,
            onLoginClick = authViewModel::login
        )
    }
}