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
import pitampoudel.komposeauth.user.data.CreateUserRequest
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
    fun `verify phone number requires authentication`() {
        val request = VerifyOtpRequest(
            username = "+1234567890",
            otp = "123456",
            type = pitampoudel.komposeauth.user.domain.OtpType.PHONE
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
            otp = "000000", // Invalid OTP
            type = pitampoudel.komposeauth.user.domain.OtpType.PHONE,
            username = "phone-verify@example.com"
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
            username = "invalid-phone"
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

    @Test
    fun `send phone otp forbids sending to another user`() {
        val ownerEmail = "phone-owner@example.com"
        val otherPhone = "+9779812345678"

        TestAuthHelpers.createUser(mockMvc, json, ownerEmail)
        val ownerCookie = TestAuthHelpers.loginCookie(mockMvc, json, ownerEmail)

        // Create another user with the target phone number
        mockMvc.post("/${ApiEndpoints.USERS}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = json.encodeToString(
                CreateUserRequest.serializer(),
                CreateUserRequest(
                    firstName = "Other",
                    lastName = "User",
                    phoneNumber = otherPhone,
                    countryNameCode = "NP",
                    password = "Password1",
                    confirmPassword = "Password1"
                )
            )
        }.andExpect {
            status { isOk() }
        }

        val request = SendOtpRequest(
            username = otherPhone
        )

        mockMvc.post("/${ApiEndpoints.SEND_OTP}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            cookie(ownerCookie)
            content = json.encodeToString(SendOtpRequest.serializer(), request)
        }.andExpect {
            status { isForbidden() }
        }
    }
}
