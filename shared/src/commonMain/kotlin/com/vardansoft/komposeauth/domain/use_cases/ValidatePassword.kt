package com.vardansoft.komposeauth.domain.use_cases

import com.vardansoft.core.domain.validators.ValidationResult
import com.vardansoft.core.domain.validators.AuthValidationError

object ValidatePassword {
    operator fun invoke(password: String): ValidationResult {
        return if (password.isBlank()) {
            ValidationResult.Error(AuthValidationError.VALIDATION_ERROR_MUST_NOT_BE_BLANK)
        } else if (!matchesPasswordRequirements(password)) {
            ValidationResult.Error(AuthValidationError.VALIDATION_ERROR_TOO_SHORT)
        } else {
            ValidationResult.Success
        }
    }

    private fun matchesPasswordRequirements(password: String): Boolean {
        val pattern = "^(?=.*\\W)(?!.* ).{8,16}$"
        return Regex(pattern).matches(password)
    }
}
