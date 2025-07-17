package com.vardansoft.auth.login.presentation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class Credential {
    @Serializable
    @SerialName("GOOGLE_ID")
    class GoogleId(
        @SerialName("clientId")
        val clientId: String,
        @SerialName("idToken")
        val idToken: String
    ) : Credential()
}