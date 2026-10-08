package pitampoudel.komposeauth.core.data

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import pitampoudel.core.data.asResource
import pitampoudel.core.data.safeApiCall
import pitampoudel.core.domain.Result
import pitampoudel.komposeauth.core.domain.ApiEndpoints.LOGIN
import pitampoudel.komposeauth.login.domain.AuthPreferences
import pitampoudel.komposeauth.core.domain.Config
import pitampoudel.komposeauth.core.domain.Platform
import pitampoudel.komposeauth.core.domain.ResponseType
import pitampoudel.komposeauth.core.domain.currentPlatform
import pitampoudel.komposeauth.user.data.Credential

internal fun HttpClientConfig<*>.installKomposeAuth(
    authPreferences: AuthPreferences,
    authServerUrl: String,
    resourceServerUrls: List<String>
) {
    Config.authServerUrl = authServerUrl
    installKomposeAuth(authPreferences, resourceServerUrls)
}

internal fun HttpClientConfig<*>.installKomposeAuth(
    authPreferences: AuthPreferences,
    authServerUrl: Flow<String?>,
    resourceServerUrls: List<String>,
    scope: CoroutineScope
) {
    scope.launch {
        authServerUrl.collect {
            Config.authServerUrl = it
        }
    }
    installKomposeAuth(authPreferences, resourceServerUrls)
}

internal fun HttpClientConfig<*>.installKomposeAuth(
    authPreferences: AuthPreferences,
    resourceServerUrls: List<String>
) {
    // Only the web target signs in with the access-token cookie (`LoginUser` is the only caller
    // that ever passes `ResponseType.COOKIE`); every other platform authenticates purely with the
    // bearer token in `Authorization`. Installing the cookie jar unconditionally used to make a
    // native client pick up and resend *any* cookie the server set — including the session cookie
    // `/login-options` creates to hold the WebAuthn challenge, which has nothing to do with
    // authentication. Once that was in the jar, every later bearer-authenticated write (send-otp,
    // verify-otp, update-profile, ...) stopped qualifying as a header-only bearer request on the
    // server, which then demands a CSRF token this client never fetches — a permanent 403 for the
    // rest of the app's lifetime. Native platforms have no legitimate use for any cookie, so they
    // simply don't keep one.
    if (currentPlatform() == Platform.WEB) {
        install(HttpCookies)
    }
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
                val tokenData = authPreferences.tokenData() ?: return@loadTokens null
                BearerTokens(
                    accessToken = tokenData.accessToken,
                    refreshToken = tokenData.refreshToken
                )
            }

            refreshTokens {
                val refreshToken = this.oldTokens?.refreshToken

                if (refreshToken.isNullOrEmpty()) {
                    authPreferences.clear()
                    return@refreshTokens null
                }
                val authServerUrl = Config.authServerUrl ?: return@refreshTokens null
                refresh(client.engine, authServerUrl, refreshToken, authPreferences)
            }
            // The token goes only to the servers it is for; any other host this client calls, by
            // name or by address, must not receive it.
            sendWithoutRequest { builder ->
                val authServerUrl = Config.authServerUrl ?: return@sendWithoutRequest false
                val hosts = (resourceServerUrls + authServerUrl).map { Url(it).host }.toSet()
                builder.url.host in hosts
            }
        }
    }
}

private suspend fun refresh(
    engine: HttpClientEngine,
    authServerUrl: String,
    refreshToken: String,
    authPreferences: AuthPreferences
): BearerTokens? {
    val refreshClient = HttpClient(engine) {
        install(DefaultRequest) {
            contentType(ContentType.Application.Json)
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }
    val result = try {
        safeApiCall<OAuth2Response> {
            refreshClient.post(
                "$authServerUrl/$LOGIN",
                block = {
                    parameter("responseType", ResponseType.TOKEN.name)
                    setBody(Credential.RefreshToken(refreshToken) as Credential)
                }
            ).asResource { body() }
        }
    } finally {
        // Built on the caller's engine, which closing this client leaves open.
        refreshClient.close()
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
            // Only the server refusing the refresh token signs the user out. A 5xx or a dropped
            // connection says nothing about the token, and clearing it then logged people out
            // whenever the server restarted.
            if (result is Result.Error.Http && result.httpStatusCode.value in REFUSED_REFRESH) {
                authPreferences.clear()
            }
            null
        }
    }
}

private val REFUSED_REFRESH = setOf(400, 401, 403)
