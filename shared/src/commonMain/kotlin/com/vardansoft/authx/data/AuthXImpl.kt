package com.vardansoft.authx.data

import com.vardansoft.authx.data.ApiEndpoints.REFRESH_TOKEN
import com.vardansoft.authx.domain.AuthX
import com.vardansoft.authx.domain.AuthXPreferences
import com.vardansoft.core.data.asResource
import com.vardansoft.core.data.safeApiCall
import com.vardansoft.core.domain.Result
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.AuthConfig
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headers

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
                val refreshToken = this.oldTokens?.refreshToken

                if (refreshToken.isNullOrEmpty() || isTokenExpired(refreshToken)) {
                    authXPreferences.clear()
                    return@refreshTokens null
                }

                val result = safeApiCall<OAuth2TokenData> {
                    client.post(
                        "$authUrl/$REFRESH_TOKEN",
                        block = {
                            headers {
                                remove(HttpHeaders.Authorization)
                            }
                            setBody(TokenRefreshRequest(refreshToken))
                        }
                    ).asResource { body() }
                }

                when (result) {
                    is Result.Success -> {
                        authXPreferences.updateTokenData(result.data)

                        BearerTokens(
                            accessToken = result.data.accessToken,
                            refreshToken = result.data.refreshToken
                        )
                    }

                    is Result.Error -> {
                        if (result is Result.Error.Http && result.httpStatusCode == HttpStatusCode.Unauthorized) {
                            authXPreferences.clear()
                        }
                        null
                    }
                }
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