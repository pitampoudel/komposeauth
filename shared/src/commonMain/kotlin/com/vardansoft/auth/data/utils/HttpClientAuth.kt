package com.vardansoft.auth.data.utils

import com.vardansoft.auth.domain.LoginPreferences
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.AuthConfig
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.scope.Scope

fun AuthConfig.vardanSoftBearer(scope: Scope, clientId: String) {
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
                        json(Json {})
                    }
                },
                loginPreferences = loginPreferences
            )
        }
    }

}