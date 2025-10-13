package com.vardansoft.core.domain.validators

object ValidateNotNull {
    operator fun <T> invoke(value: T?): ValidationResult {
        return if (value == null) {
            ValidationResult.Error("Must be selected")
        } else {
            ValidationResult.Success
        }
    }
}
