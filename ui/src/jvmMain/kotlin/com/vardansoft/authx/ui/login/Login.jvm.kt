package com.vardansoft.authx.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.vardansoft.authx.data.Credential
import com.vardansoft.authx.domain.AuthX
import com.vardansoft.authx.domain.AuthXClient
import com.vardansoft.core.data.NetworkResult
import org.koin.java.KoinJavaComponent.getKoin

@Composable
actual fun rememberCredentialRetriever(): CredentialRetriever {
    return remember {
        object : CredentialRetriever {
            override suspend fun getCredential(): NetworkResult<Credential> {
                // Fetch Google OAuth client-id dynamically from server
                val authXClient = getKoin().get<AuthXClient>()
                val res = authXClient.fetchConfig()
                when (res) {
                    is NetworkResult.Error -> return res
                    is NetworkResult.Success -> {
                        val googleAuthClientId = res.data.googleClientId
                        val clientId = getKoin().get<AuthX>().clientId
                        val token = GoogleAuthPKCE.getCredential(googleAuthClientId)
                        if (token == null) {
                            return NetworkResult.Error("Failed to retrieve Google ID token")
                        }
                        return NetworkResult.Success(Credential.GoogleId(clientId, token))
                    }
                }
            }
        }
    }
}
