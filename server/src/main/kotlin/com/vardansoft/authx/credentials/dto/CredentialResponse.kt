package com.vardansoft.authx.credentials.dto

import com.vardansoft.authx.credentials.entity.Credential

data class CredentialResponse(
    val provider: Credential.Provider,
    val accessToken: String,
    val refreshToken: String?
)