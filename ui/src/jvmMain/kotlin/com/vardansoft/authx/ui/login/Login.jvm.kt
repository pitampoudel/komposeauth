package com.vardansoft.authx.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.vardansoft.authx.data.Credential
import com.vardansoft.authx.domain.AuthX
import com.vardansoft.authx.domain.AuthXClient
import com.vardansoft.authx.ui.login.GoogleAuth
import org.koin.java.KoinJavaComponent.getKoin

@Composable
actual fun rememberCredentialRetriever(): CredentialRetriever {
    return remember {
        object : CredentialRetriever {
            override suspend fun getCredential(): Result<Credential> {
                // Fetch Google OAuth client-id dynamically from server
                val authXClient = getKoin().get<AuthXClient>()
                val googleAuthClientId = authXClient.fetchConfig().getOrElse {
                    return Result.failure(it)
                }.googleClientId
                val clientId = getKoin().get<AuthX>().clientId
                val token = GoogleAuth.getCredential(googleAuthClientId)
                return Result.success(Credential.GoogleId(clientId, token))
            }
        }
    }
}
