package pitampoudel.komposeauth.core.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import pitampoudel.core.domain.now
import kotlin.io.encoding.Base64

object JwtUtils {
    fun isJwtTokenExpired(refreshToken: String): Boolean {
        return try {
            // check if jwt
            if (refreshToken.contains(".")) {
                val payload = decodeAndParseJwtPayload(refreshToken)
                val expirationTime = payload["exp"]?.jsonPrimitive
                    ?.long?.times(1000) ?: return false
                val currentTime = now().toEpochMilliseconds()

                currentTime >= expirationTime
            } else {
                // For non jwt, we can't check expiration, assume it's not expired
                false
            }
        } catch (e: Exception) {
            // If we can't decode, assume it's not expired
            false
        }
    }

    private fun decodeAndParseJwtPayload(token: String): JsonObject {
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
}