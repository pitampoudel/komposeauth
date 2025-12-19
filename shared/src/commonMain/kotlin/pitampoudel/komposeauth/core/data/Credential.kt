package pitampoudel.komposeauth.core.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pitampoudel.core.data.parsePhoneNumber
import pitampoudel.komposeauth.core.domain.Platform

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
        @SerialName("platform")
        val platform: Platform,
        @SerialName("code")
        val code: String,
        @SerialName("redirectUri")
        val redirectUri: String
    ) : Credential()

    @Serializable
    @SerialName("USERNAME_PASSWORD")
    class UsernamePassword(
        @SerialName("countryNameCode")
        val countryNameCode: String? = null,
        @SerialName("username")
        val username: String,
        @SerialName("password")
        val password: String
    ) : Credential() {
        fun username() = parsePhoneNumber(
            countryNameCode, username
        )?.fullNumberInE164Format ?: username
    }

    @Serializable
    @SerialName("PUBLIC_KEY")
    data class PublicKey(
        @SerialName("authenticationResponseJson")
        val authenticationResponseJson: String
    ) : Credential()

    @Serializable
    @SerialName("REFRESH_TOKEN")
    data class RefreshToken(
        @SerialName("refreshToken")
        val refreshToken: String
    ) : Credential()

}