package pitampoudel.komposeauth.core.data

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
        require(givenName == null || givenName.isNotBlank()) {
            "Given name cannot be blank"
        }
        require(familyName == null || familyName.isNotBlank()) {
            "Family name cannot be blank"
        }
        require(password == null || Regex("^.{8,}").matches(password)) {
            "Password must be at least 8 characters long and may include letters, numbers, and special characters"
        }
        require(email.isNullOrBlank() || email.isValidEmail()){
            "Invalid email"
        }
    }
}