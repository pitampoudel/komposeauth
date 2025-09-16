package com.vardansoft.authx.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TokenRefreshRequest(
    @SerialName("refreshToken")
    val refreshToken: String
)