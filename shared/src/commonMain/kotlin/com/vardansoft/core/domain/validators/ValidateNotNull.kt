package com.vardansoft.core.domain.validators

object ValidateNotNull {
    operator fun <T> invoke(value: T?): ValidationResult {
        return if (value == null) {
            ValidationResult.Error(AuthXValidationError.VALIDATION_ERROR_MUST_BE_SELECTED)
        } else {
            ValidationResult.Success
        }
    }
}
