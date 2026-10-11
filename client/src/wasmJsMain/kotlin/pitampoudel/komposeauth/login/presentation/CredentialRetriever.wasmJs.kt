package pitampoudel.komposeauth.login.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.ktor.http.encodeURLParameter
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.w3c.dom.MessageEvent
import org.w3c.dom.events.Event
import pitampoudel.core.domain.Result
import pitampoudel.komposeauth.user.data.Credential
import pitampoudel.komposeauth.core.data.LoginOptionsResponse
import pitampoudel.komposeauth.core.domain.Platform
import kotlin.coroutines.resume
import kotlin.js.Promise
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """
    async (optionsJson) => {
      'use strict';
        function base64urlToArrayBuffer(base64url) {
          const base64 = base64url.replace(/-/g, '+').replace(/_/g, '/');
          const pad = base64.length % 4 ? 4 - (base64.length % 4) : 0;
          const base64Padded = base64 + '='.repeat(pad);
          const binary = atob(base64Padded);
          const bytes = new Uint8Array(binary.length);
          for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
          return bytes.buffer;
        }
        function arrayBufferToBase64url(buffer) {
          const bytes = new Uint8Array(buffer);
          let binary = '';
          for (let i = 0; i < bytes.byteLength; i++) binary += String.fromCharCode(bytes[i]);
          const base64 = btoa(binary);
          return base64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
        }

        const options = JSON.parse(optionsJson);
        // Convert binary fields from Base64URL to ArrayBuffers for WebAuthn API
        options.challenge = base64urlToArrayBuffer(options.challenge);
        if (options.user && options.user.id) {
          options.user.id = base64urlToArrayBuffer(options.user.id);
        }
        
        // Convert excludeCredentials IDs to ArrayBuffers
        if (options.excludeCredentials && Array.isArray(options.excludeCredentials)) {
          options.excludeCredentials = options.excludeCredentials.map(cred => ({
            ...cred,
            id: base64urlToArrayBuffer(cred.id)
          }));
        }

        let credential;
        try {
          credential = await navigator.credentials.create({ publicKey: options });
        } catch (e) {
          return JSON.stringify({ error: (e && e.name) || 'Error' });
        }

        // Back to Base64URL strings, in the shape the server reads a credential in.
        return JSON.stringify({
          credential: {
            id: credential.id,
            rawId: arrayBufferToBase64url(credential.rawId),
            type: credential.type,
            authenticatorAttachment: credential.authenticatorAttachment || undefined,
            clientExtensionResults: credential.getClientExtensionResults(),
            response: {
              attestationObject: arrayBufferToBase64url(credential.response.attestationObject),
              clientDataJSON: arrayBufferToBase64url(credential.response.clientDataJSON),
              transports: credential.response.getTransports ? credential.response.getTransports() : []
            }
          }
        });
       }
    """
)
external fun createPasskey(optionsJson: String): Promise<JsAny>

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => typeof PublicKeyCredential !== 'undefined' && !!navigator.credentials")
private external fun browserSupportsPasskeys(): Boolean

/** The browser and the system it runs on, such as "Chrome on macOS". */
private fun browserName(userAgent: String): String {
    val browser = when {
        "Edg" in userAgent -> "Edge"
        "OPR/" in userAgent -> "Opera"
        "SamsungBrowser" in userAgent -> "Samsung Internet"
        "Firefox/" in userAgent || "FxiOS" in userAgent -> "Firefox"
        "Chrome/" in userAgent || "CriOS" in userAgent -> "Chrome"
        "Safari/" in userAgent -> "Safari"
        else -> null
    }
    val system = when {
        "iPhone" in userAgent || "iPad" in userAgent -> "iOS"
        "Android" in userAgent -> "Android"
        "Windows" in userAgent -> "Windows"
        "CrOS" in userAgent -> "ChromeOS"
        "Mac OS X" in userAgent -> "macOS"
        "Linux" in userAgent -> "Linux"
        else -> null
    }
    return listOfNotNull(browser, system).joinToString(" on ").ifEmpty { "Web browser" }
}

private fun buildGoogleAuthUrl(
    clientId: String,
    redirectUri: String,
    state: String
): String {
    val params = mapOf(
        "response_type" to "code",
        "client_id" to clientId,
        "redirect_uri" to redirectUri,
        "scope" to "openid email profile",
        "state" to state,
        // Without this Google reuses whichever account it already has a session for, leaving no
        // way to sign in as somebody else.
        "prompt" to "select_account"
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

@OptIn(ExperimentalWasmJsInterop::class, ExperimentalUuidApi::class)
@Composable
actual fun rememberKmpCredentialManager(): KmpCredentialManager {
    return remember {
        object : KmpCredentialManager {
            override suspend fun getCredential(credentialType: CredentialType, options: LoginOptionsResponse): Result<Credential> {
                return when (credentialType) {
                    CredentialType.GOOGLE, CredentialType.ANY -> {
                        // Unguessable (Uuid.random draws from crypto.getRandomValues), and held right
                        // here: the popup answers this page, which never reloads in between.
                        val state = Uuid.random().toString()
                        val redirectUri = window.location.origin
                        val googleClientId = options.googleClientId
                            ?: return Result.Error("Google client id is not provided")
                        val authUrl = buildGoogleAuthUrl(
                            clientId = googleClientId,
                            redirectUri = redirectUri,
                            state = state
                        )

                        openAuthPopupAndWait(authUrl, state)
                    }
                    CredentialType.APPLE -> Result.Error("iOS Sign In is not supported on Web")
                }
            }

            override val canCreatePasskeys: Boolean = browserSupportsPasskeys()

            override val passkeyLabel: String = browserName(window.navigator.userAgent)

            override suspend fun createPassKeyAndRetrieveJson(options: String): Result<JsonObject> {
                val answer = try {
                    Json.parseToJsonElement(createPasskey(options).await<JsAny>().toString()).jsonObject
                } catch (e: Throwable) {
                    return Result.Error("The passkey could not be created. Try again.")
                }
                answer["credential"]?.let { return Result.Success(it.jsonObject) }
                // The names are the WebAuthn API's DOMException names.
                return when (answer["error"]?.jsonPrimitive?.content) {
                    "NotAllowedError", "AbortError" -> PasskeyCancelled
                    "InvalidStateError" -> Result.Error("This device already has a passkey for your account.")
                    "SecurityError" -> Result.Error("Passkeys are not set up for this website.")
                    else -> Result.Error("The passkey could not be created. Try again.")
                }
            }

            private suspend fun openAuthPopupAndWait(authUrl: String, expectedState: String): Result<Credential> {
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
                                    continuation.resume(handleCallback(code, state, expectedState))
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


            private fun handleCallback(code: String, state: String, expectedState: String): Result<Credential> {
                if (state != expectedState) return Result.Error("Invalid state.")

                return Result.Success(
                    Credential.AuthCode(
                        code = code,
                        redirectUri = window.location.origin,
                        platform = Platform.WEB
                    )
                )
            }
        }
    }
}
