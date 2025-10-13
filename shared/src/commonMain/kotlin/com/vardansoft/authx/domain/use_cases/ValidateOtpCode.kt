package com.vardansoft.authx.domain.use_cases

import com.vardansoft.core.domain.validators.ValidationResult

object ValidateOtpCode {
    const val OTP_LENGTH = 6
    operator fun invoke(code: String): ValidationResult {
        return if (code.isBlank()) {
            ValidationResult.Error("Must not be blank")
        } else if (code.length != OTP_LENGTH || code.toIntOrNull() == null) {
            ValidationResult.Error("Invalid format")
        } else {
            ValidationResult.Success
        }
    }
}