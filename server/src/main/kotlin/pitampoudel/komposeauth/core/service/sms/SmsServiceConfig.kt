package pitampoudel.komposeauth.core.service.sms

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import pitampoudel.komposeauth.app_config.service.AppConfigProvider

@Configuration
class SmsServiceConfig {

    @Bean
    fun smsService(appConfigProvider: AppConfigProvider, restTemplate: RestTemplate): SmsService {

        return when {
            !appConfigProvider.getConfig().twilioAccountSid.isNullOrBlank() -> {
                TwilioSmsService(appConfigProvider, restTemplate)
            }

            !appConfigProvider.getConfig().samayeApiKey.isNullOrBlank() -> {
                SamayaSmsService(appConfigProvider, restTemplate)
            }

            else -> NoOpSmsService()
        }
    }
}