package pitampoudel.komposeauth.otp.service

interface PhoneNumberVerificationService {
    fun initiate(phoneNumber: String)
    fun verify(phoneNumber: String, code: String): Boolean
}