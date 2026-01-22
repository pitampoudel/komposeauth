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
        val provider = config.smsProvider?.lowercase()?.takeIf { it.isNotBlank() }
        return when (provider) {
            "twilio" -> if (config.twilioVerifyServiceSid.isNullOrBlank()) {
                NoOpPhoneNumberVerificationService()
            } else {
                TwilioPhoneNumberVerificationService(
                    appConfigService = appConfigService,
                    restTemplate = restTemplate
                )
            }

            "samaye" -> if (config.samayeApiKey.isNullOrBlank()) {
                NoOpPhoneNumberVerificationService()
            } else {
                SamayePhoneNumberVerificationService(
                    otpRepository = otpRepository,
                    appConfigService = appConfigService,
                    restTemplate = restTemplate
                )
            }

            else -> NoOpPhoneNumberVerificationService()
        }
    }
}