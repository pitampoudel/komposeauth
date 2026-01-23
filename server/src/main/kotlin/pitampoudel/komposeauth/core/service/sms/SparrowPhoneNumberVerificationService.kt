package pitampoudel.komposeauth.core.service.sms

import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.otp.entity.Otp
import pitampoudel.komposeauth.otp.repository.OtpRepository
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestTemplate
import org.springframework.web.server.ResponseStatusException
import java.time.Duration
import java.time.Instant
import kotlin.random.Random

class SparrowPhoneNumberVerificationService(
    private val otpRepository: OtpRepository,
    private val appConfigService: AppConfigService,
    private val restTemplate: RestTemplate
) : PhoneNumberVerificationService {

    private val sparrowSmsService by lazy { SparrowSmsService(appConfigService, restTemplate) }

    override fun initiate(phoneNumber: String): Boolean {
        val resendCooldown = Duration.ofSeconds(60)
        val now = Instant.now()
        val recentOtp = otpRepository.findByReceiverOrderByCreatedAtDesc(phoneNumber).firstOrNull()
        if (recentOtp != null && recentOtp.createdAt.isAfter(now.minus(resendCooldown))) {
            throw ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "OTP already sent. Please wait ${resendCooldown.seconds} seconds before requesting again."
            )
        }
        val otp = generateOtp()
        otpRepository.save(
            Otp(
                receiver = phoneNumber,
                otp = otp
            )
        )
        if (appConfigService.getConfig().sparrowApiToken.isNullOrBlank()) {
            return false
        }
        return sparrowSmsService.sendSms(
            phoneNumber,
            "Your OTP for ${appConfigService.getConfig().name} is $otp"
        )
    }

    override fun verify(phoneNumber: String, code: String): Boolean {
        val otpRecords = otpRepository.findByReceiverOrderByCreatedAtDesc(phoneNumber)
        if (otpRecords.isEmpty()) {
            return false
        }
        val latestOtp = otpRecords.first()
        if (latestOtp.otp == code && !latestOtp.isExpired()) {
            otpRepository.deleteByReceiver(phoneNumber)
            return true
        }
        return false
    }

    private fun generateOtp(): String {
        return Random.nextInt(100000, 999999).toString()
    }
}
