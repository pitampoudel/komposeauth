package pitampoudel.komposeauth.user.controller

import kotlinx.serialization.json.Json
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import pitampoudel.komposeauth.TestAuthHelpers
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.one_time_token.entity.OneTimeToken
import pitampoudel.komposeauth.one_time_token.service.OneTimeTokenService
import pitampoudel.komposeauth.otp.repository.OtpRepository
import pitampoudel.komposeauth.user.repository.UserRepository
import kotlin.time.Duration.Companion.minutes

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
@AutoConfigureMockMvc
class EmailVerifyControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var json: Json

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var oneTimeTokenService: OneTimeTokenService

    @Autowired
    private lateinit var otpRepository: OtpRepository


    @Test
    fun `sendEmailOtp succeeds and stores otp`() {
        val email = "otp-send@example.com"
        TestAuthHelpers.createUser(mockMvc, json, email)
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, email)

        mockMvc.post("/${ApiEndpoints.SEND_EMAIL_OTP}") {
            cookie(cookie)
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
        }

        val otps = otpRepository.findByReceiverOrderByCreatedAtDesc(email)
        assert(otps.isNotEmpty())
    }

    @Test
    fun `verifyEmailOtp succeeds with valid otp`() {
        val email = "otp-verify@example.com"
        val userId = TestAuthHelpers.createUser(mockMvc, json, email)
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, email)

        mockMvc.post("/${ApiEndpoints.SEND_EMAIL_OTP}") {
            cookie(cookie)
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
        }

        val otp = otpRepository.findByReceiverOrderByCreatedAtDesc(email).first().otp

        mockMvc.post("/${ApiEndpoints.VERIFY_EMAIL_OTP}") {
            cookie(cookie)
            contentType = MediaType.APPLICATION_JSON
            content = "{\"otp\":\"$otp\"}"
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `verifyEmailOtp fails with wrong otp`() {
        val email = "otp-verify-fail@example.com"
        val userId = TestAuthHelpers.createUser(mockMvc, json, email)
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, email)

        mockMvc.post("/${ApiEndpoints.SEND_EMAIL_OTP}") {
            cookie(cookie)
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
        }

        mockMvc.post("/${ApiEndpoints.VERIFY_EMAIL_OTP}") {
            cookie(cookie)
            contentType = MediaType.APPLICATION_JSON
            content = "{\"otp\":\"000000\"}"
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `verifyEmail succeeds with valid token`() {
        val email = "verify-token@example.com"
        val userId = TestAuthHelpers.createUser(mockMvc, json, email)

        // Create a valid one-time token for email verification
        val token = oneTimeTokenService.createToken(
            userId = ObjectId(userId),
            purpose = OneTimeToken.Purpose.VERIFY_EMAIL,
            ttl = 1.minutes
        )

        mockMvc.get("/${ApiEndpoints.VERIFY_EMAIL}") {
            param("token", token)
        }.andExpect {
            status { is3xxRedirection() }
        }
    }

    @Test
    fun `verifyEmail fails with invalid token`() {
        mockMvc.get("/${ApiEndpoints.VERIFY_EMAIL}") {
            param("token", "invalid-token")
        }.andExpect {
            status { is4xxClientError() }
        }
    }
}
