package com.vardansoft.authx.ui.otp

import com.vardansoft.authx.data.UpdatePhoneNumberRequest
import com.vardansoft.authx.data.VerifyPhoneOtpRequest

internal data class OtpState(
    val progress: Float? = null,
    val infoMsg: String? = null,
    val req: UpdatePhoneNumberRequest? = null,
    val code: String = "",
    val codeError: String? = null
) {
    fun containsError() = codeError != null
    fun verifyParam() = if (containsError() || req == null) null else VerifyPhoneOtpRequest(
        phoneNumber = req.phoneNumber,
        countryCode = req.countryCode,
        otp = code
    )

}


