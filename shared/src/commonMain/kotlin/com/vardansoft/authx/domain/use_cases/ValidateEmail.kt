package com.vardansoft.authx.domain.use_cases

import com.vardansoft.core.domain.validators.ValidationResult

object ValidateEmail {
    operator fun invoke(email: String): ValidationResult {
        return if (email.isBlank()) {
            ValidationResult.Error("Must not be blank")
        } else if (!matchesEmailRegex(email)) {
            ValidationResult.Error("Must be an email")
        } else {
            ValidationResult.Success
        }
    }

    private fun matchesEmailRegex(email: CharSequence): Boolean {
        val pattern = "^[\\w.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$"
        return Regex(pattern, RegexOption.IGNORE_CASE).matches(email)
    }
}