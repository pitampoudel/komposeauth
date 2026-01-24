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
                val payload =
                    Json.parseToJsonElement(decodeAndParseJwtPayload(refreshToken)) as JsonObject
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

    fun decodeAndParseJwtPayload(token: String): String {
        val parts = token.split(".")
        if (parts.size != 3) {
            throw IllegalArgumentException("Invalid JWT token format")
        }
        val payload = parts[1]
        return payload.base64UrlDecodeToString()
    }

    private fun String.base64UrlDecodeToString(): String {
        val base64 = this
            .replace('-', '+')
            .replace('_', '/')
            .let { s ->
                val pad = (4 - s.length % 4) % 4
                s + "=".repeat(pad)
            }
        val bytes = Base64.decode(base64)
        return bytes.decodeToString()
    }

}