package pitampoudel.komposeauth.core.data

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import pitampoudel.core.data.asResource
import pitampoudel.core.data.safeApiCall
import pitampoudel.core.domain.Result
import pitampoudel.komposeauth.core.domain.AuthPreferences
import pitampoudel.komposeauth.core.domain.Config
import pitampoudel.komposeauth.data.ApiEndpoints.LOGIN
import pitampoudel.komposeauth.data.Credential
import pitampoudel.komposeauth.data.OAuth2Response
import pitampoudel.komposeauth.data.ResponseType

internal fun HttpClientConfig<*>.installKomposeAuth(
    authPreferences: AuthPreferences,
    authServerUrl: String,
    resourceServerUrls: List<String>
) {
    Config.authServerUrl = authServerUrl
    fun isIPv4(host: String): Boolean {
        val ipv4Regex = Regex(
            "^((25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])\\.){3}(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])$"
        )
        return ipv4Regex.matches(host)
    }
    install(HttpCookies)
    install(ContentNegotiation) {
        json(
            Json {
                encodeDefaults = true
                ignoreUnknownKeys = true
                useAlternativeNames = false
                prettyPrint = true
            }
        )
    }
    install(DefaultRequest) {
        contentType(ContentType.Application.Json)
    }
    install(Auth) {
        bearer {
            loadTokens {
                val accessToken = authPreferences.tokenData()?.accessToken
                val refreshToken = authPreferences.tokenData()?.refreshToken
                if (accessToken.isNullOrEmpty()) {
                    return@loadTokens null
                }
                BearerTokens(
                    accessToken = accessToken,
                    refreshToken = refreshToken
                )
            }

            refreshTokens {
                val refreshToken = this.oldTokens?.refreshToken

                if (refreshToken.isNullOrEmpty()) {
                    authPreferences.clear()
                    return@refreshTokens null
                }
                refresh(client, authServerUrl, refreshToken, authPreferences)
            }
            sendWithoutRequest { builder ->
                val hosts = (resourceServerUrls + authServerUrl).toSet().map {
                    Url(it).host
                }.toSet()
                val host = builder.url.host
                hosts.contains(host) || isIPv4(host)
            }
        }
    }
}

private suspend fun refresh(
    client: HttpClient,
    authServerUrl: String,
    refreshToken: String,
    authPreferences: AuthPreferences
): BearerTokens? {
    val refreshClient = HttpClient(client.engine) {
        install(DefaultRequest) {
            contentType(ContentType.Application.Json)
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }
    val result = safeApiCall<OAuth2Response> {
        refreshClient.post(
            "$authServerUrl/$LOGIN",
            block = {
                parameter("responseType", ResponseType.TOKEN.name)
                setBody(Credential.RefreshToken(refreshToken) as Credential)
            }
        ).asResource { body() }
    }

    return when (result) {
        is Result.Success -> {
            authPreferences.saveTokenData(result.data)
            BearerTokens(
                accessToken = result.data.accessToken,
                refreshToken = result.data.refreshToken
            )
        }

        is Result.Error -> {
            if (result is Result.Error.Http) {
                authPreferences.clear()
            }
            null
        }
    }
}