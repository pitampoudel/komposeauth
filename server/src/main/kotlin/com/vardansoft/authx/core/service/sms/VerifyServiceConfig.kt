package com.vardansoft.authx.core.service.sms

import com.vardansoft.authx.AppProperties
import com.vardansoft.authx.user.repository.PhoneOtpRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class VerifyServiceConfig {
    @Bean
    fun verifyService(
        appProperties: AppProperties,
        restTemplate: RestTemplate,
        phoneOtpRepository: PhoneOtpRepository,
        smsService: SmsService
    ): PhoneNumberVerificationService {
        return if (!appProperties.twilioVerifyServiceSid.isNullOrBlank()) {
            TwilioPhoneNumberVerificationService(appProperties, restTemplate)
        } else if (!appProperties.samayeApiKey.isNullOrBlank()) {
            SamayePhoneNumberVerificationService(phoneOtpRepository, smsService, appProperties)
        } else {
            throw Exception("No SMS service configured")
        }
    }
}