package pitampoudel.komposeauth.domain.use_cases

import pitampoudel.core.domain.isUrlValid
import pitampoudel.core.domain.validators.GeneralValidationError
import pitampoudel.core.domain.validators.ValidationResult

object ValidateFacebookLinkOrBlank {
    operator fun invoke(value: String): ValidationResult {
        return if (value.isNotBlank() && !isUrlValid(value)) {
            ValidationResult.Error(GeneralValidationError.VALIDATION_ERROR_INVALID_URL)
        } else {
            ValidationResult.Success
        }

    }
}