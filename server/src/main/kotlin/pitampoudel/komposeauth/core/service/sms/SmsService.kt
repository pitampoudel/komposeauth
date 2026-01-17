package pitampoudel.komposeauth.core.service.sms

import org.springframework.scheduling.annotation.Async

interface SmsService {
    @Async("taskExecutor")
    fun sendSms(phoneNumber: String, message: String): Boolean
}