package com.vardansoft.komposeauth.login

import androidx.compose.runtime.Composable
import com.vardansoft.core.domain.Result
import com.vardansoft.komposeauth.data.Credential

interface KmpCredentialManager {
    suspend fun getCredential(): Result<Credential>
    suspend fun createPassKeyAndRetrieveJson(): Result<String>
}

@Composable
expect fun rememberKmpCredentialManager(): KmpCredentialManager