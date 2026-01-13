package pitampoudel.komposeauth.core.service.email

import org.springframework.stereotype.Service
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.core.service.EmailService
import pitampoudel.komposeauth.otp.entity.Otp
import pitampoudel.komposeauth.otp.repository.OtpRepository
import kotlin.random.Random

@Service
class EmailVerificationService(
    private val otpRepository: OtpRepository,
    private val emailService: EmailService,
    private val appConfigService: AppConfigService,
) {
    fun initiate(email: String, baseUrl: String): Boolean {
        val otp = generateOtp()
        otpRepository.save(
            Otp(
                receiver = email,
                otp = otp
            )
        )
        return emailService.sendHtmlMail(
            baseUrl = baseUrl,
            to = email,
            subject = "Your ${appConfigService.getConfig().name} verification code",
            template = "email/generic",
            model = mapOf(
                "message" to "Use the code $otp to verify your email. The code expires in 5 minutes.",
                "recipientName" to "User",
            )
        )
    }

    fun verify(email: String, code: String): Boolean {
        val otpRecords = otpRepository.findByReceiverOrderByCreatedAtDesc(email)
        if (otpRecords.isEmpty()) {
            return false
        }
        val latestOtp = otpRecords.first()
        if (latestOtp.otp == code && !latestOtp.isExpired()) {
            otpRepository.deleteByReceiver(email)
            return true
        }
        return false
    }

    private fun generateOtp(): String {
        return Random.nextInt(100000, 999999).toString()
    }
}

