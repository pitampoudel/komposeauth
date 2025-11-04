package pitampoudel.komposeauth.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.russhwolf.settings.ObservableSettings
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.SHA256
import dev.whyoleg.cryptography.providers.webcrypto.WebCrypto
import io.ktor.http.encodeURLParameter
import io.ktor.util.encodeBase64
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.koin.mp.KoinPlatform.getKoin
import org.w3c.dom.MessageEvent
import org.w3c.dom.events.Event
import pitampoudel.core.domain.Result
import pitampoudel.komposeauth.data.Credential
import pitampoudel.komposeauth.data.LoginOptions
import pitampoudel.komposeauth.domain.Platform
import kotlin.coroutines.resume
import kotlin.js.Promise
import kotlin.random.Random

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """
    function base64urlToArrayBuffer(base64url) {
      const base64 = base64url.replace(/-/g, '+').replace(/_/g, '/');
      const pad = '='.repeat((4 - (base64.length % 4)) % 4);
      const str = atob(base64 + pad);
      const bytes = new Uint8Array(str.length);
      for (let i = 0; i < str.length; i++) bytes[i] = str.charCodeAt(i);
      return bytes.buffer;
    }
    function arrayBufferToBase64url(buffer) {
      const bytes = new Uint8Array(buffer);
      let binary = '';
      for (let i = 0; i < bytes.byteLength; i++) binary += String.fromCharCode(bytes[i]);
      return btoa(binary)
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
        .replace(/=+${'$'}/g, '');
    }

    async (optionsJson) => {
      'use strict';
        const options = JSON.parse(optionsJson);
        // Convert binary fields from Base64URL to ArrayBuffers for WebAuthn API
        options.challenge = base64urlToArrayBuffer(options.challenge);
        options.user.id = base64urlToArrayBuffer(options.user.id);

        const credential = await navigator.credentials.create({ publicKey: options });

        // Convert back from ArrayBuffers → Base64URL strings for server
        return {
          publicKey: {
            credential: {
              id: credential.id,
              rawId: arrayBufferToBase64url(credential.rawId),
              type: credential.type,
              response: {
                attestationObject: arrayBufferToBase64url(credential.response.attestationObject),
                clientDataJSON: arrayBufferToBase64url(credential.response.clientDataJSON)
              }
            },
            label: "1password"
          }
        };
       }
    """
)
external fun createPasskey(optionsJson: String): Promise<JsAny>

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
actual fun rememberKmpCredentialManager(): KmpCredentialManager {
    return remember {
        val koin = getKoin()
        val settings = koin.get<ObservableSettings>()
        object : KmpCredentialManager {
            override suspend fun getCredential(options: LoginOptions): Result<Credential> {
                val verifier = generateCodeVerifier()
                val challenge = sha256Base64Url(verifier)
                val state = "state-${Random.nextInt()}"
                val redirectUri = window.location.origin

                settings.putString("pkce_verifier", verifier)
                settings.putString("oauth_state", state)
                val googleClientId = options.googleClientId
                    ?: return Result.Error("Google client id is not provided")
                val authUrl = buildGoogleAuthUrl(
                    clientId = googleClientId,
                    redirectUri = redirectUri,
                    codeChallenge = challenge,
                    state = state
                )

                return openAuthPopupAndWait(authUrl)
            }

            override suspend fun createPassKeyAndRetrieveJson(options: String): Result<JsonObject> {
                return try {
                    val credentialJs: Any = createPasskey(options).await()
                    val jsonObject = Json.parseToJsonElement(credentialJs.toString()).jsonObject
                    Result.Success(jsonObject)
                } catch (e: Throwable) {
                    Result.Error(e.message.orEmpty())
                }
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
                        redirectUri = window.location.origin,
                        platform = Platform.WEB
                    )
                )
            }
        }
    }
}
