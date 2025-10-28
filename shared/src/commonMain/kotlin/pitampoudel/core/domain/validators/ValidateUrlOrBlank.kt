package pitampoudel.core.domain.validators

import pitampoudel.core.domain.isUrlValid

object ValidateUrlOrBlank {
    operator fun invoke(value: String): ValidationResult {
        return if (value.isNotBlank() && !isUrlValid(value)) {
            ValidationResult.Error(GeneralValidationError.VALIDATION_ERROR_INVALID_URL)
        } else {
            ValidationResult.Success
        }

    }
}