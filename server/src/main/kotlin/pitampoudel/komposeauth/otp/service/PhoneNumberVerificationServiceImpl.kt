package pitampoudel.komposeauth.otp.service

import pitampoudel.core.data.MessageResponse
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.core.service.sms.SmsService
import pitampoudel.komposeauth.core.service.sms.WhatsAppSmsService

class PhoneNumberVerificationServiceImpl(
    private val smsService: SmsService,
    private val otpCodes: OtpCodes,
    private val appConfigService: AppConfigService
) : PhoneNumberVerificationService {

    override fun initiate(phoneNumber: String): MessageResponse {
        val otp = otpCodes.issue(phoneNumber)
        smsService.sendSms(
            phoneNumber = phoneNumber,
            message = "Your OTP for ${appConfigService.getConfig().name} is $otp"
        )
        return when (smsService) {
            is WhatsAppSmsService -> MessageResponse("An OTP has just been sent to $phoneNumber via WhatsApp")
            else -> MessageResponse("An OTP has just been sent to $phoneNumber")
        }
    }

    override fun verify(phoneNumber: String, code: String): Boolean = otpCodes.redeem(phoneNumber, code)
}
