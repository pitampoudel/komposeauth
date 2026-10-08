package pitampoudel.komposeauth.otp.service

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestTemplate
import pitampoudel.komposeauth.app_config.entity.AppConfig
import pitampoudel.komposeauth.app_config.service.AppConfigProvider
import pitampoudel.komposeauth.app_config.service.AppConfigService
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TwilioPhoneNumberVerificationServiceTest {

    private val restTemplate = RestTemplate()
    private val server = MockRestServiceServer.bindTo(restTemplate).build()
    private val service = TwilioPhoneNumberVerificationService(
        appConfigService = AppConfigService(mock<AppConfigProvider>().also {
            whenever(it.get()).thenReturn(
                AppConfig(twilioAccountSid = "AC1", twilioAuthToken = "secret", twilioVerifyServiceSid = "VA1")
            )
        }),
        restTemplate = restTemplate
    )

    private fun check() = server.expect(requestTo("https://verify.twilio.com/v2/Services/VA1/VerificationCheck"))
        .andExpect(method(HttpMethod.POST))

    @Test
    fun `a wrong code, which Twilio answers with 200 and pending, is not a match`() {
        check().andRespond(withSuccess("""{"status":"pending","valid":false}""", MediaType.APPLICATION_JSON))
        assertFalse(service.verify("+9779800000000", "111111"))
    }

    @Test
    fun `an approved check is a match`() {
        check().andRespond(withSuccess("""{"status":"approved","valid":true}""", MediaType.APPLICATION_JSON))
        assertTrue(service.verify("+9779800000000", "123456"))
    }

    @Test
    fun `a verification Twilio no longer has is not a match`() {
        check().andRespond(withStatus(HttpStatus.NOT_FOUND))
        assertFalse(service.verify("+9779800000000", "123456"))
    }
}
