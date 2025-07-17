package com.vardansoft.auth.domain.use_cases

import com.vardansoft.auth.domain.ValidationResult

class ValidatePhoneNumber() {
    operator fun invoke(phoneNumber: String, countryNameCode: String): ValidationResult {
        return if (phoneNumber.isBlank()) {
            ValidationResult.Error("Must not be blank")
        } else {
            ValidationResult.Success
        }
    }
}