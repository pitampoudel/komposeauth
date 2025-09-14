package com.vardansoft.authx.ui.login

import androidx.compose.runtime.Composable
import com.vardansoft.authx.data.Credential
import com.vardansoft.core.data.NetworkResult

interface CredentialRetriever {
    suspend fun getCredential(): NetworkResult<Credential>
}

@Composable
expect fun rememberCredentialRetriever(): CredentialRetriever