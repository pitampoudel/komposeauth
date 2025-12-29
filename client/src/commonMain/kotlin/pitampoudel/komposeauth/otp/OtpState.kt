package pitampoudel.komposeauth.otp

import pitampoudel.core.domain.validators.GeneralValidationError
import pitampoudel.core.presentation.InfoMessage
import pitampoudel.komposeauth.user.data.UpdatePhoneNumberRequest
import pitampoudel.komposeauth.user.data.VerifyPhoneOtpRequest

data class OtpState(
    val progress: Float? = null,
    val infoMsg: InfoMessage? = null,
    val req: UpdatePhoneNumberRequest? = null,
    val code: String = "",
    val codeError: GeneralValidationError? = null
) {
    fun containsError() = codeError != null
    fun verifyParam() = if (containsError() || req == null) null else VerifyPhoneOtpRequest(
        phoneNumber = req.phoneNumber,
        countryCode = req.countryCode,
        otp = code
    )

}


