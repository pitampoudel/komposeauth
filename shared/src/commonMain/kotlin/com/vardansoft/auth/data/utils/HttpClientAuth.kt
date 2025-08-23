package com.vardansoft.auth.data.utils

import com.vardansoft.auth.VardanSoftAuth.RESOURCE_SERVERS
import com.vardansoft.auth.domain.LoginPreferences
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.AuthConfig
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.scope.Scope

fun AuthConfig.applyVardanSoftBearer(scope: Scope, clientId: String) {
    val loginPreferences = scope.getOrNull<LoginPreferences>()

    return bearer {
        loadTokens {
            loginPreferences?.oAuth2TokenData()?.let {
                BearerTokens(
                    accessToken = it.accessToken,
                    refreshToken = it.refreshToken
                )
            }
        }

        refreshTokens {
            tryTokenRefresh(
                clientId = clientId,
                client = HttpClient {
                    install(ContentNegotiation) {
                        json(Json)
                    }
                },
                loginPreferences = loginPreferences
            )
        }
        sendWithoutRequest {
            val host = it.url.host
            RESOURCE_SERVERS.contains(host) || isIpAddress(host)
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