package pitampoudel.komposeauth.core.domain.validators

import pitampoudel.core.data.parsePhoneNumber
import pitampoudel.core.domain.validators.GeneralValidationError
import pitampoudel.core.domain.validators.ValidationResult

object ValidatePhoneNumber {
    const val DEFAULT_COUNTRY_NAME_CODE = "NP"

    operator fun invoke(phoneNumber: String, countryNameCode: String): ValidationResult {
        return if (phoneNumber.isBlank()) {
            ValidationResult.Error(GeneralValidationError.VALIDATION_ERROR_MUST_NOT_BE_BLANK)
        } else if (parsePhoneNumber(
                countryNameCode = countryNameCode,
                phoneNumber = phoneNumber
            ) == null
        ) {
            ValidationResult.Error(GeneralValidationError.VALIDATION_ERROR_INVALID_PHONE_NUMBER)
        } else {
            ValidationResult.Success
        }
    }
}