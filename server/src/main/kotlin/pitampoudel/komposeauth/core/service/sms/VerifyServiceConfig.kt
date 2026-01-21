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
        return if (!appConfigService.getConfig().twilioVerifyServiceSid.isNullOrBlank()) {
            TwilioPhoneNumberVerificationService(appConfigService, restTemplate)
        } else if (!appConfigService.getConfig().samayeApiKey.isNullOrBlank()) {
            SamayePhoneNumberVerificationService(otpRepository, appConfigService, restTemplate)
        } else {
            NoOpPhoneNumberVerificationService()
        }
    }
}