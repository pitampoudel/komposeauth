package com.vardansoft.core.domain.validators

object ValidateNotBlank {
    operator fun invoke(value: String): ValidationResult {
        return if (value.isBlank()) {
            ValidationResult.Error("Must not be blank")
        } else {
            ValidationResult.Success
        }
    }
}
