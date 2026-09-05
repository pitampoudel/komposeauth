package pitampoudel.komposeauth.otp.service

import org.slf4j.LoggerFactory
import pitampoudel.core.data.MessageResponse

class NoOpPhoneNumberVerificationService : PhoneNumberVerificationService {
    private val logger = LoggerFactory.getLogger(NoOpPhoneNumberVerificationService::class.java)

    override fun initiate(phoneNumber: String): MessageResponse {
        logger.debug("NoOpPhoneNumberVerificationService: initiate called but no verification provider configured. phone=$phoneNumber")
        return MessageResponse("No verification provider configured.")
    }

    override fun verify(phoneNumber: String, code: String): Boolean {
        logger.debug("NoOpPhoneNumberVerificationService: verify called but no verification provider configured. phone=$phoneNumber code=$code")
        return false
    }
}
