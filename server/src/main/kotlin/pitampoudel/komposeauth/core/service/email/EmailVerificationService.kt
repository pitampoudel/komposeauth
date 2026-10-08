package pitampoudel.komposeauth.core.service.email

import org.springframework.stereotype.Service
import pitampoudel.core.data.MessageResponse
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.core.service.EmailService
import pitampoudel.komposeauth.core.utils.normalizedEmail
import pitampoudel.komposeauth.otp.service.OtpCodes

@Service
class EmailVerificationService(
    private val otpCodes: OtpCodes,
    private val emailService: EmailService,
    private val appConfigService: AppConfigService,
) {
    fun initiate(email: String, baseUrl: String): MessageResponse {
        val otp = otpCodes.issue(email.normalizedEmail())
        val sent = emailService.sendHtmlMail(
            baseUrl = baseUrl,
            to = email,
            subject = "Your ${appConfigService.getConfig().name} verification code",
            template = "email/generic",
            model = mapOf(
                "message" to "Use the code $otp to verify your email. The code expires in 5 minutes.",
                "recipientName" to "User",
            )
        )
        return if (sent) {
            MessageResponse("An OTP has just been sent to $email")
        } else {
            MessageResponse("Failed to send an OTP to $email")
        }
    }

    fun verify(email: String, code: String): Boolean = otpCodes.redeem(email.normalizedEmail(), code)
}
