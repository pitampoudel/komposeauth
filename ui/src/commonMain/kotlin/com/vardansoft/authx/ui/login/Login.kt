package com.vardansoft.authx.ui.login

import androidx.compose.runtime.Composable
import com.vardansoft.authx.data.Credential

interface CredentialRetriever {
    suspend fun getCredential(): Result<Credential>
}

@Composable
expect fun rememberCredentialRetriever(): CredentialRetriever