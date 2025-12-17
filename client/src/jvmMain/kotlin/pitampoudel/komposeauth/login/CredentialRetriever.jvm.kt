package pitampoudel.komposeauth.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.serialization.json.JsonObject
import pitampoudel.core.domain.Result
import pitampoudel.komposeauth.core.data.Credential
import pitampoudel.komposeauth.core.data.LoginOptionsResponse
import pitampoudel.komposeauth.core.domain.Platform
import pitampoudel.komposeauth.login.OAuthUtils.buildAuthUrl
import pitampoudel.komposeauth.login.OAuthUtils.listenForCode
import java.awt.Desktop
import java.net.ServerSocket
import java.net.URI

@Composable
actual fun rememberKmpCredentialManager(): KmpCredentialManager {
    return remember {
        object : KmpCredentialManager {
            override suspend fun getCredential(options: LoginOptionsResponse): Result<Credential> {
                val googleAuthClientId = options.googleClientId ?: return Result.Error(
                    "Google client id not found"
                )
                val port = ServerSocket(0).use { it.localPort }
                val redirectUri = "http://127.0.0.1:$port/callback"
                val authUri = buildAuthUrl(googleAuthClientId, redirectUri)
                Desktop.getDesktop().browse(URI(authUri))
                val code = listenForCode(port)
                val credential = Credential.AuthCode(
                    code = code,
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
