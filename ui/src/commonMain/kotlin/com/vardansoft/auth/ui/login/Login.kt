package com.vardansoft.auth.ui.login

import androidx.compose.runtime.Composable
import com.vardansoft.auth.presentation.Credential

interface CredentialRetriever {
    suspend fun getCredential(): Result<Credential>
}

@Composable
expect fun rememberCredentialRetriever(clientId: String): CredentialRetriever