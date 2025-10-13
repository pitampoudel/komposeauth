package com.vardansoft.core.domain.validators

object ValidateNotEmpty {
    operator fun <T> invoke(value: List<T>): ValidationResult {
        return if (value.isEmpty()) {
            ValidationResult.Error(AuthXValidationError.VALIDATION_ERROR_MUST_NOT_BE_EMPTY)
        } else {
            ValidationResult.Success
        }
    }
}