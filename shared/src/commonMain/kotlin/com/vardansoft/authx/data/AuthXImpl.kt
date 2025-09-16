package com.vardansoft.authx.data

import com.vardansoft.authx.data.utils.tryTokenRefresh
import com.vardansoft.authx.domain.AuthX
import com.vardansoft.authx.domain.AuthXPreferences
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.AuthConfig
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.Url
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class AuthXImpl internal constructor(
    private val authXPreferences: AuthXPreferences,
    override val authUrl: String,
    override val serverUrls: List<String>
) : AuthX {
    val hosts = (serverUrls + authUrl).map {
        Url(it).host
    }

    override fun configureBearer(auth: AuthConfig) {
        auth.bearer {
            loadTokens {
                authXPreferences.oAuth2TokenData()?.let {
                    BearerTokens(
                        accessToken = it.accessToken,
                        refreshToken = it.refreshToken
                    )
                }
            }

            refreshTokens {
                tryTokenRefresh(
                    client = HttpClient {
                        install(ContentNegotiation) { json(Json) }
                    },
                    authXPreferences = authXPreferences,
                    authUrl = authUrl
                )
            }
            sendWithoutRequest {
                val host = it.url.host
                hosts.contains(host) || isIpAddress(host)
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
}