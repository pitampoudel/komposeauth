package pitampoudel.komposeauth.core.service.sms

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.phone_otp.repository.PhoneOtpRepository

@Configuration
class VerifyServiceConfig {
    @Bean
    fun verifyService(
        appConfigService: AppConfigService,
        restTemplate: RestTemplate,
        phoneOtpRepository: PhoneOtpRepository,
        smsService: SmsService
    ): PhoneNumberVerificationService {
        return if (!appConfigService.getConfig().twilioVerifyServiceSid.isNullOrBlank()) {
            TwilioPhoneNumberVerificationService(appConfigService, restTemplate)
        } else if (!appConfigService.getConfig().samayeApiKey.isNullOrBlank()) {
            SamayePhoneNumberVerificationService(phoneOtpRepository, smsService, appConfigService)
        } else {
            NoOpPhoneNumberVerificationService()
        }
    }
}