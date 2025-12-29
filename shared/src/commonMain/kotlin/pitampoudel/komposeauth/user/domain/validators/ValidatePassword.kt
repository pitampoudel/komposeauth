package pitampoudel.komposeauth.user.domain.validators

import pitampoudel.core.domain.validators.GeneralValidationError
import pitampoudel.core.domain.validators.ValidationResult

object ValidatePassword {
    operator fun invoke(password: String): ValidationResult {
        return if (password.isBlank()) {
            ValidationResult.Error(GeneralValidationError.VALIDATION_ERROR_MUST_NOT_BE_BLANK)
        } else if (!matchesPasswordRequirements(password)) {
            ValidationResult.Error(GeneralValidationError.VALIDATION_ERROR_PASSWORD_REQUIREMENT)
        } else {
            ValidationResult.Success
        }
    }

    private fun matchesPasswordRequirements(password: String): Boolean {
        val pattern = "^.{8,}"
        return Regex(pattern).matches(password)
    }
}