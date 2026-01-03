package pitampoudel.komposeauth.core.service.sms

import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.phone_otp.entity.PhoneOtp
import pitampoudel.komposeauth.phone_otp.repository.PhoneOtpRepository
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.time.Duration
import java.time.Instant
import kotlin.random.Random

class SamayePhoneNumberVerificationService(
    private val phoneOtpRepository: PhoneOtpRepository,
    private val smsService: SmsService,
    private val appConfigService: AppConfigService
) : PhoneNumberVerificationService {

    override fun initiate(phoneNumber: String): Boolean {
        val resendCooldown = Duration.ofSeconds(60)
        val now = Instant.now()
        val recentOtp = phoneOtpRepository.findByPhoneNumberOrderByCreatedAtDesc(phoneNumber).firstOrNull()
        if (recentOtp != null && recentOtp.createdAt.isAfter(now.minus(resendCooldown))) {
            throw ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "OTP already sent. Please wait ${resendCooldown.seconds} seconds before requesting again."
            )
        }
        val otp = generateOtp()
        phoneOtpRepository.save(
            PhoneOtp(
                phoneNumber = phoneNumber,
                otp = otp
            )
        )
        return smsService.sendSms(phoneNumber, "Your OTP for ${appConfigService.getConfig().name} is $otp")
    }

    override fun verify(phoneNumber: String, code: String): Boolean {
        val otpRecords = phoneOtpRepository.findByPhoneNumberOrderByCreatedAtDesc(phoneNumber)
        if (otpRecords.isEmpty()) {
            return false
        }
        val latestOtp = otpRecords.first()
        if (latestOtp.otp == code && !latestOtp.isExpired()) {
            phoneOtpRepository.deleteByPhoneNumber(phoneNumber)
            return true
        }
        return false
    }

    private fun generateOtp(): String {
        return Random.nextInt(100000, 999999).toString()
    }
}
