package com.vardansoft.auth.ui.otp

import com.vardansoft.auth.data.UpdatePhoneNumberRequest
import com.vardansoft.auth.data.VerifyPhoneOtpRequest

data class OtpState(
    val progress: Float? = null,
    val infoMsg: String? = null,
    val req: UpdatePhoneNumberRequest? = null,
    val code: String = "",
    val codeError: String? = null
) {
    fun containsError() = codeError != null
    fun verifyParam() = if (containsError()) null else VerifyPhoneOtpRequest(otp = code)

}


