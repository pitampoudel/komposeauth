package pitampoudel.komposeauth.core.service.sms

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import pitampoudel.komposeauth.app_config.service.AppConfigService

@Configuration
class SmsServiceConfig {

    @Bean
    fun smsService(appConfigService: AppConfigService, restTemplate: RestTemplate): SmsService {

        return when {
            !appConfigService.getConfig().twilioAccountSid.isNullOrBlank() -> {
                TwilioSmsService(appConfigService, restTemplate)
            }

            !appConfigService.getConfig().samayeApiKey.isNullOrBlank() -> {
                SamayaSmsService(appConfigService, restTemplate)
            }

            else -> NoOpSmsService()
        }
    }
}