package pitampoudel.komposeauth.user.controller

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import pitampoudel.komposeauth.TestAuthHelpers
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.core.data.UpdatePhoneNumberRequest
import pitampoudel.komposeauth.core.data.VerifyPhoneOtpRequest
import pitampoudel.komposeauth.core.domain.ApiEndpoints

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
@AutoConfigureMockMvc
class PhoneNumberControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var json: Json


    @Test
    fun `initiatePhoneNumberUpdate returns 401 for unauthenticated user`() {
        val request = UpdatePhoneNumberRequest(
            phoneNumber = "+1234567890"
        )

        mockMvc.post("/${ApiEndpoints.UPDATE_PHONE_NUMBER}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = json.encodeToString(UpdatePhoneNumberRequest.serializer(), request)
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `verifyPhoneNumberUpdate requires authentication`() {
        val request = VerifyPhoneOtpRequest(
            countryCode = null,
            phoneNumber = "+1234567890",
            otp = "123456"
        )

        mockMvc.post("/${ApiEndpoints.VERIFY_PHONE_NUMBER}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = json.encodeToString(VerifyPhoneOtpRequest.serializer(), request)
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `verifyPhoneNumberUpdate fails with invalid OTP`() {
        val email = "phone-verify@example.com"
        TestAuthHelpers.createUser(mockMvc, json, email)
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, email)

        val request = VerifyPhoneOtpRequest(
            countryCode = null,
            phoneNumber = "+1234567890",
            otp = "000000" // Invalid OTP
        )

        mockMvc.post("/${ApiEndpoints.VERIFY_PHONE_NUMBER}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
            content = json.encodeToString(VerifyPhoneOtpRequest.serializer(), request)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `initiatePhoneNumberUpdate rejects invalid phone number format`() {
        val email = "phone-invalid@example.com"
        TestAuthHelpers.createUser(mockMvc, json, email)
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, email)

        val request = UpdatePhoneNumberRequest(
            phoneNumber = "invalid-phone"
        )

        mockMvc.post("/${ApiEndpoints.UPDATE_PHONE_NUMBER}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
            content = json.encodeToString(UpdatePhoneNumberRequest.serializer(), request)
        }.andExpect {
            status { isBadRequest() }
        }
    }
}
