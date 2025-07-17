package com.vardansoft.auth.credentials.dto

import com.vardansoft.auth.credentials.entity.Credential

data class CredentialResponse(
    val provider: Credential.Provider,
    val accessToken: String,
    val refreshToken: String?
)