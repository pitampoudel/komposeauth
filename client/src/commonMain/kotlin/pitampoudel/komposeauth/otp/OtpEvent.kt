package pitampoudel.komposeauth.otp

import pitampoudel.komposeauth.user.data.SendOtpRequest

sealed interface OtpEvent {
    data object DismissInfoMsg : OtpEvent
    class PhoneNumberChanged(val req: SendOtpRequest) : OtpEvent
    object SendOtp : OtpEvent
    class CodeChanged(val value: String) : OtpEvent
    data object Verify : OtpEvent
    data object Login : OtpEvent
}