package com.vardansoft.komposeauth.domain.use_cases

import com.vardansoft.core.data.parsePhoneNumber
import com.vardansoft.core.domain.validators.ValidationResult
import com.vardansoft.core.domain.validators.AuthValidationError

object ValidatePhoneNumber {
    const val DEFAULT_COUNTRY_NAME_CODE = "NP"

    operator fun invoke(phoneNumber: String, countryNameCode: String): ValidationResult {
        return if (phoneNumber.isBlank()) {
            ValidationResult.Error(AuthValidationError.VALIDATION_ERROR_MUST_NOT_BE_BLANK)
        } else if (parsePhoneNumber(
                countryNameCode = countryNameCode,
                phoneNumber = phoneNumber
            ) == null
        ) {
            ValidationResult.Error(AuthValidationError.VALIDATION_ERROR_INVALID_PHONE_NUMBER)
        } else {
            ValidationResult.Success
        }
    }
}