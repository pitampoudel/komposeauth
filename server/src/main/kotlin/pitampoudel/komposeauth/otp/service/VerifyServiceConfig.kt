package pitampoudel.komposeauth.otp.service

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.core.service.sms.SamayaSmsService
import pitampoudel.komposeauth.core.service.sms.SparrowSmsService
import pitampoudel.komposeauth.core.service.sms.WhatsAppSmsService

@Configuration
class VerifyServiceConfig {
    /**
     * The provider is chosen from the config on every call rather than once at boot, so one picked or
     * changed on the config page of a running server is used without a restart.
     */
    @Bean
    fun phoneNumberVerificationService(
        appConfigService: AppConfigService,
        restTemplate: RestTemplate,
        otpCodes: OtpCodes,
    ): PhoneNumberVerificationService = object : PhoneNumberVerificationService {
        override fun initiate(phoneNumber: String) =
            verifyService(appConfigService, restTemplate, otpCodes).initiate(phoneNumber)

        override fun verify(phoneNumber: String, code: String) =
            verifyService(appConfigService, restTemplate, otpCodes).verify(phoneNumber, code)
    }

    fun verifyService(
        appConfigService: AppConfigService,
        restTemplate: RestTemplate,
        otpCodes: OtpCodes,
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
                PhoneNumberVerificationServiceImpl(
                    otpCodes = otpCodes,
                    appConfigService = appConfigService,
                    smsService = SamayaSmsService(
                        appConfigService = appConfigService,
                        restTemplate = restTemplate
                    )
                )
            }

            "sparrow" -> if (config.sparrowApiToken.isNullOrBlank()) {
                NoOpPhoneNumberVerificationService()
            } else {
                PhoneNumberVerificationServiceImpl(
                    otpCodes = otpCodes,
                    appConfigService = appConfigService,
                    smsService = SparrowSmsService(
                        appConfigService = appConfigService,
                        restTemplate = restTemplate
                    )
                )
            }

            "whatsapp" -> if (config.whatsappAccessToken.isNullOrBlank() || config.whatsappPhoneNumberId.isNullOrBlank()) {
                NoOpPhoneNumberVerificationService()
            } else {
                PhoneNumberVerificationServiceImpl(
                    otpCodes = otpCodes,
                    appConfigService = appConfigService,
                    smsService = WhatsAppSmsService(
                        appConfigService = appConfigService,
                        restTemplate = restTemplate
                    )
                )
            }

            else -> NoOpPhoneNumberVerificationService()
        }
    }
}