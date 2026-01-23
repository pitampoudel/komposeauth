package pitampoudel.komposeauth.core.service.sms

interface SmsService {

    fun sendSms(phoneNumber: String, message: String): Boolean
}
