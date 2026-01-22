package pitampoudel.komposeauth.core.service.sms

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestTemplate
import pitampoudel.komposeauth.app_config.entity.AppConfig
import pitampoudel.komposeauth.app_config.service.AppConfigProvider
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.otp.repository.OtpRepository
import kotlin.test.assertTrue

class VerifyServiceConfigTest {

    private val mockAppConfigProvider = mockk<AppConfigProvider>()
    private val appConfigService = AppConfigService(mockAppConfigProvider)
    private val restTemplate = mockk<RestTemplate>()
    private val otpRepository = mockk<OtpRepository>()
    private val config = VerifyServiceConfig()

    @Test
    fun `verifyService returns TwilioPhoneNumberVerificationService when smsProvider is twilio`() {
        every { mockAppConfigProvider.get() } returns AppConfig(
            smsProvider = "twilio",
            twilioVerifyServiceSid = "VA123456"
        )

        val service = config.verifyService(appConfigService, restTemplate, otpRepository)

        assertTrue(service is TwilioPhoneNumberVerificationService)
    }

    @Test
    fun `verifyService returns SamayePhoneNumberVerificationService when smsProvider is samaye`() {
        every { mockAppConfigProvider.get() } returns AppConfig(
            smsProvider = "samaye",
            samayeApiKey = "key123"
        )

        val service = config.verifyService(appConfigService, restTemplate, otpRepository)

        assertTrue(service is SamayePhoneNumberVerificationService)
    }

    @Test
    fun `verifyService returns NoOpPhoneNumberVerificationService when smsProvider is null`() {
        every { mockAppConfigProvider.get() } returns AppConfig(
            smsProvider = null,
            twilioVerifyServiceSid = "VA123456",
            samayeApiKey = "key123"
        )

        val service = config.verifyService(appConfigService, restTemplate, otpRepository)

        assertTrue(service is NoOpPhoneNumberVerificationService)
    }

    @Test
    fun `verifyService returns NoOpPhoneNumberVerificationService when smsProvider is empty`() {
        every { mockAppConfigProvider.get() } returns AppConfig(
            smsProvider = "",
            twilioVerifyServiceSid = "VA123456",
            samayeApiKey = "key123"
        )

        val service = config.verifyService(appConfigService, restTemplate, otpRepository)

        assertTrue(service is NoOpPhoneNumberVerificationService)
    }

    @Test
    fun `verifyService returns NoOpPhoneNumberVerificationService when twilio selected but credentials missing`() {
        every { mockAppConfigProvider.get() } returns AppConfig(
            smsProvider = "twilio",
            twilioVerifyServiceSid = null
        )

        val service = config.verifyService(appConfigService, restTemplate, otpRepository)

        assertTrue(service is NoOpPhoneNumberVerificationService)
    }

    @Test
    fun `verifyService returns NoOpPhoneNumberVerificationService when samaye selected but credentials missing`() {
        every { mockAppConfigProvider.get() } returns AppConfig(
            smsProvider = "samaye",
            samayeApiKey = null
        )

        val service = config.verifyService(appConfigService, restTemplate, otpRepository)

        assertTrue(service is NoOpPhoneNumberVerificationService)
    }

    @Test
    fun `verifyService is case insensitive for provider name`() {
        every { mockAppConfigProvider.get() } returns AppConfig(
            smsProvider = "TWILIO",
            twilioVerifyServiceSid = "VA123456"
        )

        val service = config.verifyService(appConfigService, restTemplate, otpRepository)

        assertTrue(service is TwilioPhoneNumberVerificationService)
    }

    @Test
    fun `verifyService returns NoOpPhoneNumberVerificationService for unknown provider`() {
        every { mockAppConfigProvider.get() } returns AppConfig(
            smsProvider = "unknown",
            twilioVerifyServiceSid = "VA123456",
            samayeApiKey = "key123"
        )

        val service = config.verifyService(appConfigService, restTemplate, otpRepository)

        assertTrue(service is NoOpPhoneNumberVerificationService)
    }
}
