package com.vardansoft.authx.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class Credential {
    @Serializable
    @SerialName("GOOGLE_ID")
    class GoogleId(
        @SerialName("idToken")
        val idToken: String
    ) : Credential()

    @Serializable
    @SerialName("USERNAME_PASSWORD")
    class EmailPassword(
        @SerialName("username")
        val username: String,
        @SerialName("password")
        val password: String
    ) : Credential()

}