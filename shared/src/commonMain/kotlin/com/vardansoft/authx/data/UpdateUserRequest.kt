package com.vardansoft.authx.data

import com.vardansoft.core.domain.isValidEmail


data class UpdateUserRequest(
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val password: String? = null,
    val confirmPassword: String? = null,
) {
    init {
        require(password == confirmPassword) {
            "Password and confirmation password must match"
        }
        require(firstName == null || firstName.isNotBlank())
        require(lastName == null || lastName.isNotBlank())
        require(password == null || Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$").matches(password))
        require(password == confirmPassword) {
            "Password and confirmation password must match"
        }
        require(email.isNullOrBlank() || email.isValidEmail())
    }
}
