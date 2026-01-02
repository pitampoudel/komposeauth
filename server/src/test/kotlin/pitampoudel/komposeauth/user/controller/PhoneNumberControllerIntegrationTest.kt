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
import pitampoudel.komposeauth.user.data.SendOtpRequest
import pitampoudel.komposeauth.user.data.VerifyOtpRequest
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
    fun `send otp returns 401 for unauthenticated user`() {
        val request = SendOtpRequest(
            phoneNumber = "+1234567890"
        )

        mockMvc.post("/${ApiEndpoints.SEND_OTP}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = json.encodeToString(SendOtpRequest.serializer(), request)
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `verify phone number requires authentication`() {
        val request = VerifyOtpRequest(
            countryCode = null,
            phoneNumber = "+1234567890",
            otp = "123456"
        )

        mockMvc.post("/${ApiEndpoints.VERIFY_OTP}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = json.encodeToString(VerifyOtpRequest.serializer(), request)
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `verify phone number fails with invalid OTP`() {
        val email = "phone-verify@example.com"
        TestAuthHelpers.createUser(mockMvc, json, email)
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, email)

        val request = VerifyOtpRequest(
            countryCode = null,
            phoneNumber = "+1234567890",
            otp = "000000" // Invalid OTP
        )

        mockMvc.post("/${ApiEndpoints.VERIFY_OTP}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
            content = json.encodeToString(VerifyOtpRequest.serializer(), request)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `send otp rejects invalid phone number format`() {
        val email = "phone-invalid@example.com"
        TestAuthHelpers.createUser(mockMvc, json, email)
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, email)

        val request = SendOtpRequest(
            phoneNumber = "invalid-phone"
        )

        mockMvc.post("/${ApiEndpoints.SEND_OTP}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
            content = json.encodeToString(SendOtpRequest.serializer(), request)
        }.andExpect {
            status { isBadRequest() }
        }
    }
}
