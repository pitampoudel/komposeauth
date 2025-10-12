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
    @SerialName("APPLE_ID")
    class AppleId(
        @SerialName("idToken")
        val idToken: String
    ) : Credential()

    @Serializable
    @SerialName("AUTH_CODE")
    class AuthCode(
        @SerialName("code")
        val code: String,
        @SerialName("codeVerifier")
        val codeVerifier: String,
        @SerialName("redirectUri")
        val redirectUri: String
    ) : Credential()

    @Serializable
    @SerialName("USERNAME_PASSWORD")
    class UsernamePassword(
        @SerialName("username")
        val username: String,
        @SerialName("password")
        val password: String
    ) : Credential()

    @Serializable
    @SerialName("REFRESH_TOKEN")
    data class RefreshToken(
        @SerialName("refreshToken")
        val refreshToken: String
    ) : Credential()

}