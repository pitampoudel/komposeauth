package pitampoudel.komposeauth.core.domain.validators

import pitampoudel.core.domain.isValidEmail
import pitampoudel.core.domain.validators.GeneralValidationError
import pitampoudel.core.domain.validators.ValidationResult

object ValidateEmail {
    operator fun invoke(email: String): ValidationResult {
        return if (email.isBlank()) {
            ValidationResult.Error(GeneralValidationError.VALIDATION_ERROR_MUST_NOT_BE_BLANK)
        } else if (!email.isValidEmail()) {
            ValidationResult.Error(GeneralValidationError.VALIDATION_ERROR_INVALID_EMAIL)
        } else {
            ValidationResult.Success
        }
    }
}
