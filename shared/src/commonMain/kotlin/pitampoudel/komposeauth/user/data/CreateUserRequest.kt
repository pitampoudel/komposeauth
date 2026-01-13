package pitampoudel.komposeauth.user.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pitampoudel.core.data.parsePhoneNumber
import pitampoudel.core.domain.isValidEmail

@Serializable
data class CreateUserRequest(
    @SerialName("firstName")
    val firstName: String? = null,
    @SerialName("lastName")
    val lastName: String? = null,
    @SerialName("email")
    val email: String? = null,
    @SerialName("phoneNumber")
    val phoneNumber: String? = null,
    @SerialName("countryNameCode")
    val countryNameCode: String? = null,
    @SerialName("password")
    val password: String? = null,
    @SerialName("confirmPassword")
    val confirmPassword: String? = null,
    @SerialName("photoUrl")
    val photoUrl: String? = null
) {
    fun phoneNumberParsed() = phoneNumber?.let {
        parsePhoneNumber(
            countryNameCode = countryNameCode,
            phoneNumber = it
        )
    }?.fullNumberInE164Format

    fun findPrimaryUsername(): String? {
        return email?.lowercase() ?: phoneNumberParsed()
    }

    fun findAlternateUsername(): String? {
        val primary = findPrimaryUsername()
        return phoneNumberParsed().takeIf { it != primary }
    }

    init {
        require(password == null || Regex("^.{8,}").matches(password)) {
            "Password must be at least 8 characters long and may include letters, numbers, and special characters"
        }
        require(password == confirmPassword) {
            "Password and confirmation password must match"
        }
        require(email.isNullOrBlank() || email.isValidEmail()) {
            "Invalid email format"
        }
        require(phoneNumber.isNullOrBlank() || phoneNumber.isValidPhoneNumber()) {
            "Invalid phone number format"
        }
        require(email != null || phoneNumber != null) {
            "Either email or phone number must be provided"
        }
    }

    private fun String.isValidPhoneNumber(): Boolean {
        // Basic phone number validation
        return this.length > 9 && this.all { it.isDigit() || it == '+' || it == '(' || it == ')' || it == '-' || it == ' ' }
    }
}