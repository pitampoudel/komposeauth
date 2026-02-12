package pitampoudel.core.domain.validators

object ValidateAlphabeticName {
    private val nameRegex = Regex(
        "^[\\p{L}\\p{M}]+(?:[ '\\-’][\\p{L}\\p{M}]+)*$"
    )
    private val whitespaceRegex = Regex("\\s+")

    fun sanitize(value: String): String = value.trim().replace(whitespaceRegex, " ")

    fun isValid(value: String, allowBlank: Boolean = false): Boolean {
        val normalized = sanitize(value)
        if (normalized.isEmpty()) return allowBlank
        return nameRegex.matches(normalized)
    }

    operator fun invoke(value: String, allowBlank: Boolean = false): ValidationResult {
        val normalized = sanitize(value)
        if (normalized.isEmpty()) {
            return if (allowBlank) {
                ValidationResult.Success
            } else {
                ValidationResult.Error(GeneralValidationError.VALIDATION_ERROR_MUST_NOT_BE_BLANK)
            }
        }
        return if (nameRegex.matches(normalized)) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(GeneralValidationError.VALIDATION_ERROR_INVALID_NAME)
        }
    }
}

