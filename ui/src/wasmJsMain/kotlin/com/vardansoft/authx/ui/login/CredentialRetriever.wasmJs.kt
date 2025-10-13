package com.vardansoft.authx.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.russhwolf.settings.ObservableSettings
import com.vardansoft.authx.data.Credential
import com.vardansoft.authx.domain.AuthXClient
import com.vardansoft.core.domain.Result
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.digest.SHA256
import dev.whyoleg.cryptography.providers.webcrypto.WebCrypto
import io.ktor.http.encodeURLParameter
import io.ktor.util.encodeBase64
import kotlinx.browser.window
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.mp.KoinPlatform.getKoin
import org.w3c.dom.MessageEvent
import org.w3c.dom.events.Event
import kotlin.coroutines.resume
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

@OptIn(ExperimentalWasmJsInterop::class)
private external interface OAuthMessageData : JsAny {
    val code: String?
    val state: String?
    val error: String?
}

@OptIn(ExperimentalWasmJsInterop::class)
@Composable
actual fun rememberCredentialRetriever(): CredentialRetriever {
    return remember {
        val koin = getKoin()
        val authXClient = koin.get<AuthXClient>()
        val settings = koin.get<ObservableSettings>()
        object : CredentialRetriever {
            override suspend fun getCredential(): Result<Credential> {
                val config = when (val result = authXClient.fetchConfig(pkce = true)) {
                    is Result.Error -> return result
                    is Result.Success -> result.data
                }
                val verifier = generateCodeVerifier()
                val challenge = sha256Base64Url(verifier)
                val state = "state-${Random.nextInt()}"
                val redirectUri = window.location.origin

                settings.putString("pkce_verifier", verifier)
                settings.putString("oauth_state", state)

                val authUrl = buildGoogleAuthUrl(
                    clientId = config.googleClientId,
                    redirectUri = redirectUri,
                    codeChallenge = challenge,
                    state = state
                )

                return openAuthPopupAndWait(authUrl)
            }

            private suspend fun openAuthPopupAndWait(authUrl: String): Result<Credential> {
                return suspendCancellableCoroutine { continuation ->
                    val popup = window.open(
                        url = authUrl,
                        target = "_blank",
                        features = "width=500,height=700"
                    ) ?: run {
                        continuation.resume(Result.Error("Failed to open authentication popup."))
                        return@suspendCancellableCoroutine
                    }

                    lateinit var listener: (Event) -> Unit
                    listener = { event ->
                        if (event is MessageEvent && event.origin == window.location.origin) {
                            val data = event.data?.unsafeCast<OAuthMessageData>()
                            if (data != null) {
                                val code = data.code
                                val state = data.state
                                val error = data.error

                                if (error != null) {
                                    popup.close()
                                    window.removeEventListener("message", listener)
                                    continuation.resume(Result.Error("Authentication failed: $error"))
                                }

                                if (code != null && state != null) {
                                    popup.close()
                                    window.removeEventListener("message", listener)
                                    continuation.resume(handleCallback(code, state))
                                }
                            }
                        }
                    }

                    // Register message listener
                    window.addEventListener("message", listener)

                    // Clean up if coroutine is cancelled
                    continuation.invokeOnCancellation {
                        window.removeEventListener("message", listener)
                        popup.close()
                    }
                }
            }


            private fun handleCallback(code: String, state: String): Result<Credential> {
                val savedState = settings.getStringOrNull("oauth_state")
                if (savedState != state) return Result.Error("Invalid state.")
                val verifier = settings.getStringOrNull("pkce_verifier")
                    ?: return Result.Error("Missing PKCE verifier.")

                settings.remove("pkce_verifier")
                settings.remove("oauth_state")

                return Result.Success(
                    Credential.AuthCode(
                        code = code,
                        codeVerifier = verifier,
                        redirectUri = window.location.origin
                    )
                )
            }
        }
    }
}
