package com.vardansoft.komposeauth.ui.otp

import com.vardansoft.komposeauth.data.UpdatePhoneNumberRequest
import com.vardansoft.komposeauth.data.VerifyPhoneOtpRequest
import com.vardansoft.core.presentation.InfoMessage
import com.vardansoft.core.domain.validators.AuthValidationError

data class OtpState(
    val progress: Float? = null,
    val infoMsg: InfoMessage? = null,
    val req: UpdatePhoneNumberRequest? = null,
    val code: String = "",
    val codeError: AuthValidationError? = null
) {
    fun containsError() = codeError != null
    fun verifyParam() = if (containsError() || req == null) null else VerifyPhoneOtpRequest(
        phoneNumber = req.phoneNumber,
        countryCode = req.countryCode,
        otp = code
    )

}


