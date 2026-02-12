package pitampoudel.core.domain.validators

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import pitampoudel.core.domain.now

/**
 * Ensures a date is not in the future relative to the current system time zone.
 */
object ValidateDateNotInFuture {
    operator fun invoke(value: LocalDate?): ValidationResult {
        if (value == null) {
            return ValidationResult.Error(GeneralValidationError.VALIDATION_ERROR_MUST_BE_SELECTED)
        }
        val today = now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return if (value > today) {
            ValidationResult.Error(GeneralValidationError.VALIDATION_ERROR_DATE_IN_FUTURE)
        } else {
            ValidationResult.Success
        }
    }
}

