package pitampoudel.komposeauth.domain.use_cases

import pitampoudel.core.domain.validators.AuthValidationError
import pitampoudel.core.domain.validators.ValidationResult

object ValidateEmail {
    operator fun invoke(email: String): ValidationResult {
        return if (email.isBlank()) {
            ValidationResult.Error(AuthValidationError.VALIDATION_ERROR_MUST_NOT_BE_BLANK)
        } else if (!matchesEmailRegex(email)) {
            ValidationResult.Error(AuthValidationError.VALIDATION_ERROR_INVALID_EMAIL)
        } else {
            ValidationResult.Success
        }
    }

    private fun matchesEmailRegex(email: CharSequence): Boolean {
        val pattern = "^[\\w.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$"
        return Regex(pattern, RegexOption.IGNORE_CASE).matches(email)
    }
}