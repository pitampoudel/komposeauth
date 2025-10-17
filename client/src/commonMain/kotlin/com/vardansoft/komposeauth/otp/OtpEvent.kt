package com.vardansoft.komposeauth.otp

import com.vardansoft.komposeauth.data.UpdatePhoneNumberRequest

sealed interface OtpEvent {
    data object DismissInfoMsg : OtpEvent
    class CodeChanged(val value: String) : OtpEvent
    object SendOtp : OtpEvent
    class SubmitPhoneNumber(val req: UpdatePhoneNumberRequest) : OtpEvent
    data object Verify : OtpEvent
}