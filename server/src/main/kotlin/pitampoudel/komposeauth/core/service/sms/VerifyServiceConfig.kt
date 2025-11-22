package pitampoudel.komposeauth.core.service.sms

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import pitampoudel.komposeauth.app_config.service.AppConfigProvider
import pitampoudel.komposeauth.user.repository.PhoneOtpRepository

@Configuration
class VerifyServiceConfig {
    @Bean
    fun verifyService(
        appConfigProvider: AppConfigProvider,
        restTemplate: RestTemplate,
        phoneOtpRepository: PhoneOtpRepository,
        smsService: SmsService
    ): PhoneNumberVerificationService {
        return if (!appConfigProvider.twilioVerifyServiceSid.isNullOrBlank()) {
            TwilioPhoneNumberVerificationService(appConfigProvider, restTemplate)
        } else if (!appConfigProvider.samayeApiKey.isNullOrBlank()) {
            SamayePhoneNumberVerificationService(phoneOtpRepository, smsService, appConfigProvider)
        } else {
            NoOpPhoneNumberVerificationService()
        }
    }
}