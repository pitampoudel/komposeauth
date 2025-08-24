package com.vardansoft.authx.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConfigResponse(
    @SerialName("googleClientId")
    val googleClientId: String
)
