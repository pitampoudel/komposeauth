package pitampoudel.komposeauth.core.service.sms

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async

class NoOpSmsService : SmsService {
    private val logger = LoggerFactory.getLogger(NoOpSmsService::class.java)

    @Async("taskExecutor")
    override fun sendSms(phoneNumber: String, message: String): Boolean {
        logger.debug("NoOpSmsService: SMS not sent because no SMS provider is configured. To=$phoneNumber Message=$message")
        return false
    }
}
