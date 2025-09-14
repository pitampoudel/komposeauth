package com.vardansoft.core.domain.validators

import com.vardansoft.core.domain.isUrlValid

class ValidateUrlOrBlank {
    operator fun invoke(value: String): ValidationResult {
        return if (value.isNotBlank() && !isUrlValid(value)) {
            ValidationResult.Error("Invalid URL")
        } else {
            ValidationResult.Success
        }

    }
}