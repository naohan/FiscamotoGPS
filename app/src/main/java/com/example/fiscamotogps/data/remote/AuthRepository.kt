package com.example.fiscamotogps.data.remote

import com.example.fiscamotogps.data.DeviceInfoProvider
import com.example.fiscamotogps.data.remote.model.LoginRequest
import com.example.fiscamotogps.data.remote.model.LoginResponse
import com.example.fiscamotogps.domain.model.AuthResult
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(
    private val authApi: AuthApi,
    private val deviceInfoProvider: DeviceInfoProvider,
    private val gson: Gson = Gson()
) {

    suspend fun login(username: String, password: String): AuthResult =
        withContext(Dispatchers.IO) {
            val request = LoginRequest(
                username = username,
                password = password,
                platform = "android",
                deviceInfo = deviceInfoProvider.createPayload()
            )

            val response = authApi.login(request)
            if (response.isSuccessful) {
                val body = response.body()
                    ?: throw IllegalStateException("Respuesta vacía del servidor")
                val token = extractToken(body)
                    ?: throw IllegalStateException("El servidor no devolvió un token válido")
                val userName = extractUserName(body) ?: username
                val userId = extractUserId(body)
                val rawJson = gson.toJson(body)

                AuthResult(
                    token = token,
                    refreshToken = body.refreshToken ?: body.data?.safeString("refreshToken"),
                    userName = userName,
                    rawUserData = rawJson,
                    userId = userId
                )
            } else {
                val errorBody = response.errorBody()?.string()
                val message = errorBody?.let { extractErrorMessage(it) }
                    ?: "Error ${response.code()}: ${response.message()}"
                throw IllegalStateException(message)
            }
        }

    private fun extractToken(body: LoginResponse): String? {
        val directToken = listOfNotNull(
            body.accessTokenCamel,
            body.accessTokenSnake,
            body.token
        ).firstOrNull { !it.isNullOrBlank() }?.trim()
        if (!directToken.isNullOrBlank()) return directToken

        val dataToken = body.data?.let { data ->
            listOfNotNull(
                data.safeString("accessToken"),
                data.safeString("token"),
                data.safeString("access_token")
            ).firstOrNull { !it.isNullOrBlank() }?.trim()
        }
        if (!dataToken.isNullOrBlank()) return dataToken

        val userToken = body.user?.let { user ->
            listOfNotNull(
                user.safeString("token"),
                user.safeString("access_token"),
                user.safeString("accessToken")
            ).firstOrNull { !it.isNullOrBlank() }?.trim()
        }
        if (!userToken.isNullOrBlank()) return userToken

        return null
    }

    private fun extractUserName(body: LoginResponse): String? {
        val candidates = listOfNotNull(
            body.name,
            body.data?.safeString("name"),
            body.data?.safeString("username"),
            body.data?.safeObject("user")?.safeString("name"),
            body.data?.safeObject("user")?.safeString("username"),
            body.user?.safeString("name"),
            body.user?.safeString("username")
        )
        return candidates.firstOrNull { !it.isNullOrBlank() }?.trim()
    }

    private fun extractUserId(body: LoginResponse): String? {
        // Extraer el ID de data.id según la estructura de la respuesta
        return body.data?.safeString("id")?.trim()
    }

    private fun extractErrorMessage(raw: String): String? {
        return try {
            val json = gson.fromJson(raw, JsonObject::class.java)
            json.safeString("message") ?: json.safeString("error")
        } catch (_: Exception) {
            null
        }
    }

    private fun JsonObject.safeString(key: String): String? =
        if (has(key) && !get(key).isJsonNull) get(key).asString else null

    private fun JsonObject.safeObject(key: String): JsonObject? =
        if (has(key) && get(key).isJsonObject) get(key).asJsonObject else null
}


