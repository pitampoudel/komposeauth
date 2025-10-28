package pitampoudel.core.domain.validators

object ValidateNotEmpty {
    operator fun <T> invoke(value: List<T>): ValidationResult {
        return if (value.isEmpty()) {
            ValidationResult.Error(GeneralValidationError.VALIDATION_ERROR_MUST_NOT_BE_EMPTY)
        } else {
            ValidationResult.Success
        }
    }
}