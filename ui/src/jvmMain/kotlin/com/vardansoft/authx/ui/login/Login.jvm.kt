package com.vardansoft.authx.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.vardansoft.authx.data.Credential


@Composable
actual fun rememberCredentialRetriever(): CredentialRetriever {
    return remember {
        object : CredentialRetriever {
            override suspend fun getCredential(): Result<Credential> {
                TODO("Not yet implemented")
            }
        }

    }
}