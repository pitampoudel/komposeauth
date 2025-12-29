package pitampoudel.komposeauth.core.domain.validators

import pitampoudel.core.domain.validators.GeneralValidationError
import pitampoudel.core.domain.validators.ValidationResult

object ValidateEmail {
    operator fun invoke(email: String): ValidationResult {
        return if (email.isBlank()) {
            ValidationResult.Error(GeneralValidationError.VALIDATION_ERROR_MUST_NOT_BE_BLANK)
        } else if (!matchesEmailRegex(email)) {
            ValidationResult.Error(GeneralValidationError.VALIDATION_ERROR_INVALID_EMAIL)
        } else {
            ValidationResult.Success
        }
    }

    private fun matchesEmailRegex(email: CharSequence): Boolean {
        val pattern = "^[\\w.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$"
        return Regex(pattern, RegexOption.IGNORE_CASE).matches(email)
    }
}