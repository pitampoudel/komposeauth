package pitampoudel.komposeauth.core.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccessTokenClaims(
    @SerialName("aud")
    val aud: List<String>,
    @SerialName("authorities")
    val authorities: List<String>,
    @SerialName("email")
    val email: String,
    @SerialName("exp")
    val exp: Long,
    @SerialName("familyName")
    val familyName: String? = null,
    @SerialName("givenName")
    val givenName: String,
    @SerialName("iat")
    val iat: Long,
    @SerialName("iss")
    val iss: String,
    @SerialName("nbf")
    val nbf: Long,
    @SerialName("picture")
    val picture: String? = null,
    @SerialName("scope")
    val scope: String,
    @SerialName("scp")
    val scp: List<String>,
    @SerialName("sub")
    val sub: String,
    @SerialName("kycVerified")
    val kycVerified: Boolean,
    @SerialName("phoneNumberVerified")
    val phoneNumberVerified: Boolean
) {
    fun fullName(): String {
        return givenName + (familyName?.let { " $it" } ?: "")
    }
}