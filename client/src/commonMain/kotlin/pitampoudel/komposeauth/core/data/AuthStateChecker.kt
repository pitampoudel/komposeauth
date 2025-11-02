package pitampoudel.komposeauth.core.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.cookies.cookies
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import pitampoudel.core.data.asResource
import pitampoudel.core.data.safeApiCall
import pitampoudel.core.domain.Result
import pitampoudel.core.domain.now
import pitampoudel.komposeauth.data.ApiEndpoints.LOGIN
import pitampoudel.komposeauth.data.Credential
import pitampoudel.komposeauth.data.ProfileResponse
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.ExperimentalTime

internal class AuthStateChecker(val httpClient: HttpClient, val authUrl: String) {
    suspend fun isLoggedIn(): Boolean {
        val cookies = httpClient.cookies(authUrl)
        val access = cookies.find { it.name == "ACCESS_TOKEN" }
        val refresh = cookies.find { it.name == "REFRESH_TOKEN" }

        // No cookies at all → definitely logged out
        if (access == null && refresh == null) return false

        if (access == null || isJwtExpired(access.value))
            return refreshSession(httpClient, authUrl)

        return true
    }

    suspend fun refreshSession(
        client: HttpClient,
        url: String
    ): Boolean {
        val result = safeApiCall<ProfileResponse> {
            client.post(
                "$url/$LOGIN",
                block = {
                    setBody(Credential.RefreshToken(null) as Credential)
                }
            ).asResource { body() }
        }
        return result is Result.Success
    }

    @OptIn(ExperimentalTime::class)
    private fun isJwtExpired(refreshToken: String): Boolean {
        return try {
            // Some tokens are also JWTs, others might be opaque
            if (refreshToken.contains(".")) {
                val payload = decodeAndParseJwtPayload(refreshToken)
                val expirationTime =
                    payload["exp"]?.jsonPrimitive?.long?.times(1000) ?: return false
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