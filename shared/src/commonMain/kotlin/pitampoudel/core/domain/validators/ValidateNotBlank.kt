package pitampoudel.core.domain.validators

object ValidateNotBlank {
    operator fun invoke(value: String): ValidationResult {
        return if (value.isBlank()) {
            ValidationResult.Error(AuthValidationError.VALIDATION_ERROR_MUST_NOT_BE_BLANK)
        } else {
            ValidationResult.Success
        }
    }
}
