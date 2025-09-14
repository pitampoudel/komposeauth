package com.vardansoft.authx.ui.login

import androidx.compose.runtime.Composable
import com.vardansoft.authx.data.Credential
import com.vardansoft.core.domain.Result

interface CredentialRetriever {
    suspend fun getCredential(): Result<Credential>
}

@Composable
expect fun rememberCredentialRetriever(): CredentialRetriever