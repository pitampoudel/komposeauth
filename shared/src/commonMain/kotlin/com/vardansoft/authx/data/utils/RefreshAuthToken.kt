package com.vardansoft.authx.data.utils

import com.vardansoft.authx.EndPoints.TOKEN
import com.vardansoft.authx.data.OAuth2TokenData
import com.vardansoft.authx.domain.AuthXPreferences
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.RefreshTokensParams
import io.ktor.client.request.forms.submitForm
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.http.headers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Clock
import kotlin.time.ExperimentalTime


internal suspend fun RefreshTokensParams.tryTokenRefresh(
    authUrl: String,
    clientId: String,
    client: HttpClient,
    authXPreferences: AuthXPreferences?
): BearerTokens? {
    val refreshToken = this.oldTokens?.refreshToken

    if (refreshToken.isNullOrEmpty()) {
        authXPreferences?.clear()
        return null
    }

    // Check if refresh token is expired
    if (isRefreshTokenExpired(refreshToken)) {
        authXPreferences?.clear()
        return null
    }

    val resource = safeApiCall<OAuth2TokenData> {
        client.submitForm(
            "$authUrl/$TOKEN",
            formParameters = Parameters.build {
                append("refresh_token", refreshToken)
                append("client_id", clientId)
                append("grant_type", "refresh_token")

            },
            block = {
                headers {
                    remove(HttpHeaders.Authorization)
                }
            }
        ).asResource { body() }
    }

    return when {
        resource.isSuccess -> {
            // Save new tokens
            authXPreferences?.updateTokenData(resource.getOrThrow())

            BearerTokens(
                accessToken = resource.getOrThrow().accessToken,
                refreshToken = resource.getOrThrow().refreshToken
            )
        }

        else -> null
    }


}


@OptIn(ExperimentalTime::class)
private fun isRefreshTokenExpired(refreshToken: String): Boolean {
    return try {
        // Some refresh tokens are also JWTs, others might be opaque
        if (refreshToken.contains(".")) {
            val payload = decodeJWTPayload(refreshToken)
            val expirationTime = payload["exp"]?.jsonPrimitive?.long?.times(1000) ?: return false
            val currentTime = Clock.System.now().toEpochMilliseconds()

            currentTime >= expirationTime
        } else {
            // For opaque refresh tokens, we can't check expiration
            // Let the server handle it
            false
        }
    } catch (e: Exception) {
        // If we can't decode, assume it's not expired and let server handle it
        false
    }
}

@OptIn(ExperimentalEncodingApi::class)
private fun decodeJWTPayload(token: String): JsonObject {
    val parts = token.split(".")
    if (parts.size != 3) {
        throw IllegalArgumentException("Invalid JWT token format")
    }

    val payload = parts[1]
    // Add padding if needed for Base64 decoding
    val paddedPayload = payload + "=".repeat((4 - payload.length % 4) % 4)
    val decodedBytes = Base64.UrlSafe.decode(paddedPayload.encodeToByteArray())
    val jsonString = decodedBytes.decodeToString()

    return Json.parseToJsonElement(jsonString) as JsonObject
}


