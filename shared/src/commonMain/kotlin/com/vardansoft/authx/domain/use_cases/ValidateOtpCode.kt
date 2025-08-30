package com.vardansoft.authx.domain.use_cases

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