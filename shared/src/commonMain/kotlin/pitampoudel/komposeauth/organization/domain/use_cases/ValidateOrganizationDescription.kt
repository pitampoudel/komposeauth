package pitampoudel.komposeauth.organization.domain.use_cases

import pitampoudel.core.domain.validators.GeneralValidationError
import pitampoudel.core.domain.validators.ValidationResult

object ValidateOrganizationDescription {
    operator fun invoke(value: String): ValidationResult {
        return if (value.length > 500) {
            ValidationResult.Error(GeneralValidationError.VALIDATION_ERROR_TOO_LONG)
        } else {
            ValidationResult.Success
        }
    }
}
