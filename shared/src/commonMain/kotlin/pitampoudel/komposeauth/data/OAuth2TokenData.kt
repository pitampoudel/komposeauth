package pitampoudel.komposeauth.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OAuth2TokenData(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("token_ype")
    val tokenType: String? = null,
    @SerialName("expires_in")
    val expiresIn: Long,
    @SerialName("refresh_token")
    val refreshToken: String? = null,
    @SerialName("id_token")
    val idToken: String? = null
)
