package pitampoudel.komposeauth.core.service.sms

import pitampoudel.komposeauth.AppProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class SmsServiceConfig {

    @Bean
    fun smsService(appProperties: AppProperties, restTemplate: RestTemplate): SmsService {

        return when {
            !appProperties.twilioAccountSid.isNullOrBlank() -> {
                TwilioSmsService(appProperties, restTemplate)
            }

            !appProperties.samayeApiKey.isNullOrBlank() -> {
                SamayaSmsService(appProperties, restTemplate)
            }

            else -> NoOpSmsService()
        }
    }
}