package com.vardansoft.authx.domain.use_cases

import com.vardansoft.core.domain.validators.ValidationResult

class ValidatePassword {
    operator fun invoke(password: String): ValidationResult {
        return if (password.isBlank()) {
            ValidationResult.Error("Must not be blank")
        } else if (!matchesPasswordRequirements(password)) {
            ValidationResult.Error("Password must be strong")
        } else {
            ValidationResult.Success
        }
    }

    private fun matchesPasswordRequirements(password: String): Boolean {
        val pattern = "^(?=.*\\W)(?!.* ).{8,16}$"
        return Regex(pattern).matches(password)
    }
}
