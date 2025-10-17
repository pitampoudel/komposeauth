package com.vardansoft.komposeauth.ui.core.presentation

import com.vardansoft.core.domain.validators.AuthValidationError
import com.vardansoft.core.domain.validators.AuthValidationError.*
import com.vardansoft.ui.generated.resources.Res
import com.vardansoft.ui.generated.resources.invalid_email
import com.vardansoft.ui.generated.resources.invalid_phone_number
import com.vardansoft.ui.generated.resources.invalid_url
import com.vardansoft.ui.generated.resources.must_be_selected
import com.vardansoft.ui.generated.resources.must_not_be_blank
import com.vardansoft.ui.generated.resources.must_not_be_empty
import com.vardansoft.ui.generated.resources.passwords_dont_match
import com.vardansoft.ui.generated.resources.too_short
import org.jetbrains.compose.resources.StringResource

fun AuthValidationError.toStringRes(): StringResource {
    return when (this) {
        VALIDATION_ERROR_MUST_NOT_BE_BLANK -> Res.string.must_not_be_blank
        VALIDATION_ERROR_MUST_BE_SELECTED -> Res.string.must_be_selected
        VALIDATION_ERROR_TOO_SHORT -> Res.string.too_short
        VALIDATION_ERROR_INVALID_URL -> Res.string.invalid_url
        VALIDATION_ERROR_INVALID_EMAIL -> Res.string.invalid_email
        VALIDATION_ERROR_INVALID_PHONE_NUMBER -> Res.string.invalid_phone_number
        VALIDATION_ERROR_PASSWORDS_DONT_MATCH -> Res.string.passwords_dont_match
        VALIDATION_ERROR_MUST_NOT_BE_EMPTY -> Res.string.must_not_be_empty
    }
}
