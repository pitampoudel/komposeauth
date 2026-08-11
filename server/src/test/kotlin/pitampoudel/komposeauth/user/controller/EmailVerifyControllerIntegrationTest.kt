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
import pitampoudel.komposeauth.NoopEmailTestConfig
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.one_time_token.entity.OneTimeToken
import pitampoudel.komposeauth.one_time_token.service.OneTimeTokenService
import pitampoudel.komposeauth.otp.repository.OtpRepository
import pitampoudel.komposeauth.user.data.SendOtpRequest
import pitampoudel.komposeauth.user.data.VerifyOtpRequest
import pitampoudel.komposeauth.user.domain.OtpType
import pitampoudel.komposeauth.user.repository.UserRepository
import kotlin.time.Duration.Companion.minutes

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class, NoopEmailTestConfig::class)
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

        mockMvc.post("/${ApiEndpoints.SEND_OTP}") {
            cookie(cookie)
            contentType = MediaType.APPLICATION_JSON
            content = json.encodeToString(
                SendOtpRequest.serializer(),
                SendOtpRequest(email, type = OtpType.EMAIL)
            )
        }.andExpect {
            status { isOk() }
        }

        val otps = otpRepository.findByReceiverOrderByCreatedAtDesc(email)
        assert(otps.isNotEmpty())
    }

    @Test
    fun `sendEmailOtp forbids sending to another user`() {
        val ownerEmail = "owner@example.com"
        val otherEmail = "other@example.com"

        TestAuthHelpers.createUser(mockMvc, json, ownerEmail)
        TestAuthHelpers.createUser(mockMvc, json, otherEmail)
        val ownerCookie = TestAuthHelpers.loginCookie(mockMvc, json, ownerEmail)

        mockMvc.post("/${ApiEndpoints.SEND_OTP}") {
            cookie(ownerCookie)
            contentType = MediaType.APPLICATION_JSON
            content = json.encodeToString(
                SendOtpRequest.serializer(),
                SendOtpRequest(otherEmail, type = OtpType.EMAIL)
            )
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `verifyEmailOtp succeeds with valid otp`() {
        val email = "otp-verify@example.com"
        val userId = TestAuthHelpers.createUser(mockMvc, json, email)
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, email)

        mockMvc.post("/${ApiEndpoints.SEND_OTP}") {
            cookie(cookie)
            contentType = MediaType.APPLICATION_JSON
            content = json.encodeToString(
                SendOtpRequest.serializer(),
                SendOtpRequest(email, type = OtpType.EMAIL)
            )
        }.andExpect {
            status { isOk() }
        }

        val otp = otpRepository.findByReceiverOrderByCreatedAtDesc(email).first().otp

        mockMvc.post("/${ApiEndpoints.VERIFY_OTP}") {
            cookie(cookie)
            contentType = MediaType.APPLICATION_JSON
            content = json.encodeToString(
                VerifyOtpRequest.serializer(),
                VerifyOtpRequest(username = "otp-verify@example.com", otp = otp, type = OtpType.EMAIL)
            )
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `verifyEmailOtp fails with wrong otp`() {
        val email = "otp-verify-fail@example.com"
        val userId = TestAuthHelpers.createUser(mockMvc, json, email)
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, email)

        mockMvc.post("/${ApiEndpoints.SEND_OTP}") {
            cookie(cookie)
            contentType = MediaType.APPLICATION_JSON
            content = json.encodeToString(
                SendOtpRequest.serializer(),
                SendOtpRequest(email, type = OtpType.EMAIL)
            )
        }.andExpect {
            status { isOk() }
        }

        mockMvc.post("/${ApiEndpoints.VERIFY_OTP}") {
            cookie(cookie)
            contentType = MediaType.APPLICATION_JSON
            content = "{\"otp\":\"000000\"}"
        }.andExpect {
            status { isBadRequest() }
        }
    }

    /**
     * The endpoint that mails the verification link, at the path it is documented at.
     *
     * It used to answer `POST /` — a bare `@PostMapping` with no class-level `@RequestMapping` to
     * hang off — so there was no way to ask for a verification email at all, and `POST` to the root
     * of the auth server quietly sent one instead.
     */
    @Test
    fun `sendVerificationEmail is served at the verify-email path`() {
        val email = "verify-send@example.com"
        TestAuthHelpers.createUser(mockMvc, json, email)
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, email)

        mockMvc.post("/${ApiEndpoints.VERIFY_EMAIL}") {
            cookie(cookie)
        }.andExpect {
            status { isOk() }
        }
    }

    /**
     * The other half of the same fix. `/verify-email` is public so the emailed link can be followed
     * without credentials, and "public" here means the bearer resolver returns null and no
     * authentication is attempted — which for the POST would leave the handler with no idea who to
     * write to. It is public for GET only.
     */
    @Test
    fun `sendVerificationEmail refuses an anonymous caller`() {
        mockMvc.post("/${ApiEndpoints.VERIFY_EMAIL}").andExpect {
            status { isUnauthorized() }
        }
    }

    /** And nothing answers POST at the root any more. */
    @Test
    fun `posting to the root is not a way to send a verification email`() {
        val email = "verify-root@example.com"
        TestAuthHelpers.createUser(mockMvc, json, email)
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, email)

        mockMvc.post("/") {
            cookie(cookie)
        }.andExpect {
            status { isMethodNotAllowed() }
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
