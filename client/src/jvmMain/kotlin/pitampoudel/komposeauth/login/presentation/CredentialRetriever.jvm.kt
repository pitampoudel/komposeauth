package pitampoudel.komposeauth.login.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonObject
import pitampoudel.core.domain.Result
import pitampoudel.komposeauth.user.data.Credential
import pitampoudel.komposeauth.core.data.LoginOptionsResponse
import pitampoudel.komposeauth.core.domain.Platform
import pitampoudel.komposeauth.login.OAuthUtils
import pitampoudel.komposeauth.login.OAuthUtils.buildAuthUrl
import java.awt.Desktop
import java.net.URI
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

@Composable
actual fun rememberKmpCredentialManager(): KmpCredentialManager {
    return remember {
        object : KmpCredentialManager {
            override suspend fun getCredential(credentialType: CredentialType, options: LoginOptionsResponse): Result<Credential> {
                return when (credentialType) {
                    CredentialType.GOOGLE, CredentialType.ANY -> {
                        val googleAuthClientId = options.googleClientId ?: return Result.Error(
                            "Google client id not found"
                        )
                        // The state ties the redirect to this request, so a page that knows the port
                        // cannot complete someone's sign-in with a code of its own.
                        val state = UUID.randomUUID().toString()
                        OAuthUtils.LoopbackReceiver().use { receiver ->
                            Desktop.getDesktop().browse(URI(buildAuthUrl(googleAuthClientId, receiver.redirectUri, state)))
                            val redirect = withTimeoutOrNull(5.minutes) { receiver.awaitRedirect() }
                                ?: return Result.Error("Google sign-in timed out")
                            val code = redirect["code"]?.takeIf { redirect["state"] == state }
                                ?: return Result.Error("Google sign-in was cancelled")
                            Result.Success(
                                Credential.AuthCode(
                                    code = code,
                                    redirectUri = receiver.redirectUri,
                                    platform = Platform.DESKTOP
                                )
                            )
                        }
                    }
                    CredentialType.APPLE -> Result.Error("iOS Sign In is not supported on Desktop")
                }
            }

            override suspend fun createPassKeyAndRetrieveJson(options: String): Result<JsonObject> {
                return Result.Error("Passkeys are not supported on Desktop")
            }
        }
    }
}
