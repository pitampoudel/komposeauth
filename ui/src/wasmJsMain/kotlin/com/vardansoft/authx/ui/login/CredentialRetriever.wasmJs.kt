package com.vardansoft.authx.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.russhwolf.settings.ObservableSettings
import com.vardansoft.authx.data.Credential
import com.vardansoft.authx.data.Credential.GoogleId
import com.vardansoft.authx.domain.AuthXClient
import com.vardansoft.core.domain.Result
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.digest.SHA256
import dev.whyoleg.cryptography.providers.webcrypto.WebCrypto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.http.Parameters
import io.ktor.http.encodeURLParameter
import io.ktor.http.parseQueryString
import io.ktor.util.encodeBase64
import kotlinx.browser.window
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.mp.KoinPlatform.getKoin
import kotlin.random.Random

private fun generateCodeVerifier(length: Int = 64): String {
    val allowed = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
    return buildString {
        repeat(length) { append(allowed.random()) }
    }
}

private suspend fun sha256Base64Url(input: String): String {
    val digest = CryptographyProvider.WebCrypto.get(SHA256)
        .hasher().hash(input.encodeToByteArray())
    return digest.encodeBase64()
        .replace('+', '-')
        .replace('/', '_')
        .replace("=", "")
}

private fun buildGoogleAuthUrl(
    clientId: String,
    redirectUri: String,
    codeChallenge: String,
    state: String
): String {
    val params = mapOf(
        "response_type" to "code",
        "client_id" to clientId,
        "redirect_uri" to redirectUri,
        "scope" to "openid email profile",
        "code_challenge" to codeChallenge,
        "code_challenge_method" to "S256",
        "state" to state
    )
    return "https://accounts.google.com/o/oauth2/v2/auth?" +
            params.entries.joinToString("&") { (k, v) -> "${k}=${v.encodeURLParameter()}" }
}

private class WasmCredentialRetriever(
    private val settings: ObservableSettings,
    private val authXClient: AuthXClient,
    private val httpClient: HttpClient
) : CredentialRetriever {

    override suspend fun getCredential(): Result<Credential> {
        val params = parseQueryString(window.location.search.drop(1))
        val code = params["code"]
        val state = params["state"]

        return if (code != null && state != null) {
            handleRedirect(code, state)
        } else {
            startLogin()
        }
    }


    private suspend fun startLogin(): Result<Credential> {
        val config = when (val configResult = authXClient.fetchConfig(desktop = true)) {
            is Result.Error -> return configResult
            is Result.Success -> configResult.data
        }

        val googleAuthClientId = config.googleClientId
        val redirectUri = window.location.origin

        val codeVerifier = generateCodeVerifier()
        val codeChallenge = sha256Base64Url(codeVerifier)
        val newState = "random-${Random.nextDouble()}"
        settings.putString("pkce_verifier", codeVerifier)
        settings.putString("oauth_state", newState)

        val authUrl = buildGoogleAuthUrl(googleAuthClientId, redirectUri, codeChallenge, newState)
        window.location.href = authUrl

        // Suspend indefinitely since we are redirecting away from the page
        suspendCancellableCoroutine<Nothing> { }
    }

    private suspend fun handleRedirect(code: String, state: String): Result<Credential> {
        val savedState = settings.getStringOrNull("oauth_state")
        if (savedState != state) {
            return Result.Error("Invalid state parameter from redirect.")
        }
        settings.remove("oauth_state")

        val verifier = settings.getStringOrNull("pkce_verifier")
            ?: return Result.Error("Missing PKCE verifier.")
        settings.remove("pkce_verifier")

        val config = when (val configResult = authXClient.fetchConfig(desktop = true)) {
            is Result.Error -> return configResult
            is Result.Success -> configResult.data
        }

        val redirectUri = window.location.origin + "/callback"

        return try {
            val token = exchangeToken(
                code = code,
                clientId = config.googleClientId,
                redirectUri = redirectUri,
                verifier = verifier,
            )
            Result.Success(GoogleId(token))
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to exchange token for credential.")
        }
    }

    private suspend fun exchangeToken(
        code: String,
        clientId: String,
        redirectUri: String,
        verifier: String
    ): String {
        val response: JsonObject = httpClient.submitForm(
            url = "https://oauth2.googleapis.com/token",
            formParameters = Parameters.build {
                append("grant_type", "authorization_code")
                append("code", code)
                append("client_id", clientId)
                append("redirect_uri", redirectUri)
                append("code_verifier", verifier)
            }
        ).body()

        return response["id_token"]?.jsonPrimitive?.content ?: error("No id_token in response")
    }
}

@Composable
actual fun rememberCredentialRetriever(): CredentialRetriever {
    return remember {
        val koin = getKoin()
        WasmCredentialRetriever(
            settings = koin.get(),
            authXClient = koin.get(),
            httpClient = koin.get()
        )
    }
}
