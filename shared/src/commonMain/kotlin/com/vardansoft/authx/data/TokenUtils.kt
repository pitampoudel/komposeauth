package com.vardansoft.authx.data

import com.vardansoft.core.domain.now
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.ExperimentalTime


@OptIn(ExperimentalTime::class)
fun isTokenExpired(refreshToken: String): Boolean {
    return try {
        // Some refresh tokens are also JWTs, others might be opaque
        if (refreshToken.contains(".")) {
            val payload = decodeJWTPayload(refreshToken)
            val expirationTime = payload["exp"]?.jsonPrimitive?.long?.times(1000) ?: return false
            val currentTime = now().toEpochMilliseconds()

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


