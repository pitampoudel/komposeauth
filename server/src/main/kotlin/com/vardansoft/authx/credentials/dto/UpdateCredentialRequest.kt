package com.vardansoft.authx.credentials.dto

import com.vardansoft.authx.credentials.entity.Credential

data class UpdateCredentialRequest(
    val provider: Credential.Provider,
    val accessToken: String,
    val refreshToken: String? = null
)