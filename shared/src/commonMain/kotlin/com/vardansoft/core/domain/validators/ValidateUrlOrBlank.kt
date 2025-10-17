package com.vardansoft.core.domain.validators

import com.vardansoft.core.domain.isUrlValid

object ValidateUrlOrBlank {
    operator fun invoke(value: String): ValidationResult {
        return if (value.isNotBlank() && !isUrlValid(value)) {
            ValidationResult.Error(AuthValidationError.VALIDATION_ERROR_INVALID_URL)
        } else {
            ValidationResult.Success
        }

    }
}