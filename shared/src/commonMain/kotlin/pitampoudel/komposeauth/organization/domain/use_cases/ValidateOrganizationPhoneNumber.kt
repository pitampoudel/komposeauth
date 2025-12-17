package pitampoudel.komposeauth.organization.domain.use_cases

import pitampoudel.core.domain.validators.ValidationResult
import pitampoudel.komposeauth.core.domain.use_cases.ValidatePhoneNumber

object ValidateOrganizationPhoneNumber {
    operator fun invoke(value: String, countryNameCode: String): ValidationResult {
        return if (value.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidatePhoneNumber(phoneNumber = value, countryNameCode = countryNameCode)
        }
    }
}
