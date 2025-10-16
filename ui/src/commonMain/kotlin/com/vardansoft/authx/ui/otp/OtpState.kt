package com.vardansoft.authx.ui.otp

import com.vardansoft.authx.data.UpdatePhoneNumberRequest
import com.vardansoft.authx.data.VerifyPhoneOtpRequest
import com.vardansoft.core.presentation.InfoMessage
import com.vardansoft.core.domain.validators.AuthXValidationError

data class OtpState(
    val progress: Float? = null,
    val infoMsg: InfoMessage? = null,
    val req: UpdatePhoneNumberRequest? = null,
    val code: String = "",
    val codeError: AuthXValidationError? = null
) {
    fun containsError() = codeError != null
    fun verifyParam() = if (containsError() || req == null) null else VerifyPhoneOtpRequest(
        phoneNumber = req.phoneNumber,
        countryCode = req.countryCode,
        otp = code
    )

}


