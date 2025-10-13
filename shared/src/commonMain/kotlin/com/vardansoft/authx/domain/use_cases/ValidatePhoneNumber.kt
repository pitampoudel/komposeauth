package com.vardansoft.authx.domain.use_cases

import com.vardansoft.core.data.parsePhoneNumber
import com.vardansoft.core.domain.validators.ValidationResult

object ValidatePhoneNumber {
    const val DEFAULT_COUNTRY_NAME_CODE = "NP"

    operator fun invoke(phoneNumber: String, countryNameCode: String): ValidationResult {
        return if (phoneNumber.isBlank()) {
            ValidationResult.Error("Must not be blank")
        } else if (parsePhoneNumber(
                countryNameCode = countryNameCode,
                phoneNumber = phoneNumber
            ) == null
        ) {
            ValidationResult.Error("Invalid phone number")
        } else {
            ValidationResult.Success
        }
    }
}