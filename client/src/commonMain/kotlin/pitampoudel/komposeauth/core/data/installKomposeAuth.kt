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
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.HttpStatusCode
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
import pitampoudel.komposeauth.core.domain.ResponseType
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
    // Passkey sign-in needs the auth server's session cookie on every platform: `/login-options`
    // keeps the WebAuthn challenge in that session and `/login` reads it back. The bearer token is
    // still the credential, so the jar holds the auth server's cookies and no other host's.
    install(HttpCookies) {
        storage = AuthServerCookies()
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
        // The token goes only to the servers it is for; any other host this client calls, by name
        // or by address, must not receive it, neither on the first send nor on the retry after a 401.
        reAuthorizeOnResponse { response ->
            response.status == HttpStatusCode.Unauthorized &&
                response.call.request.url.host in tokenHosts(resourceServerUrls)
        }
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
            sendWithoutRequest { builder -> builder.url.host in tokenHosts(resourceServerUrls) }
        }
    }
}

private fun tokenHosts(resourceServerUrls: List<String>): Set<String> {
    val authServerUrl = Config.authServerUrl ?: return emptySet()
    return (resourceServerUrls + authServerUrl).map { Url(it).host }.toSet()
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

private class AuthServerCookies(
    private val delegate: CookiesStorage = AcceptAllCookiesStorage()
) : CookiesStorage by delegate {
    private fun isAuthServer(url: Url): Boolean =
        Config.authServerUrl?.let { Url(it).host == url.host } ?: false

    override suspend fun get(requestUrl: Url): List<Cookie> =
        if (isAuthServer(requestUrl)) delegate.get(requestUrl) else emptyList()

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        if (isAuthServer(requestUrl)) delegate.addCookie(requestUrl, cookie)
    }
}
