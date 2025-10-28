package pitampoudel.komposeauth.core.service.sms

interface PhoneNumberVerificationService {
    fun initiate(phoneNumber: String): Boolean
    fun verify(phoneNumber: String, code: String): Boolean
}