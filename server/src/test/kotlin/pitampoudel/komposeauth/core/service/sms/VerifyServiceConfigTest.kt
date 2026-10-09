package pitampoudel.komposeauth.core.service.sms

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.springframework.web.client.RestTemplate
import pitampoudel.komposeauth.app_config.entity.AppConfig
import pitampoudel.komposeauth.app_config.service.AppConfigProvider
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.otp.service.OtpCodes
import pitampoudel.komposeauth.otp.service.NoOpPhoneNumberVerificationService
import pitampoudel.komposeauth.otp.service.PhoneNumberVerificationService
import pitampoudel.komposeauth.otp.service.PhoneNumberVerificationServiceImpl
import pitampoudel.komposeauth.otp.service.TwilioPhoneNumberVerificationService
import pitampoudel.komposeauth.otp.service.VerifyServiceConfig
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class VerifyServiceConfigTest {

    @Mock
    private lateinit var mockAppConfigProvider: AppConfigProvider

    @Mock
    private lateinit var restTemplate: RestTemplate

    @Mock
    private lateinit var otpCodes: OtpCodes

    private lateinit var appConfigService: AppConfigService
    private val config = VerifyServiceConfig()

    @BeforeEach
    fun setUp() {
        appConfigService = AppConfigService(mockAppConfigProvider)
    }

    @Test
    fun `verifyService returns TwilioPhoneNumberVerificationService when smsProvider is twilio`() {
        whenever(mockAppConfigProvider.get()).thenReturn(
            AppConfig(
                smsProvider = "twilio",
                twilioVerifyServiceSid = "VA123456"
            )
        )

        val service = config.verifyService(appConfigService, restTemplate, otpCodes)

        assertTrue(service is TwilioPhoneNumberVerificationService)
    }

    @Test
    fun `verifyService returns SamayePhoneNumberVerificationService when smsProvider is samaye`() {
        whenever(mockAppConfigProvider.get()).thenReturn(
            AppConfig(
                smsProvider = "samaye",
                samayeApiKey = "key123"
            )
        )

        val service = config.verifyService(appConfigService, restTemplate, otpCodes)

        assertTrue(service is PhoneNumberVerificationServiceImpl)
    }

    @Test
    fun `verifyService returns PhoneNumberVerificationServiceImpl when smsProvider is sparrow`() {
        whenever(mockAppConfigProvider.get()).thenReturn(
            AppConfig(
                smsProvider = "sparrow",
                sparrowApiToken = "token123"
            )
        )

        val service = config.verifyService(appConfigService, restTemplate, otpCodes)

        assertTrue(service is PhoneNumberVerificationServiceImpl)
    }

    @Test
    fun `verifyService returns PhoneNumberVerificationServiceImpl when smsProvider is whatsapp`() {
        whenever(mockAppConfigProvider.get()).thenReturn(
            AppConfig(
                smsProvider = "whatsapp",
                whatsappAccessToken = "token123",
                whatsappPhoneNumberId = "1234567890"
            )
        )

        val service = config.verifyService(appConfigService, restTemplate, otpCodes)

        assertTrue(service is PhoneNumberVerificationServiceImpl)
    }

    @Test
    fun `verifyService returns NoOpPhoneNumberVerificationService when smsProvider is null`() {
        whenever(mockAppConfigProvider.get()).thenReturn(
            AppConfig(
                smsProvider = null,
                twilioVerifyServiceSid = "VA123456",
                samayeApiKey = "key123"
            )
        )

        val service = config.verifyService(appConfigService, restTemplate, otpCodes)

        assertTrue(service is NoOpPhoneNumberVerificationService)
    }

    @Test
    fun `verifyService returns NoOpPhoneNumberVerificationService when smsProvider is empty`() {
        whenever(mockAppConfigProvider.get()).thenReturn(
            AppConfig(
                smsProvider = "",
                twilioVerifyServiceSid = "VA123456",
                samayeApiKey = "key123"
            )
        )

        val service = config.verifyService(appConfigService, restTemplate, otpCodes)

        assertTrue(service is NoOpPhoneNumberVerificationService)
    }

    @Test
    fun `verifyService returns NoOpPhoneNumberVerificationService when twilio selected but credentials missing`() {
        whenever(mockAppConfigProvider.get()).thenReturn(
            AppConfig(
                smsProvider = "twilio",
                twilioVerifyServiceSid = null
            )
        )

        val service = config.verifyService(appConfigService, restTemplate, otpCodes)

        assertTrue(service is NoOpPhoneNumberVerificationService)
    }

    @Test
    fun `verifyService returns NoOpPhoneNumberVerificationService when samaye selected but credentials missing`() {
        whenever(mockAppConfigProvider.get()).thenReturn(
            AppConfig(
                smsProvider = "samaye",
                samayeApiKey = null
            )
        )

        val service = config.verifyService(appConfigService, restTemplate, otpCodes)

        assertTrue(service is NoOpPhoneNumberVerificationService)
    }

    @Test
    fun `verifyService returns NoOpPhoneNumberVerificationService when sparrow selected but credentials missing`() {
        whenever(mockAppConfigProvider.get()).thenReturn(
            AppConfig(
                smsProvider = "sparrow",
                sparrowApiToken = null
            )
        )

        val service = config.verifyService(appConfigService, restTemplate, otpCodes)

        assertTrue(service is NoOpPhoneNumberVerificationService)
    }

    @Test
    fun `verifyService returns NoOpPhoneNumberVerificationService when whatsapp selected but credentials missing`() {
        whenever(mockAppConfigProvider.get()).thenReturn(
            AppConfig(
                smsProvider = "whatsapp",
                whatsappAccessToken = null,
                whatsappPhoneNumberId = "1234567890"
            )
        )

        val service = config.verifyService(appConfigService, restTemplate, otpCodes)

        assertTrue(service is NoOpPhoneNumberVerificationService)
    }

    @Test
    fun `verifyService is case insensitive for provider name`() {
        whenever(mockAppConfigProvider.get()).thenReturn(
            AppConfig(
                smsProvider = "TWILIO",
                twilioVerifyServiceSid = "VA123456"
            )
        )

        val service = config.verifyService(appConfigService, restTemplate, otpCodes)

        assertTrue(service is TwilioPhoneNumberVerificationService)
    }

    @Test
    fun `verifyService returns NoOpPhoneNumberVerificationService for unknown provider`() {
        whenever(mockAppConfigProvider.get()).thenReturn(
            AppConfig(
                smsProvider = "unknown",
                twilioVerifyServiceSid = "VA123456",
                samayeApiKey = "key123"
            )
        )

        val service = config.verifyService(appConfigService, restTemplate, otpCodes)

        assertTrue(service is NoOpPhoneNumberVerificationService)
    }

    @Test
    fun `the service follows a provider configured after startup`() {
        whenever(mockAppConfigProvider.get()).thenReturn(AppConfig(smsProvider = null))
        val service = config.phoneNumberVerificationService(appConfigService, restTemplate, otpCodes)
        assertFalse(service.verify("+9779800000000", "123456"))

        whenever(mockAppConfigProvider.get()).thenReturn(AppConfig(smsProvider = "sparrow", sparrowApiToken = "token123"))
        whenever(otpCodes.redeem("+9779800000000", "123456")).thenReturn(true)
        assertTrue(service.verify("+9779800000000", "123456"))
    }
}
