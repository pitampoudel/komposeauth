package com.vardansoft.authx.domain.use_cases

import com.vardansoft.core.domain.validators.ValidationResult
import com.vardansoft.core.domain.validators.AuthXValidationError

object ValidateOtpCode {
    const val OTP_LENGTH = 6
    operator fun invoke(code: String): ValidationResult {
        return if (code.isBlank()) {
            ValidationResult.Error(AuthXValidationError.VALIDATION_ERROR_MUST_NOT_BE_BLANK)
        } else if (code.length != OTP_LENGTH || code.toIntOrNull() == null) {
            ValidationResult.Error(AuthXValidationError.VALIDATION_ERROR_TOO_SHORT)
        } else {
            ValidationResult.Success
        }
    }
}