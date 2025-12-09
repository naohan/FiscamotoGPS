package com.example.fiscamotogps.domain.model

data class AuthSession(
    val token: String,
    val refreshToken: String? = null,
    val userName: String,
    val rawUserData: String,
    val username: String,
    val password: String? = null
)


