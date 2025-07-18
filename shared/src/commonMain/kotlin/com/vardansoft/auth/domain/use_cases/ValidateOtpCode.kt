package com.vardansoft.auth.domain.use_cases

import com.vardansoft.auth.domain.ValidationResult

class ValidateOtpCode {
    operator fun invoke(code: String): ValidationResult {
        return if (code.isBlank()) {
            ValidationResult.Error("Must not be blank")
        } else if (code.length != OTP_LENGTH || code.toIntOrNull() == null) {
            ValidationResult.Error("Invalid format")
        } else {
            ValidationResult.Success
        }
    }

    companion object {
        const val OTP_LENGTH = 6
    }
}