package com.vardansoft.komposeauth.login

import androidx.compose.runtime.Composable
import com.vardansoft.komposeauth.data.Credential
import com.vardansoft.core.domain.Result

interface CredentialRetriever {
    suspend fun getCredential(): Result<Credential>
}

@Composable
expect fun rememberCredentialRetriever(): CredentialRetriever