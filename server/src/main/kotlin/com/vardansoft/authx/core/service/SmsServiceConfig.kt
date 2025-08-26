package com.vardansoft.authx.core.service

import com.vardansoft.authx.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class SmsServiceConfig {

    private val logger = LoggerFactory.getLogger(SmsServiceConfig::class.java)

    @Bean
    fun smsService(appProperties: AppProperties, restTemplate: RestTemplate): SmsService {
        val hasTwilio = !appProperties.twilioAccountSid.isNullOrBlank() &&
                !appProperties.twilioAuthToken.isNullOrBlank() &&
                !appProperties.twilioFromNumber.isNullOrBlank()

        val hasSamaya = try {
            !appProperties.smsApiKey.isNullOrBlank()
        } catch (_: Exception) {
            false
        }

        return when {
            hasTwilio -> {
                logger.info("Using TwilioSmsService for SMS delivery")
                TwilioSmsService(appProperties, restTemplate)
            }

            hasSamaya -> {
                logger.info("Using SamayaSmsService for SMS delivery")
                SamayaSmsService(appProperties, restTemplate)
            }

            else -> throw Exception("No SMS service configured")

        }
    }
}
