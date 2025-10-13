package com.vardansoft.authx.domain.use_cases

import com.vardansoft.core.domain.validators.ValidationResult

object ValidateConfirmPassword {
    operator fun invoke(password: String, confirmPassword: String): ValidationResult {
        return if (confirmPassword.isBlank()) {
            ValidationResult.Error("Must not be blank")
        } else if (confirmPassword != password) {
            ValidationResult.Error("Confirm password do not match")
        } else {
            ValidationResult.Success
        }
    }
}