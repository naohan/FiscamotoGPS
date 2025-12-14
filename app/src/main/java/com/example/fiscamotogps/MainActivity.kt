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
import com.example.fiscamotogps.data.remote.LocationApi
import com.example.fiscamotogps.data.remote.LocationRepository
import com.example.fiscamotogps.location.DefaultLocationClient
import com.example.fiscamotogps.location.LocationClient
import com.example.fiscamotogps.ui.screens.LocationScreen
import com.example.fiscamotogps.ui.screens.LoginScreen
import com.example.fiscamotogps.ui.theme.FiscamotoGPSTheme
import com.example.fiscamotogps.ui.viewmodel.AuthViewModel
import com.example.fiscamotogps.ui.viewmodel.LocationViewModel
import com.google.android.gms.location.LocationServices
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.provider.Settings

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
                        locationRepository = LocationRepository(provideLocationApi()),
                        authDataStore = authDataStore,
                        context = this@MainActivity
                    )
                }
            }
        }
    }

    private val retrofit: Retrofit by lazy {
        val isDebug = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        
        // Obtener Device ID
        val deviceId = Settings.Secure.getString(
            applicationContext.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown"
        
        // Interceptor para agregar el header X-Device-Id
        val deviceIdInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val newRequest = originalRequest.newBuilder()
                .header("X-Device-Id", deviceId)
                .build()
            chain.proceed(newRequest)
        }
        
        val logging = HttpLoggingInterceptor().apply {
            level = if (isDebug) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.BASIC
            }
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(deviceIdInterceptor) // Agregar device ID primero
            .addInterceptor(logging) // Luego logging
            // Configurar timeouts más largos para servidores en la nube (Render)
            .connectTimeout(60, TimeUnit.SECONDS) // Tiempo para establecer conexión
            .readTimeout(60, TimeUnit.SECONDS) // Tiempo para leer respuesta
            .writeTimeout(60, TimeUnit.SECONDS) // Tiempo para escribir request
            .build()

        val gson = GsonBuilder()
            .setLenient()
            .create()

        Retrofit.Builder()
            .baseUrl("https://backfiscamotov2.onrender.com/api/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    private fun provideAuthApi(): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }

    private fun provideLocationApi(): LocationApi {
        return retrofit.create(LocationApi::class.java)
    }
}

@Composable
fun FiscamotoGpsApp(
    authViewModel: AuthViewModel,
    locationClient: LocationClient,
    locationRepository: LocationRepository,
    authDataStore: AuthDataStore,
    context: android.content.Context
) {
    val authState = authViewModel.uiState.collectAsStateWithLifecycle().value
    
    val locationViewModel = viewModel<LocationViewModel>(
        factory = LocationViewModel.Factory(
            locationClient = locationClient,
            locationRepository = locationRepository,
            authDataStore = authDataStore,
            context = context,
            userId = authState.username.ifBlank { authState.userName }
        )
    )
    val locationState = locationViewModel.uiState.collectAsStateWithLifecycle().value

    if (authState.isLoggedIn) {
        LocationScreen(
            userName = authState.userName.ifBlank { authState.username },
            locationState = locationState,
            onRequestLocation = { locationViewModel.fetchLocation() },
            onPermissionDenied = { locationViewModel.reportPermissionError() },
            onStartContinuousSending = { locationViewModel.startContinuousSending() },
            onStopContinuousSending = { locationViewModel.stopContinuousSending() },
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