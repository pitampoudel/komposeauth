package com.vardansoft.authx.domain.use_cases

import com.vardansoft.core.domain.validators.ValidationResult
import com.vardansoft.core.domain.validators.AuthXValidationError

object ValidateEmail {
    operator fun invoke(email: String): ValidationResult {
        return if (email.isBlank()) {
            ValidationResult.Error(AuthXValidationError.VALIDATION_ERROR_MUST_NOT_BE_BLANK)
        } else if (!matchesEmailRegex(email)) {
            ValidationResult.Error(AuthXValidationError.VALIDATION_ERROR_INVALID_EMAIL)
        } else {
            ValidationResult.Success
        }
    }

    private fun matchesEmailRegex(email: CharSequence): Boolean {
        val pattern = "^[\\w.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$"
        return Regex(pattern, RegexOption.IGNORE_CASE).matches(email)
    }
}