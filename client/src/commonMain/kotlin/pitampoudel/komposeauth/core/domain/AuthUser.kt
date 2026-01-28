package pitampoudel.komposeauth.core.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthUser(
    @SerialName("authorities")
    val authorities: List<String>,
    @SerialName("email")
    val email: String? = null,
    @SerialName("familyName")
    val familyName: String? = null,
    @SerialName("givenName")
    val givenName: String? = null ,
    @SerialName("kycVerified")
    val kycVerified: Boolean,
    @SerialName("phoneNumberVerified")
    val phoneNumberVerified: Boolean,
    @SerialName("picture")
    val picture: String? = null,
    @SerialName("sub")
    val sub: String
) {
    fun fullName(): String {
        return givenName + (familyName?.let { " $it" } ?: "")
    }
}