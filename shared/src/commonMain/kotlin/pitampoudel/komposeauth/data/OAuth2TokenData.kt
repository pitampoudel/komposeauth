package pitampoudel.komposeauth.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OAuth2TokenData(
    @SerialName("accessToken")
    val accessToken: String,
    @SerialName("tokenType")
    val tokenType: String,
    @SerialName("expiresIn")
    val expiresIn: Long,
    @SerialName("refreshToken")
    val refreshToken: String? = null,
    @SerialName("idToken")
    val idToken: String? = null
)
