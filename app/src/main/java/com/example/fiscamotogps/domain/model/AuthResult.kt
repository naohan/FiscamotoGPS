package com.example.fiscamotogps.domain.model

data class AuthResult(
    val token: String,
    val refreshToken: String?,
    val userName: String,
    val rawUserData: String
)


