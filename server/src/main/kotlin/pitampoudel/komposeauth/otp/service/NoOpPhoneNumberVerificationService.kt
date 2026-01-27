package pitampoudel.komposeauth.otp.service

import org.slf4j.LoggerFactory

class NoOpPhoneNumberVerificationService : PhoneNumberVerificationService {
    private val logger = LoggerFactory.getLogger(NoOpPhoneNumberVerificationService::class.java)

    override fun initiate(phoneNumber: String) {
        logger.debug("NoOpPhoneNumberVerificationService: initiate called but no verification provider configured. phone=$phoneNumber")
    }

    override fun verify(phoneNumber: String, code: String): Boolean {
        logger.debug("NoOpPhoneNumberVerificationService: verify called but no verification provider configured. phone=$phoneNumber code=$code")
        return false
    }
}
