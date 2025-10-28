package pitampoudel.komposeauth.domain.use_cases

import pitampoudel.core.domain.validators.GeneralValidationError
import pitampoudel.core.domain.validators.ValidationResult

object ValidateConfirmPassword {
    operator fun invoke(password: String, confirmPassword: String): ValidationResult {
        return if (confirmPassword.isBlank()) {
            ValidationResult.Error(GeneralValidationError.VALIDATION_ERROR_MUST_NOT_BE_BLANK)
        } else if (confirmPassword != password) {
            ValidationResult.Error(GeneralValidationError.VALIDATION_ERROR_PASSWORDS_DONT_MATCH)
        } else {
            ValidationResult.Success
        }
    }
}