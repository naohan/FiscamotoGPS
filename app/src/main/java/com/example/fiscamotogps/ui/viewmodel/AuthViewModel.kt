package com.example.fiscamotogps.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fiscamotogps.data.local.AuthDataStore
import com.example.fiscamotogps.data.remote.AuthRepository
import com.example.fiscamotogps.domain.model.AuthSession
import com.example.fiscamotogps.ui.state.AuthUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository,
    private val authDataStore: AuthDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        observeSession()
    }

    fun onUsernameChanged(value: String) {
        _uiState.update { it.copy(username = value) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value) }
    }

    fun login() {
        val username = _uiState.value.username.trim()
        val password = _uiState.value.password

        if (username.isBlank() || password.isBlank()) {
            _uiState.update {
                it.copy(errorMessage = "Ingrese usuario y contraseña válidos")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val result = repository.login(username, password)
                val session = AuthSession(
                    token = result.token,
                    refreshToken = result.refreshToken,
                    userName = result.userName,
                    rawUserData = result.rawUserData,
                    username = username,
                    password = password
                )
                authDataStore.saveSession(session)
                _uiState.update { it.copy(isLoading = false) }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Error desconocido"
                    )
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authDataStore.clear()
        }
    }

    private fun observeSession() {
        viewModelScope.launch {
            authDataStore.authSessionFlow.collectLatest { session ->
                if (session == null) {
                    _uiState.update {
                        it.copy(
                            isLoggedIn = false,
                            userName = "",
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoggedIn = true,
                            userName = session.userName.ifBlank { session.username },
                            username = session.username,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
            }
        }
    }

    class Factory(
        private val repository: AuthRepository,
        private val authDataStore: AuthDataStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                return AuthViewModel(repository, authDataStore) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}


