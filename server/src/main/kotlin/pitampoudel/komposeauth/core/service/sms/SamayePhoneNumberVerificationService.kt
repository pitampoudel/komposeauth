package pitampoudel.komposeauth.core.service.sms

import pitampoudel.komposeauth.app_config.service.AppConfigProvider
import pitampoudel.komposeauth.user.entity.PhoneOtp
import pitampoudel.komposeauth.user.repository.PhoneOtpRepository
import kotlin.random.Random

class SamayePhoneNumberVerificationService(
    private val phoneOtpRepository: PhoneOtpRepository,
    private val smsService: SmsService,
    private val appConfigProvider: AppConfigProvider
) : PhoneNumberVerificationService {

    override fun initiate(phoneNumber: String): Boolean {
        val otp = generateOtp()
        phoneOtpRepository.save(
            PhoneOtp(
                phoneNumber = phoneNumber,
                otp = otp
            )
        )
        return smsService.sendSms(phoneNumber, "Your OTP for ${appConfigProvider.getConfig().name} is $otp")
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
