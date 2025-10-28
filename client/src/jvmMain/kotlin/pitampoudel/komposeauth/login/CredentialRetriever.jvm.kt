package pitampoudel.komposeauth.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.serialization.json.JsonObject
import pitampoudel.core.domain.Result
import pitampoudel.komposeauth.data.Credential
import pitampoudel.komposeauth.data.LoginOptions
import pitampoudel.komposeauth.domain.Platform
import pitampoudel.komposeauth.login.OAuthUtils.buildAuthUrl
import pitampoudel.komposeauth.login.OAuthUtils.generateCodeChallenge
import pitampoudel.komposeauth.login.OAuthUtils.generateCodeVerifier
import pitampoudel.komposeauth.login.OAuthUtils.listenForCode
import java.awt.Desktop
import java.net.ServerSocket
import java.net.URI

@Composable
actual fun rememberKmpCredentialManager(): KmpCredentialManager {
    return remember {
        object : KmpCredentialManager {
            override suspend fun getCredential(options: LoginOptions): Result<Credential> {
                val googleAuthClientId = options.googleClientId ?: return Result.Error(
                    "Google client id not found"
                )
                val port = ServerSocket(0).use { it.localPort }
                val redirectUri = "http://127.0.0.1:$port/callback"
                val verifier = generateCodeVerifier()
                val challenge = generateCodeChallenge(verifier)
                val authUri = buildAuthUrl(googleAuthClientId, redirectUri, challenge)
                Desktop.getDesktop().browse(URI(authUri))
                val code = listenForCode(port)
                val credential = Credential.AuthCode(
                    code = code,
                    codeVerifier = verifier,
                    redirectUri = redirectUri,
                    platform = Platform.DESKTOP
                )
                return Result.Success(credential)
            }

            override suspend fun createPassKeyAndRetrieveJson(options: String): Result<JsonObject> {
                TODO("Not yet implemented")
            }
        }
    }
}
