package com.vardansoft.authx.data

import com.vardansoft.core.domain.isValidEmail
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateUserRequest(
    @SerialName("id")
    val id: String? = null,
    @SerialName("firstName")
    val firstName: String,
    @SerialName("lastName")
    val lastName: String?,
    @SerialName("email")
    val email: String? = null,
    @SerialName("phoneNumber")
    val phoneNumber: String? = null,
    @SerialName("password")
    val password: String? = null,
    @SerialName("confirmPassword")
    val confirmPassword: String? = null,
    @SerialName("picture")
    val picture: String? = null
) {
    init {
        require(firstName.isNotBlank())
        require(lastName?.isNotBlank() == true)
        require(password == null || Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$").matches(password)) {
            "Password must contain at least one uppercase letter, one lowercase letter, and one digit"
        }
        require(password == confirmPassword) {
            "Password and confirmation password must match"
        }
        require(email.isNullOrBlank() || email.isValidEmail())
        require(phoneNumber.isNullOrBlank() || phoneNumber.isValidPhoneNumber())
        require(email != null || phoneNumber != null)
    }

    private fun String.isValidPhoneNumber(): Boolean {
        // Basic phone number validation
        return this.length > 9 && this.all { it.isDigit() || it == '+' || it == '(' || it == ')' || it == '-' || it == ' ' }
    }
}