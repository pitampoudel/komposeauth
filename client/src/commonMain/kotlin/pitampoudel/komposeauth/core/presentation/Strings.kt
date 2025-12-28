package pitampoudel.komposeauth.core.presentation

import io.github.pitampoudel.client.generated.resources.Res
import io.github.pitampoudel.client.generated.resources.invalid_email
import io.github.pitampoudel.client.generated.resources.invalid_phone_number
import io.github.pitampoudel.client.generated.resources.invalid_url
import io.github.pitampoudel.client.generated.resources.must_be_selected
import io.github.pitampoudel.client.generated.resources.must_not_be_blank
import io.github.pitampoudel.client.generated.resources.must_not_be_empty
import io.github.pitampoudel.client.generated.resources.passwords_dont_match
import io.github.pitampoudel.client.generated.resources.too_long
import io.github.pitampoudel.client.generated.resources.too_short
import org.jetbrains.compose.resources.StringResource
import pitampoudel.core.domain.validators.GeneralValidationError
import pitampoudel.core.domain.validators.GeneralValidationError.VALIDATION_ERROR_INVALID_EMAIL
import pitampoudel.core.domain.validators.GeneralValidationError.VALIDATION_ERROR_INVALID_PHONE_NUMBER
import pitampoudel.core.domain.validators.GeneralValidationError.VALIDATION_ERROR_INVALID_URL
import pitampoudel.core.domain.validators.GeneralValidationError.VALIDATION_ERROR_MUST_BE_SELECTED
import pitampoudel.core.domain.validators.GeneralValidationError.VALIDATION_ERROR_MUST_NOT_BE_BLANK
import pitampoudel.core.domain.validators.GeneralValidationError.VALIDATION_ERROR_MUST_NOT_BE_EMPTY
import pitampoudel.core.domain.validators.GeneralValidationError.VALIDATION_ERROR_PASSWORDS_DONT_MATCH
import pitampoudel.core.domain.validators.GeneralValidationError.VALIDATION_ERROR_TOO_SHORT

fun GeneralValidationError.toStringRes(): StringResource {
    return when (this) {
        VALIDATION_ERROR_MUST_NOT_BE_BLANK -> Res.string.must_not_be_blank
        VALIDATION_ERROR_MUST_BE_SELECTED -> Res.string.must_be_selected
        VALIDATION_ERROR_TOO_SHORT -> Res.string.too_short
        VALIDATION_ERROR_INVALID_URL -> Res.string.invalid_url
        VALIDATION_ERROR_INVALID_EMAIL -> Res.string.invalid_email
        VALIDATION_ERROR_INVALID_PHONE_NUMBER -> Res.string.invalid_phone_number
        VALIDATION_ERROR_PASSWORDS_DONT_MATCH -> Res.string.passwords_dont_match
        VALIDATION_ERROR_MUST_NOT_BE_EMPTY -> Res.string.must_not_be_empty
        GeneralValidationError.VALIDATION_ERROR_TOO_LONG -> Res.string.too_long
        GeneralValidationError.VALIDATION_ERROR_PASSWORD_REQUIREMENT -> Res.string.validation_error_password_requirement
    }
}
