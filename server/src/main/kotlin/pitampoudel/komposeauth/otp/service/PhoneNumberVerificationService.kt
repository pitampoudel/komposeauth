package pitampoudel.komposeauth.otp.service

import pitampoudel.core.data.MessageResponse

interface PhoneNumberVerificationService {
    fun initiate(phoneNumber: String): MessageResponse
    fun verify(phoneNumber: String, code: String): Boolean
}