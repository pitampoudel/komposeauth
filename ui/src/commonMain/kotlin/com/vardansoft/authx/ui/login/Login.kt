package com.vardansoft.authx.ui.login

import androidx.compose.runtime.Composable
import com.vardansoft.authx.domain.Credential

interface CredentialRetriever {
    suspend fun getCredential(): Result<Credential>
}

@Composable
expect fun rememberCredentialRetriever(
    clientId: String,
    googleAuthClientId: String
): CredentialRetriever