package pitampoudel.komposeauth.core.domain.use_cases

import pitampoudel.core.domain.validators.GeneralValidationError
import pitampoudel.core.domain.validators.ValidationResult

object ValidatePassword {
    operator fun invoke(password: String): ValidationResult {
        return if (password.isBlank()) {
            ValidationResult.Error(GeneralValidationError.VALIDATION_ERROR_MUST_NOT_BE_BLANK)
        } else if (!matchesPasswordRequirements(password)) {
            ValidationResult.Error(GeneralValidationError.VALIDATION_ERROR_TOO_SHORT)
        } else {
            ValidationResult.Success
        }
    }

    private fun matchesPasswordRequirements(password: String): Boolean {
        // Password must be 8-16 characters, contain at least one uppercase, one lowercase, 
        // one digit, and one special character, no spaces allowed
        val pattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*\\W)(?!.* ).{8,16}$"
        return Regex(pattern).matches(password)
    }
}
