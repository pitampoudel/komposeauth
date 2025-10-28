package pitampoudel.komposeauth.domain.use_cases

import pitampoudel.core.domain.validators.GeneralValidationError
import pitampoudel.core.domain.validators.ValidationResult

object ValidateOtpCode {
    const val OTP_LENGTH = 6
    operator fun invoke(code: String): ValidationResult {
        return if (code.isBlank()) {
            ValidationResult.Error(GeneralValidationError.VALIDATION_ERROR_MUST_NOT_BE_BLANK)
        } else if (code.length != OTP_LENGTH || code.toIntOrNull() == null) {
            ValidationResult.Error(GeneralValidationError.VALIDATION_ERROR_TOO_SHORT)
        } else {
            ValidationResult.Success
        }
    }
}