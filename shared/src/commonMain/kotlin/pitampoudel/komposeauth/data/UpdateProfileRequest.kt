package pitampoudel.komposeauth.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pitampoudel.core.data.EncodedData
import pitampoudel.core.domain.isValidEmail

@Serializable
data class UpdateProfileRequest(
    @SerialName("email")
    val email: String? = null,
    @SerialName("givenName")
    val givenName: String? = null,
    @SerialName("familyName")
    val familyName: String? = null,
    @SerialName("password")
    val password: String? = null,
    @SerialName("confirmPassword")
    val confirmPassword: String? = null,
    @SerialName("picture")
    val picture: EncodedData? = null
) {
    init {
        require(password == confirmPassword) {
            "Password and confirmation password must match"
        }
        require(givenName == null || givenName.isNotBlank())
        require(familyName == null || familyName.isNotBlank())
        require(password == null || Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$").matches(password))
        require(password == confirmPassword) {
            "Password and confirmation password must match"
        }
        require(email.isNullOrBlank() || email.isValidEmail())
    }
}