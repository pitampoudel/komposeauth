package com.vardansoft.komposeauth.login

import androidx.compose.runtime.Composable
import com.vardansoft.core.domain.Result
import com.vardansoft.komposeauth.data.Credential
import com.vardansoft.komposeauth.data.LoginOptions

interface KmpCredentialManager {
    suspend fun getCredential(options: LoginOptions): Result<Credential>
    suspend fun createPassKeyAndRetrieveJson(options: String): Result<String>
}

@Composable
expect fun rememberKmpCredentialManager(): KmpCredentialManager