package com.vardansoft.core.domain.validators

object ValidateNotBlank {
    operator fun invoke(value: String): ValidationResult {
        return if (value.isBlank()) {
            ValidationResult.Error(AuthXValidationError.VALIDATION_ERROR_MUST_NOT_BE_BLANK)
        } else {
            ValidationResult.Success
        }
    }
}
