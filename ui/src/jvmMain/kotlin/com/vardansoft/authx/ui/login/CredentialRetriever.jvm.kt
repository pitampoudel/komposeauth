package com.vardansoft.authx.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.vardansoft.authx.data.Credential
import com.vardansoft.authx.data.Platform
import com.vardansoft.authx.domain.AuthXClient
import com.vardansoft.authx.ui.login.OAuthUtils.buildAuthUrl
import com.vardansoft.authx.ui.login.OAuthUtils.generateCodeChallenge
import com.vardansoft.authx.ui.login.OAuthUtils.generateCodeVerifier
import com.vardansoft.authx.ui.login.OAuthUtils.listenForCode
import com.vardansoft.core.domain.Result
import org.koin.java.KoinJavaComponent.getKoin
import java.awt.Desktop
import java.net.ServerSocket
import java.net.URI

@Composable
actual fun rememberCredentialRetriever(): CredentialRetriever {
    return remember {
        object : CredentialRetriever {
            override suspend fun getCredential(): Result<Credential> {
                // Fetch Google OAuth client-id dynamically from server
                val authXClient = getKoin().get<AuthXClient>()
                when (val res = authXClient.fetchConfig(platform = Platform.DESKTOP)) {
                    is Result.Error -> return res
                    is Result.Success -> {
                        val googleAuthClientId = res.data.googleClientId
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
                            redirectUri = redirectUri
                        )
                        return Result.Success(credential)
                    }
                }
            }
        }
    }
}
