package com.vardansoft.authx.domain.use_cases

import com.vardansoft.core.domain.validators.ValidationResult
import com.vardansoft.core.domain.validators.AuthXValidationError

object ValidateConfirmPassword {
    operator fun invoke(password: String, confirmPassword: String): ValidationResult {
        return if (confirmPassword.isBlank()) {
            ValidationResult.Error(AuthXValidationError.VALIDATION_ERROR_MUST_NOT_BE_BLANK)
        } else if (confirmPassword != password) {
            ValidationResult.Error(AuthXValidationError.VALIDATION_ERROR_PASSWORDS_DONT_MATCH)
        } else {
            ValidationResult.Success
        }
    }
}