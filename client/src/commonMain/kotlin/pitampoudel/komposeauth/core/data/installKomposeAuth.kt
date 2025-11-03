package pitampoudel.komposeauth.core.data

import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import pitampoudel.core.data.asResource
import pitampoudel.core.data.safeApiCall
import pitampoudel.core.domain.Result
import pitampoudel.komposeauth.core.data.JwtUtils.isJwtTokenExpired
import pitampoudel.komposeauth.core.domain.Config
import pitampoudel.komposeauth.data.ApiEndpoints.LOGIN
import pitampoudel.komposeauth.data.Credential
import pitampoudel.komposeauth.data.OAuth2TokenData

fun HttpClientConfig<*>.installKomposeAuth(
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

    val authPreferences = AuthPreferencesImpl.getInstance()

    install(HttpCookies) {
        storage = AcceptAllCookiesStorage()
    }

    install(Auth) {
        bearer {
            loadTokens {
                val accessToken = authPreferences.accessToken()
                val refreshToken = authPreferences.refreshToken()
                if (refreshToken.isNullOrEmpty()) {
                    return@loadTokens null
                }
                BearerTokens(
                    accessToken = accessToken ?: "",
                    refreshToken = refreshToken
                )
            }

            refreshTokens {
                val refreshToken = this.oldTokens?.refreshToken

                if (refreshToken.isNullOrEmpty() || isJwtTokenExpired(refreshToken)) {
                    authPreferences.clear()
                    return@refreshTokens null
                }

                val result = safeApiCall<OAuth2TokenData> {
                    client.post(
                        "$authServerUrl/$LOGIN",
                        block = {
                            parameter("wantToken", true)
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
            sendWithoutRequest { builder ->
                val authorizedHosts = (resourceServerUrls + authServerUrl).map { Url(it).host }
                val host = builder.url.host
                val urlString = builder.url.toString()
                val isAuthEndpoint = urlString.endsWith("/$LOGIN")
                (authorizedHosts.contains(host) || isIPv4(host)) && !isAuthEndpoint
            }
        }
    }
}