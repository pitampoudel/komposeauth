package com.vardansoft.komposeauth.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.vardansoft.komposeauth.data.Credential
import com.vardansoft.komposeauth.domain.Platform
import com.vardansoft.komposeauth.core.domain.AuthClient
import com.vardansoft.komposeauth.login.OAuthUtils.buildAuthUrl
import com.vardansoft.komposeauth.login.OAuthUtils.generateCodeChallenge
import com.vardansoft.komposeauth.login.OAuthUtils.generateCodeVerifier
import com.vardansoft.komposeauth.login.OAuthUtils.listenForCode
import com.vardansoft.core.domain.Result
import com.vardansoft.komposeauth.data.LoginOptions
import org.koin.java.KoinJavaComponent.getKoin
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

            override suspend fun createPassKeyAndRetrieveJson(options: String): Result<String> {
                TODO("Not yet implemented")
            }
        }
    }
}
