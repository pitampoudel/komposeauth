package pitampoudel.komposeauth.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pitampoudel.core.data.parsePhoneNumber
import pitampoudel.core.domain.isValidEmail
import pitampoudel.komposeauth.domain.use_cases.ValidatePhoneNumber

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
    val countryNameCode: String? = ValidatePhoneNumber.DEFAULT_COUNTRY_NAME_CODE,
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

    init {
        require(firstName.isNotBlank()) {
            "First name cannot be blank"
        }
        require(password == null || Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$").matches(password)) {
            "Password must contain at least one uppercase letter, one lowercase letter, and one digit"
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