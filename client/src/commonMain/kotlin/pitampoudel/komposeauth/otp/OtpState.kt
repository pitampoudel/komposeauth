package pitampoudel.komposeauth.otp

import pitampoudel.core.domain.validators.GeneralValidationError
import pitampoudel.core.presentation.InfoMessage
import pitampoudel.komposeauth.user.data.Credential
import pitampoudel.komposeauth.user.data.SendOtpRequest
import pitampoudel.komposeauth.user.data.VerifyOtpRequest

data class OtpState(
    val progress: Float? = null,
    val infoMsg: InfoMessage? = null,
    val req: SendOtpRequest? = null,
    val code: String = "",
    val codeError: GeneralValidationError? = null
) {
    fun containsError() = codeError != null
    fun asVerifyRequest() = if (containsError() || req == null) null else VerifyOtpRequest(
        otp = code
    )

    fun asLoginCredential() = if (containsError() || req == null) null else Credential.OTP(
        username = req.username,
        otp = code
    )

}


