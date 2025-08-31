package com.vardansoft.authx.domain.use_cases

class ValidateNotBlank {
    operator fun invoke(value: String): ValidationResult {
        return if (value.isBlank()) {
            ValidationResult.Error("Must not be blank")
        } else {
            ValidationResult.Success
        }
    }
}
