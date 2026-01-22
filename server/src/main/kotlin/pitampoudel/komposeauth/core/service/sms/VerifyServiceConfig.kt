package pitampoudel.komposeauth.core.service.sms

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.otp.repository.OtpRepository

@Configuration
class VerifyServiceConfig {
    @Bean
    fun verifyService(
        appConfigService: AppConfigService,
        restTemplate: RestTemplate,
        otpRepository: OtpRepository,
    ): PhoneNumberVerificationService {
        val config = appConfigService.getConfig()
        return when (config.smsProvider?.lowercase()) {
            "twilio" -> {
                if (config.twilioVerifyServiceSid.isNullOrBlank()) {
                    NoOpPhoneNumberVerificationService()
                } else {
                    TwilioPhoneNumberVerificationService(appConfigService, restTemplate)
                }
            }
            "samaye" -> {
                if (config.samayeApiKey.isNullOrBlank()) {
                    NoOpPhoneNumberVerificationService()
                } else {
                    SamayePhoneNumberVerificationService(otpRepository, appConfigService, restTemplate)
                }
            }
            else -> NoOpPhoneNumberVerificationService()
        }
    }
}