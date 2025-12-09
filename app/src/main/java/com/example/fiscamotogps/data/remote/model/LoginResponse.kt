package com.example.fiscamotogps.data.remote.model

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("token") val token: String? = null,
    @SerializedName("access_token") val accessTokenSnake: String? = null,
    @SerializedName("accessToken") val accessTokenCamel: String? = null,
    @SerializedName("refreshToken") val refreshToken: String? = null,
    @SerializedName("data") val data: JsonObject? = null,
    @SerializedName("user") val user: JsonObject? = null,
    @SerializedName("name") val name: String? = null
)


