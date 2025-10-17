package com.vardansoft.komposeauth.ui.core.data

import com.vardansoft.core.data.asResource
import com.vardansoft.core.data.safeApiCall
import com.vardansoft.core.domain.Result
import com.vardansoft.core.domain.now
import com.vardansoft.komposeauth.data.ApiEndpoints.TOKEN
import com.vardansoft.komposeauth.data.Credential
import com.vardansoft.komposeauth.data.OAuth2TokenData
import com.vardansoft.komposeauth.ui.core.domain.AuthPreferences
import com.vardansoft.komposeauth.ui.core.domain.KtorBearerHandler
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.AuthConfig
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.ExperimentalTime

internal class KtorBearerHandlerImpl internal constructor(
    private val authPreferences: AuthPreferences,
    override val authUrl: String,
    override val serverUrls: List<String>
) : KtorBearerHandler {
    val hosts = (serverUrls + authUrl).map {
        Url(it).host
    }

    override fun configure(auth: AuthConfig) {
        auth.bearer {
            loadTokens {
                authPreferences.oAuth2TokenData()?.let {
                    BearerTokens(
                        accessToken = it.accessToken,
                        refreshToken = it.refreshToken
                    )
                }
            }

            refreshTokens {
                val refreshToken = this.oldTokens?.refreshToken

                if (refreshToken.isNullOrEmpty() || isJwtTokenExpired(refreshToken)) {
                    authPreferences.clear()
                    return@refreshTokens null
                }

                val result = safeApiCall<OAuth2TokenData> {
                    client.post(
                        "$authUrl/$TOKEN",
                        block = {
                            setBody(Credential.RefreshToken(refreshToken) as Credential)
                        }
                    ).asResource { body() }
                }

                when (result) {
                    is Result.Success -> {
                        authPreferences.updateTokenData(result.data)

                        BearerTokens(
                            accessToken = result.data.accessToken,
                            refreshToken = result.data.refreshToken
                        )
                    }

                    is Result.Error -> {
                        if (result is Result.Error.Http && result.httpStatusCode == HttpStatusCode.Unauthorized) {
                            authPreferences.clear()
                        }
                        null
                    }
                }
            }
            sendWithoutRequest {
                val host = it.url.host
                val urlString = it.url.toString()
                val isAuthEndpoint = urlString.endsWith("/$TOKEN")
                (hosts.contains(host) || isIpAddress(host)) && !isAuthEndpoint
            }
        }
    }

    private fun isIpAddress(host: String): Boolean {
        return isIPv4(host)
    }

    private fun isIPv4(host: String): Boolean {
        val ipv4Regex = Regex(
            "^((25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])\\.){3}(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])$"
        )
        return ipv4Regex.matches(host)
    }

    @OptIn(ExperimentalTime::class)
    private fun isJwtTokenExpired(refreshToken: String): Boolean {
        return try {
            // Some refresh tokens are also JWTs, others might be opaque
            if (refreshToken.contains(".")) {
                val payload = decodeJWTPayload(refreshToken)
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

}