package pitampoudel.komposeauth.core.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pitampoudel.core.data.parsePhoneNumber
import pitampoudel.core.domain.isValidEmail
import pitampoudel.komposeauth.core.domain.use_cases.ValidatePhoneNumber

@Serializable
data class CreateUserRequest(
    @SerialName("firstName")
    val firstName: String,
    @SerialName("lastName")
    val lastName: String?,
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
        return email ?: phoneNumberParsed()
    }

    fun findAlternateUsername(): String? {
        val primary = findPrimaryUsername()
        return phoneNumberParsed().takeIf { it != primary }
    }

    init {
        require(firstName.isNotBlank()) {
            "First name cannot be blank"
        }
        require(password == null || Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*\\W)(?!.* ).{8,16}$").matches(password)) {
            "Password must be 8-16 characters with at least one uppercase, lowercase, digit, and special character"
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