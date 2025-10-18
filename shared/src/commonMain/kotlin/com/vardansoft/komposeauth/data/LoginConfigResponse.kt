package com.vardansoft.komposeauth.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginConfigResponse(
    @SerialName("googleClientId")
    val googleClientId: String,
    @SerialName("publicKeyAuthOptionsJson")
    val publicKeyAuthOptionsJson: String?
)
