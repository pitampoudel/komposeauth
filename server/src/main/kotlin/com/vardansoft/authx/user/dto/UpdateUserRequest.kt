package com.vardansoft.authx.user.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class UpdateUserRequest(
    @Email(message = "Invalid email address")
    val email: String? = null,
    @NotBlank(message = "First name is required")
    val firstName: String? = null,
    @NotBlank(message = "Last name is required")
    val lastName: String? = null,
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
        message = "Password must contain at least one uppercase letter, one lowercase letter and one number"
    )
    val password: String? = null,
    val confirmPassword: String? = null,
) {
    init {
        require(password == confirmPassword) {
            "Password and confirmation password must match"
        }
    }
}
