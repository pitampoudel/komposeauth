package com.vardansoft.authx.domain.use_cases

class ValidateNotNull {
    operator fun <T> invoke(value: T?): ValidationResult {
        return if (value == null) {
            ValidationResult.Error("Must be selected")
        } else {
            ValidationResult.Success
        }
    }
}
